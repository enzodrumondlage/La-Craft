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
    public static final Identifier SUGAR=id("sugar"),HUD=id("hud"),EFEITO=id("efeito"),PEDIR=id("pedir"),SOM=id("som"),PREVIA=id("previa");
    private static final Map<UUID,Sessao> sessoes=new HashMap<>();
    private static final Set<Block> PERMITIDOS=Set.of(Blocks.DIRT,Blocks.GRASS_BLOCK,Blocks.COARSE_DIRT,Blocks.STONE,Blocks.COBBLESTONE,Blocks.SAND,Blocks.RED_SAND,Blocks.GRAVEL,Blocks.ANDESITE,Blocks.DIORITE,Blocks.GRANITE,Blocks.DEEPSLATE,Blocks.NETHERRACK,Blocks.END_STONE);
    private static final class Sessao {int ate,ultimo=-100,ultimoPedido=-100;boolean ativo,gerando;long geracao;Pedido pedido;final Deque<Pedido> fila=new ArrayDeque<>();MotorCriacao.Obra obra,ultimaObra;Pedido ultimaPrevia;int previaAte;}
    private static Identifier id(String n){return new Identifier("vozdacriacao",n);}
    private static Sessao sessao(ServerPlayerEntity p){return sessoes.computeIfAbsent(p.getUuid(),k->new Sessao());}
    @Override public void onInitialize(){
        ServerPlayNetworking.registerGlobalReceiver(SUGAR,(server,p,handler,buf,sender)->{
            if(buf.readableBytes()!=1)return;
            boolean ativo=buf.readBoolean();
            server.execute(()->{Sessao s=sessao(p);int t=server.getTicks();if(ativo&&t-s.ultimo<2)return;s.ultimo=t;s.ativo=ativo;s.ate=t+12;});
        });
        ServerPlayNetworking.registerGlobalReceiver(PEDIR,(server,p,handler,buf,sender)->{
            if(buf.readableBytes()>3080)return;
            final String texto;
            try{texto=buf.readString(768);}catch(RuntimeException ex){return;}
            if(buf.isReadable())return;
            server.execute(()->{
                Sessao s=sessao(p);int t=server.getTicks();
                if(t-s.ultimoPedido<40)return;
                s.ultimoPedido=t;
                if(texto.codePoints().anyMatch(Character::isISOControl))return;
                pedir(server,p,texto);
            });
        });
        net.fabricmc.fabric.api.message.v1.ServerMessageEvents.CHAT_MESSAGE.register((msg,p,params)->{
            if(!ConfigLaCraft.obter().chatDesejos||!ServerPlayNetworking.canSend(p,HUD))return;
            String texto=msg.getSignedContent();
            if(texto.length()>768||(!ConfigLaCraft.obter().chatTodos&&!Pedido.pedidoChat(texto)))return;
            var server=p.getServer();
            if(server==null)return;
            server.execute(()->{
                if(server.getPlayerManager().getPlayer(p.getUuid())!=p)return;
                Sessao s=sessao(p);int t=server.getTicks();
                if(t-s.ultimoPedido<40)return;
                s.ultimoPedido=t;pedir(server,p,texto);
            });
        });
        ArtefatoLaCraft.registrar();
        PoderesLaCraft.registrar();
        AcoesMundo.registrar();
        ModelosVivos.registrar();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->IndiceRecursos.limpar());
        ServerPlayConnectionEvents.JOIN.register((h,s,server)->hud(server,h.player));
        ServerPlayConnectionEvents.DISCONNECT.register((h,server)->{
            Sessao s=sessoes.remove(h.player.getUuid());
            if(s!=null&&s.obra!=null)MateriaState.obter(server).alterar(h.player.getUuid(),s.obra.reembolso());
        });
        ServerTickEvents.END_SERVER_TICK.register(VozDaCriacao::tick);
        CommandRegistrationCallback.EVENT.register((d,a,e)->{
            d.register(literal("lacraft").then(argument("pedido",StringArgumentType.greedyString()).executes(c->pedir(c.getSource().getServer(),c.getSource().getPlayerOrThrow(),StringArgumentType.getString(c,"pedido")))));
            d.register(literal("lacraftbuscar").then(argument("termo",StringArgumentType.greedyString()).executes(c->{
                String termo=StringArgumentType.getString(c,"termo");if(termo.length()>120)return 0;
                var p=c.getSource().getPlayerOrThrow();String resultado=CatalogoMods.contexto(termo);
                for(String linha:resultado.split("\\n"))p.sendMessage(Text.literal(linha.length()>1800?linha.substring(0,1800)+"...":linha),false);return 1;
            })));
            d.register(literal("lalimparmodelos").executes(c->{var p=c.getSource().getPlayerOrThrow();int n=ModelosVivos.limpar(p);p.sendMessage(Text.literal("Peças dos seus modelos próximas removidas: "+n),false);return n;}));
            d.register(literal("vozteste").executes(c->pedir(c.getSource().getServer(),c.getSource().getPlayerOrThrow(),"fonte")));
            d.register(literal("lacancelar").executes(c->{var p=c.getSource().getPlayerOrThrow();Sessao s=sessao(p);s.pedido=null;s.fila.clear();if(s.obra!=null){MateriaState.obter(c.getSource().getServer()).alterar(p.getUuid(),s.obra.reembolso());s.obra=null;}AcoesMundo.cancelar(p);s.gerando=false;s.geracao++;hud(c.getSource().getServer(),p);return 1;}));
        });
    }
    private static int pedir(MinecraftServer server,ServerPlayerEntity p,String texto){
        Sessao s=sessao(p);
        if(p.isSpectator()||!p.getAbilities().allowModifyWorld||s.obra!=null){p.sendMessage(Text.literal("§eNão é possível criar agora: construção em andamento ou sem permissão."),true);return 0;}
        if(texto.length()>768||texto.codePoints().anyMatch(Character::isISOControl))return 0;
        String corrigido=AcoesTexto.corrigir(texto);
        if(!corrigido.equals(Pedido.normal(texto))){p.sendMessage(Text.literal("§bInterpretei: "+corrigido),false);}
        if(Set.of("desfaca","desfazer","desfaca a ultima criacao","desfaca meu ultimo desejo","desfaca a ultima construcao").contains(Pedido.normal(texto))){
            if(s.ultimaObra==null||!s.ultimaObra.desfazer(p)){p.sendMessage(Text.literal("§eNão foi possível desfazer: construção alterada, inventário ocupado ou mundo diferente."),false);return 0;}
            s.ultimaObra=null;p.sendMessage(Text.literal("§aÚltima construção removida."),false);return 1;
        }
        if(s.gerando||s.pedido!=null||!s.fila.isEmpty()){p.sendMessage(Text.literal("§eJá existe um pedido ativo; use /lacancelar."),false);return 0;}
        if(Pedido.palavra(corrigido,"nao quero|nao crie|nao faca|nao invoque")&&corrigido.matches("^(?:lacraft: )?nao (?:quero|crie|faca|invoque).*")){p.sendMessage(Text.literal("§ePedido negado; nada criado."),false);return 0;}
        Pedido existente=IndiceRecursos.exato(texto);
        if(existente!=null){s.pedido=existente;p.sendMessage(Text.literal("§bRecurso encontrado: "+existente.alvo()),false);hud(server,p);return 1;}
        Pedido equipamento=CatalogoMods.equipamentoSimples(texto);
        if(equipamento!=null){s.pedido=equipamento;p.sendMessage(Text.literal("§bEquipamento encontrado em mod: "+equipamento.alvo()),false);hud(server,p);return 1;}
        if(!IAConstrucoes.ativo()&&CatalogoMods.pedeCajadoSimples(texto)){p.sendMessage(Text.literal("§eNenhum cajado ou varinha correspondente encontrado nos mods instalados. Use /lacraftbuscar cajado."),false);return 0;}
        Pedido pedido;
        try{pedido=AcoesTexto.interpretar(texto);if(pedido!=null){s.pedido=pedido;hud(server,p);return 1;}pedido=Residencia.interpretar(texto,ConfigLaCraft.obter().qualidadeCasa);if(pedido==null)pedido=ArtefatoSpec.offline(texto);if(pedido==null)pedido=Pedido.interpretar(texto);}catch(IllegalArgumentException ex){if(IAConstrucoes.ativo())pedido=null;else{p.sendMessage(Text.literal("§ePedido não suportado pelo modo offline: "+ex.getMessage()),true);return 0;}}
        if(pedido!=null&&pedido.tipo()==Pedido.Tipo.CASA)pedido=null;
        Pedido recurso=CatalogoMods.alternativa(texto);if(recurso!=null)pedido=recurso;
        if(recurso!=null){s.pedido=recurso;hud(server,p);return 1;}
        if(pedido!=null&&pedido.tipo()==Pedido.Tipo.RESIDENCIA&&!IAConstrucoes.ativo()){s.pedido=pedido;hud(server,p);return 1;}
        final Pedido alternativa=pedido;
        boolean usarIA=true;
        if(usarIA&&IAConstrucoes.ativo()){
            if(s.gerando){p.sendMessage(Text.literal("§eJá existe uma criação sendo planejada."),true);return 0;}
            var futuro=IAConstrucoes.planejar(corrigido,CatalogoMods.contexto(corrigido),IndiceRecursos.obter());
            if(futuro==null){p.sendMessage(Text.literal("§eIA ocupada. Tente novamente."),true);return 0;}
            s.pedido=null;s.gerando=true;long geracao=++s.geracao;hud(server,p);
            futuro.whenComplete((plano,erro)->server.execute(()->{
                if(sessoes.get(p.getUuid())!=s||s.geracao!=geracao)return;
                s.gerando=false;
                if(p.isAlive()&&!p.isSpectator()&&p.getAbilities().allowModifyWorld&&s.obra==null){
                    s.pedido=erro==null?plano:null;
                    if(erro!=null)p.sendMessage(Text.literal("§e"+IAConstrucoes.diagnostico(erro)+" Nenhuma criação substituta foi executada. Para IA gratuita: Mods > La Craft > IA > OLLAMA."),false);
                    else if(s.pedido!=null)p.sendMessage(Text.literal("§bPlano validado da IA: "+s.pedido.nome()),false);
                    else if(s.pedido==null)p.sendMessage(Text.literal("§ePlano indisponível."),false);

                }
                hud(server,p);
            }));
            return 1;
        }
        s.geracao++;s.gerando=false;
        if(pedido==null){p.sendMessage(Text.literal("§ePedido não suportado no modo local. Ative uma IA generativa em Mods > La Craft > IA."),false);return 0;}
        s.pedido=pedido;hud(server,p);return 1;
    }
    private static void tick(MinecraftServer server){
        int t=server.getTicks();
        for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList()){
            Sessao s=sessao(p);
            if(s.obra!=null){
                if(!p.isAlive()||p.getServerWorld()!=s.obra.mundo||p.isSpectator()||!p.getAbilities().allowModifyWorld){
                    MateriaState.obter(server).alterar(p.getUuid(),s.obra.reembolso());s.obra=null;s.fila.clear();
                }else if(s.obra.tick(p)){
                    boolean completa=s.obra.completa();if(completa)s.ultimaObra=s.obra;else s.fila.clear();
                    hud(server,p);
                    MateriaState.obter(server).alterar(p.getUuid(),s.obra.reembolso());s.obra=null;
                    som(p.getServerWorld(),p.getBlockPos(),completa?3:4);
                    p.sendMessage(Text.literal(completa?"§aDesejo realizado!":"§eConstrução interrompida por obstáculos."),true);
                }
            }
            if(t%4!=0||!p.isAlive()||!ServerPlayNetworking.canSend(p,HUD))continue;
            if(ConfigLaCraft.obter().sugar&&s.ativo&&t<=s.ate&&!p.isSpectator()&&p.getAbilities().allowModifyWorld)sugar(server,p);
            if(s.ativo&&t>s.ate)s.ativo=false;
            var m=MateriaState.obter(server);
            if(s.pedido==null&&s.obra==null&&!s.fila.isEmpty()&&!AcoesMundo.ocupado(p))s.pedido=s.fila.removeFirst();
            if(s.pedido!=null&&s.pedido.tipo()==Pedido.Tipo.SEQUENCIA){s.fila.addAll(s.pedido.acoes());s.pedido=s.fila.removeFirst();}
            if(s.pedido!=null&&s.obra==null&&!AcoesMundo.ocupado(p)&&!p.isSpectator()&&p.getAbilities().allowModifyWorld&&(!ConfigLaCraft.obter().coletarMateria||m.saldo(p.getUuid())>=s.pedido.custo())){
                var pedido=s.pedido;s.pedido=null;
                if(!ConfigLaCraft.obter().coletarMateria)pedido=pedido.gratuito();
                var resultado=MotorCriacao.iniciar(p,pedido);
                if(resultado.sucesso()){
                    m.alterar(p.getUuid(),-pedido.custo());s.obra=resultado.obra();s.ultimaPrevia=pedido;s.previaAte=t+100;enviarPrevia(p,pedido);
                    som(p.getServerWorld(),p.getBlockPos(),s.obra==null?3:1);
                    p.sendMessage(Text.literal("§aMaterializando: "+pedido.nome()),false);
                }else {s.fila.clear();p.sendMessage(Text.literal("§eAção indisponível ou espaço insuficiente. Etapas restantes canceladas; esta ação não foi cobrada."),false);}
                hud(server,p);
            }
            if(s.pedido!=null&&s.obra==null&&ConfigLaCraft.obter().coletarMateria&&m.saldo(p.getUuid())<s.pedido.custo()&&t%100==0)p.sendMessage(Text.literal("§eFaltam "+(s.pedido.custo()-m.saldo(p.getUuid()))+" de matéria. G para coletar ou /lacancelar."),true);
            if(t%20==0||s.ativo||s.obra!=null)hud(server,p);
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
            if(n>=ConfigLaCraft.obter().velocidadeSugador||m.saldo(p.getUuid())>=MateriaState.LIMITE)break;
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
        if(n>0)som(w,p.getBlockPos(),0);
    }
    /** Um evento sonoro por lote, não por bloco; preferência individual no cliente. */
    static void som(ServerWorld w,BlockPos pos,int tipo){
        for(ServerPlayerEntity q:w.getPlayers()){
            if(q.getPos().squaredDistanceTo(Vec3d.ofCenter(pos))>1024||!ServerPlayNetworking.canSend(q,SOM))continue;
            var b=PacketByteBufs.create();b.writeBlockPos(pos);b.writeByte(tipo);ServerPlayNetworking.send(q,SOM,b);
        }
    }
    private static void enviarPrevia(ServerPlayerEntity p,Pedido pedido){
        if(!ServerPlayNetworking.canSend(p,PREVIA))return;
        var b=PacketByteBufs.create();b.writeString(pedido.nome(),80);
        var celulas=pedido.celulas();int max=ConfigLaCraft.obter().perfilFPS==0?200:ConfigLaCraft.obter().perfilFPS==1?400:800;int passo=Math.max(1,(celulas.size()+max-1)/max);
        b.writeVarInt((celulas.size()+passo-1)/passo);
        for(int i=0;i<celulas.size();i+=passo){var c=celulas.get(i);b.writeInt(c.x());b.writeInt(c.y());b.writeInt(c.z());b.writeString(c.bloco(),120);}
        ServerPlayNetworking.send(p,PREVIA,b);
    }
    private static void hud(MinecraftServer server,ServerPlayerEntity p){
        if(!ServerPlayNetworking.canSend(p,HUD))return;
        Sessao s=sessao(p);var b=PacketByteBufs.create();b.writeVarInt(MateriaState.obter(server).saldo(p.getUuid()));b.writeVarInt(s.pedido==null||!ConfigLaCraft.obter().coletarMateria?0:s.pedido.custo());b.writeString(s.pedido==null?(s.gerando?"IA planejando...":(s.obra==null?"Nenhum":"Construindo...")):s.pedido.nome(),80);b.writeBoolean(!ConfigLaCraft.obter().coletarMateria);b.writeVarInt(s.obra==null?0:s.obra.progresso());b.writeVarInt(s.obra==null?0:s.obra.total());ServerPlayNetworking.send(p,HUD,b);
    }
}
