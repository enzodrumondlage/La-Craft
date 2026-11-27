package br.com.vozdacriacao;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import java.util.*;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public final class VozDaCriacao implements ModInitializer {
    public static final Identifier SUGAR=id("sugar"),HUD=id("hud"),EFEITO=id("efeito"),PEDIR=id("pedir");
    private static final Map<UUID,Sessao> sessoes=new HashMap<>();
    private static final Set<Block> PERMITIDOS=Set.of(Blocks.DIRT,Blocks.GRASS_BLOCK,Blocks.COARSE_DIRT,Blocks.STONE,Blocks.COBBLESTONE,Blocks.SAND,Blocks.RED_SAND,Blocks.GRAVEL,Blocks.ANDESITE,Blocks.DIORITE,Blocks.GRANITE,Blocks.DEEPSLATE,Blocks.NETHERRACK,Blocks.END_STONE);
    private static final class Sessao {int ate,ultimo=-100,ultimoPedido=-100;boolean ativo,gerando;long geracao;Pedido pedido;MotorCriacao.Obra obra;}
    private static Identifier id(String n){return new Identifier("vozdacriacao",n);}
    private static Sessao sessao(ServerPlayerEntity p){return sessoes.computeIfAbsent(p.getUuid(),k->new Sessao());}
    @Override public void onInitialize(){
        ServerPlayNetworking.registerGlobalReceiver(SUGAR,(server,p,handler,buf,sender)->{
            if(buf.readableBytes()!=1)return;
            boolean ativo=buf.readBoolean();
            server.execute(()->{Sessao s=sessao(p);int t=server.getTicks();if(ativo&&t-s.ultimo<2)return;s.ultimo=t;s.ativo=ativo;s.ate=t+12;});
        });
        ServerPlayNetworking.registerGlobalReceiver(PEDIR,(server,p,handler,buf,sender)->{
            if(buf.readableBytes()>970)return;
            final String texto;
            try{texto=buf.readString(240);}catch(RuntimeException ex){return;}
            if(buf.isReadable())return;
            server.execute(()->{
                Sessao s=sessao(p);int t=server.getTicks();
                if(t-s.ultimoPedido<40)return;
                s.ultimoPedido=t;
                if(texto.codePoints().anyMatch(Character::isISOControl))return;
                pedir(server,p,texto);
            });
        });
        ServerPlayConnectionEvents.JOIN.register((h,s,server)->hud(server,h.player));
        ServerPlayConnectionEvents.DISCONNECT.register((h,server)->{
            Sessao s=sessoes.remove(h.player.getUuid());
            if(s!=null&&s.obra!=null)MateriaState.obter(server).alterar(h.player.getUuid(),s.obra.reembolso());
        });
        ServerTickEvents.END_SERVER_TICK.register(VozDaCriacao::tick);
        CommandRegistrationCallback.EVENT.register((d,a,e)->{
            d.register(literal("lacraft").then(argument("pedido",StringArgumentType.greedyString()).executes(c->pedir(c.getSource().getServer(),c.getSource().getPlayerOrThrow(),StringArgumentType.getString(c,"pedido")))));
            d.register(literal("vozteste").executes(c->pedir(c.getSource().getServer(),c.getSource().getPlayerOrThrow(),"fonte")));
            d.register(literal("lacancelar").executes(c->{var p=c.getSource().getPlayerOrThrow();Sessao s=sessao(p);s.pedido=null;s.gerando=false;s.geracao++;hud(c.getSource().getServer(),p);return 1;}));
        });
    }
    private static int pedir(MinecraftServer server,ServerPlayerEntity p,String texto){
        Sessao s=sessao(p);
        if(p.isSpectator()||!p.getAbilities().allowModifyWorld||s.obra!=null)return 0;
        if(texto.length()>240||texto.codePoints().anyMatch(Character::isISOControl))return 0;
        Pedido pedido=Pedido.interpretar(texto);
        boolean estrutura=pedido==null||switch(pedido.tipo()){case CASA,TORRE,FOGUETE,CASTELO,CAPIVARA,VOXEL->true;default->false;};
        if(estrutura&&IAConstrucoes.ativo()){
            if(s.gerando){p.sendMessage(Text.literal("§eJá existe uma criação sendo planejada."),true);return 0;}
            var futuro=IAConstrucoes.planejar(texto);
            if(futuro==null){p.sendMessage(Text.literal("§eIA ocupada. Tente novamente."),true);return 0;}
            s.pedido=null;s.gerando=true;long geracao=++s.geracao;hud(server,p);
            futuro.whenComplete((plano,erro)->server.execute(()->{
                if(sessoes.get(p.getUuid())!=s||s.geracao!=geracao)return;
                s.gerando=false;
                if(p.isAlive()&&!p.isSpectator()&&p.getAbilities().allowModifyWorld&&s.obra==null){
                    s.pedido=erro==null?plano:pedido;
                    if(s.pedido==null)p.sendMessage(Text.literal("§eA IA não retornou uma construção válida. Tente outro pedido."),false);
                    else if(erro!=null)p.sendMessage(Text.literal("§eIA indisponível; usando modelo procedural."),false);
                }
                hud(server,p);
            }));
            return 1;
        }
        s.geracao++;s.gerando=false;
        if(pedido==null){p.sendMessage(Text.literal("§ePedido desconhecido. Tente casa, castelo, torre, foguete, capivara, burros, arco ou armadura."),false);return 0;}
        s.pedido=pedido;hud(server,p);return 1;
    }
    private static void tick(MinecraftServer server){
        int t=server.getTicks();
        for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList()){
            Sessao s=sessao(p);
            if(s.obra!=null){
                if(!p.isAlive()||p.getServerWorld()!=s.obra.mundo||p.isSpectator()||!p.getAbilities().allowModifyWorld){
                    MateriaState.obter(server).alterar(p.getUuid(),s.obra.reembolso());s.obra=null;
                }else if(s.obra.tick(p)){MateriaState.obter(server).alterar(p.getUuid(),s.obra.reembolso());s.obra=null;p.sendMessage(Text.literal("§aConstrução concluída ou interrompida por obstáculos."),true);}
            }
            if(t%4!=0||!p.isAlive()||!ServerPlayNetworking.canSend(p,HUD))continue;
            if(s.ativo&&t<=s.ate&&!p.isSpectator()&&p.getAbilities().allowModifyWorld)sugar(server,p);
            if(s.ativo&&t>s.ate)s.ativo=false;
            var m=MateriaState.obter(server);
            if(s.pedido!=null&&s.obra==null&&!p.isSpectator()&&p.getAbilities().allowModifyWorld&&m.saldo(p.getUuid())>=s.pedido.custo()){
                var pedido=s.pedido;s.pedido=null;
                var resultado=MotorCriacao.iniciar(p,pedido);
                if(resultado.sucesso()){
                    m.alterar(p.getUuid(),-pedido.custo());s.obra=resultado.obra();
                    p.sendMessage(Text.literal("§aMaterializando: "+pedido.nome()),false);
                }else p.sendMessage(Text.literal("§eEspaço insuficiente. Nada foi cobrado; faça o pedido em área aberta."),false);
                hud(server,p);
            }
            if(t%20==0||s.ativo)hud(server,p);
        }
    }
    private static void sugar(MinecraftServer server,ServerPlayerEntity p){
        ServerWorld w=p.getServerWorld();MateriaState m=MateriaState.obter(server);
        HitResult hit=p.raycast(6,1,false);
        if(!(hit instanceof BlockHitResult alvo)||hit.getType()!=HitResult.Type.BLOCK)return;
        BlockPos centro=alvo.getBlockPos();List<BlockPos> ps=new ArrayList<>();
        for(BlockPos pos:BlockPos.iterate(centro.add(-2,-2,-2),centro.add(2,2,2)))if(pos.getSquaredDistance(centro)<=4)ps.add(pos.toImmutable());
        ps.sort(Comparator.comparingDouble(pos->pos.getSquaredDistance(centro)));
        int n=0;
        for(BlockPos pos:ps){
            if(n>=3||m.saldo(p.getUuid())>=MateriaState.LIMITE)break;
            if(w.isOutOfHeightLimit(pos)||!w.isChunkLoaded(pos.getX()>>4,pos.getZ()>>4)||!w.canPlayerModifyAt(p,pos)||p.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos))>64||p.getBoundingBox().intersects(new Box(pos)))continue;
            BlockState b=w.getBlockState(pos);
            if(!PERMITIDOS.contains(b.getBlock())||!b.getFluidState().isEmpty()||w.getBlockEntity(pos)!=null||b.getHardness(w,pos)<0)continue;
            if(!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(w,p,pos,b,null))continue;
            if(!w.setBlockState(pos,Blocks.AIR.getDefaultState(),Block.NOTIFY_ALL))continue;
            m.alterar(p.getUuid(),1);n++;
            PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(w,p,pos,b,null);
            for(ServerPlayerEntity q:w.getPlayers()){
                if(q.squaredDistanceTo(p)>1024||!ServerPlayNetworking.canSend(q,EFEITO))continue;
                PacketByteBuf buf=PacketByteBufs.create();buf.writeBlockPos(pos);buf.writeVarInt(Block.getRawIdFromState(b));buf.writeVarInt(p.getId());ServerPlayNetworking.send(q,EFEITO,buf);
            }
        }
    }
    private static void hud(MinecraftServer server,ServerPlayerEntity p){
        if(!ServerPlayNetworking.canSend(p,HUD))return;
        Sessao s=sessao(p);var b=PacketByteBufs.create();b.writeVarInt(MateriaState.obter(server).saldo(p.getUuid()));b.writeVarInt(s.pedido==null?0:s.pedido.custo());b.writeString(s.pedido==null?(s.gerando?"IA planejando...":(s.obra==null?"Nenhum":"Construindo...")):s.pedido.nome(),80);ServerPlayNetworking.send(p,HUD,b);
    }
}
