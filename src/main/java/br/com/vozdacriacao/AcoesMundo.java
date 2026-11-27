package br.com.vozdacriacao;
import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
final class AcoesMundo {
    private record Chuva(ServerWorld mundo,BlockPos centro,Pedido pedido,int inicio,int feitos){}
    private static final Map<UUID,Chuva> eventos=new HashMap<>();
    static void registrar(){
        ServerTickEvents.END_SERVER_TICK.register(AcoesMundo::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->eventos.remove(h.player.getUuid()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->eventos.clear());
    }
    static void cancelar(ServerPlayerEntity p){eventos.remove(p.getUuid());}
    static boolean especial(Pedido.Tipo t){return t==Pedido.Tipo.TEMPO||t==Pedido.Tipo.CLIMA||t==Pedido.Tipo.CHUVA_ITEM||t==Pedido.Tipo.CHUVA_MOB||t==Pedido.Tipo.FENOMENO;}
    private static boolean autorizado(ServerPlayerEntity p){return p.hasPermissionLevel(2)||p.getServer().isHost(p.getGameProfile());}
    static boolean executar(ServerPlayerEntity p,Pedido a){
        ServerWorld w=p.getServerWorld();
        if(a.tipo()==Pedido.Tipo.TEMPO||a.tipo()==Pedido.Tipo.CLIMA){
            if(!autorizado(p)){p.sendMessage(Text.literal("§eAlterar clima ou horário requer autorização do mundo."),false);return false;}
            if(a.tipo()==Pedido.Tipo.TEMPO){
                int h=Integer.parseInt(a.alvo());if(h<0||h>=24000)return false;
                w.setTimeOfDay(Math.floorDiv(w.getTimeOfDay(),24000L)*24000L+h);return true;
            }
            int dur=Math.max(1,Math.min(3600,a.escala()))*20;
            switch(a.alvo()){case "limpo"->w.setWeather(dur,0,false,false);case "chuva"->w.setWeather(0,dur,true,false);case "tempestade"->w.setWeather(0,dur,true,true);default->{return false;}}
            return true;
        }
        if(eventos.containsKey(p.getUuid())||eventos.size()>=16)return false;
        if(a.tipo()==Pedido.Tipo.FENOMENO){
            if(!Set.of("onda","redemoinho").contains(a.alvo())||a.escala()<1||a.escala()>30)return false;
            eventos.put(p.getUuid(),new Chuva(w,p.getBlockPos().offset(p.getHorizontalFacing(),10),a,p.getServer().getTicks(),0));return true;
        }
        Identifier id=Identifier.tryParse(a.alvo());if(id==null||a.quantidade()<1||a.quantidade()>32||a.escala()<1||a.escala()>30)return false;
        if(a.tipo()==Pedido.Tipo.CHUVA_ITEM){
            if(!Registries.ITEM.containsId(id)||!PoderesLaCraft.itemPermitido(id))return false;
        }else if(!Registries.ENTITY_TYPE.containsId(id))return false;
        eventos.put(p.getUuid(),new Chuva(w,p.getBlockPos().offset(p.getHorizontalFacing(),6),a,p.getServer().getTicks(),0));return true;
    }
    private static void tick(MinecraftServer s){
        if(s.getTicks()%5!=0)return;
        var it=eventos.entrySet().iterator();
        while(it.hasNext()){
            var e=it.next();var c=e.getValue();var p=s.getPlayerManager().getPlayer(e.getKey());var a=c.pedido();
            int idade=s.getTicks()-c.inicio();
            if(p==null||!p.isAlive()||p.isSpectator()||!p.getAbilities().allowModifyWorld||p.getServerWorld()!=c.mundo()||idade>=a.escala()*20||(a.tipo()!=Pedido.Tipo.FENOMENO&&c.feitos()>=a.quantidade())){
                if(p!=null)p.sendMessage(Text.literal(a.tipo()==Pedido.Tipo.FENOMENO?"§bFenômeno visual encerrado.":"§bEvento encerrado: "+c.feitos()+" de "+a.quantidade()+" aparições."),false);it.remove();continue;
            }
            if(a.tipo()==Pedido.Tipo.FENOMENO){
                var w=c.mundo();var o=c.centro();double fase=idade/20.0;
                if(w.isChunkLoaded(o.getX()>>4,o.getZ()>>4)){
                    if(a.alvo().equals("onda"))for(int x=-5;x<=5;x++)for(int y=0;y<6;y++){
                        double z=o.getZ()-5+(fase/a.escala())*10+Math.sin(x*.35+y*.2);
                        w.spawnParticles(ParticleTypes.SPLASH,o.getX()+x,o.getY()+y,z,2,.15,.15,.15,.02);
                    }else for(int j=0;j<40;j++){
                        double h=(j%10)*.65,ang=j*.8+fase*5,r=.8+h*.45;
                        w.spawnParticles(ParticleTypes.CLOUD,o.getX()+Math.cos(ang)*r,o.getY()+h,o.getZ()+Math.sin(ang)*r,1,.05,.05,.05,.01);
                    }
                }continue;
            }
            if(c.feitos()>=Math.max(1,(idade*a.quantidade())/(a.escala()*20)))continue;
            var w=c.mundo();var r=w.getRandom();BlockPos pos=c.centro().add(r.nextInt(9)-4,10,r.nextInt(9)-4);
            if(w.isOutOfHeightLimit(pos)||!w.isChunkLoaded(pos.getX()>>4,pos.getZ()>>4)||!w.canPlayerModifyAt(p,pos)||!w.getBlockState(pos).isAir())continue;
            Identifier id=new Identifier(a.alvo());Entity entidade;
            if(a.tipo()==Pedido.Tipo.CHUVA_ITEM)entidade=new ItemEntity(w,pos.getX()+.5,pos.getY(),pos.getZ()+.5,new ItemStack(Registries.ITEM.get(id)));
            else{
                entidade=Registries.ENTITY_TYPE.get(id).create(w);
                if(!(entidade instanceof MobEntity mob)){if(entidade!=null)entidade.discard();it.remove();continue;}
                mob.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
                mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOW_FALLING,240,0));
                if(!w.isSpaceEmpty(mob,mob.getBoundingBox())){mob.discard();continue;}
            }
            if(w.spawnEntity(entidade)){w.spawnParticles(ParticleTypes.CLOUD,pos.getX()+.5,pos.getY(),pos.getZ()+.5,5,.3,.3,.3,.02);e.setValue(new Chuva(w,c.centro(),a,c.inicio(),c.feitos()+1));}
        }
    }
}
