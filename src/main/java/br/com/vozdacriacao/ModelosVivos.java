package br.com.vozdacriacao;
import br.com.vozdacriacao.mixin.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;
/** Modelos decorativos de BlockDisplay. Sem colisão, IA de mob ou condução. */
final class ModelosVivos {
    private static final String PREFIX="lc_peca:";
    private record Anim(float sx,float sy,float sz,String tipo){}
    private static final Map<DisplayEntity.BlockDisplayEntity,Anim> ativos=new HashMap<>();
    static void registrar(){
        ServerEntityEvents.ENTITY_LOAD.register((e,w)->{
            if(e instanceof DisplayEntity.BlockDisplayEntity d)
                for(String t:e.getCommandTags())if(t.startsWith(PREFIX)){
                    try{String[] a=t.substring(PREFIX.length()).split(":");float sx=Float.parseFloat(a[0]),sy=Float.parseFloat(a[1]),sz=Float.parseFloat(a[2]);if(sx>=.2f&&sx<=8&&sy>=.2f&&sy<=8&&sz>=.2f&&sz<=8&&Set.of("fixo","balanco","giro","flutuar").contains(a[3]))ativos.put(d,new Anim(sx,sy,sz,a[3]));}catch(RuntimeException ignored){}break;
                }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((e,w)->ativos.remove(e));
        ServerLifecycleEvents.SERVER_STOPPED.register(s->ativos.clear());
        ServerTickEvents.END_SERVER_TICK.register(s->{
            if(s.getTicks()%5!=0)return;
            double fase=s.getTicks()*.075;int n=0;
            for(var entry:ativos.entrySet()){
                var d=entry.getKey();var a=entry.getValue();
                if(d.isRemoved()||a.tipo().equals("fixo")||n>=96)continue;
                if(d.getWorld().getClosestPlayer(d,48)==null)continue;
                animar(d,a,fase);n++;
            }
        });
    }
    private static void animar(DisplayEntity.BlockDisplayEntity e,Anim a,double fase){
        Quaternionf q=new Quaternionf();float y=0;
        switch(a.tipo()){case "balanco"->q.rotateZ((float)Math.sin(fase)*.35f);case "giro"->q.rotateY((float)fase);case "flutuar"->y=(float)Math.sin(fase)*.3f;default->{}}
        DisplayAcesso ac=(DisplayAcesso)e;ac.lacraft$duracao(5);ac.lacraft$inicio(0);
        ac.lacraft$transformar(new AffineTransformation(new Vector3f(0,y,0),q,new Vector3f(a.sx(),a.sy(),a.sz()),new Quaternionf()));
    }
    static boolean criar(ServerPlayerEntity p,Pedido pedido){
        ServerWorld w=p.getServerWorld();var pecas=ModeloSpec.ler(pedido.alvo());
        String dono="lc_dono:"+p.getUuid();
        long quantidade=ativos.keySet().stream().filter(e->e.getCommandTags().contains(dono)).count();
        if(quantidade+pecas.size()>72||ativos.size()+pecas.size()>256)return false;
        BlockPos origem=p.getBlockPos().offset(p.getHorizontalFacing(),13);
        List<DisplayEntity.BlockDisplayEntity> criados=new ArrayList<>();
        for(var v:pecas){
            BlockPos pos=origem.add((int)Math.floor(v.x()),(int)Math.floor(v.y()),(int)Math.floor(v.z()));
            if(w.isOutOfHeightLimit(pos)||!w.isChunkLoaded(pos.getX()>>4,pos.getZ()>>4)||!w.canPlayerModifyAt(p,pos))return false;
        }
        for(var v:pecas){
            var e=EntityType.BLOCK_DISPLAY.create(w);if(e==null){criados.forEach(x->x.discard());return false;}
            e.refreshPositionAndAngles(origem.getX()+v.x(),origem.getY()+v.y(),origem.getZ()+v.z(),0,0);
            e.addCommandTag(dono);e.addCommandTag(PREFIX+v.sx()+":"+v.sy()+":"+v.sz()+":"+v.movimento());e.setNoGravity(true);
            ((BlocoDisplayAcesso)e).lacraft$bloco(Registries.BLOCK.get(new Identifier("minecraft",v.bloco())).getDefaultState());
            animar(e,new Anim(v.sx(),v.sy(),v.sz(),v.movimento()),0);
            if(!w.spawnEntity(e)){e.discard();criados.forEach(x->x.discard());return false;}criados.add(e);
        }
        return true;
    }
    static int limpar(ServerPlayerEntity p){
        String dono="lc_dono:"+p.getUuid();
        var lista=ativos.keySet().stream().filter(e->e.getWorld()==p.getServerWorld()&&e.getCommandTags().contains(dono)&&e.squaredDistanceTo(p)<48*48).toList();
        lista.forEach(e->e.discard());return lista.size();
    }
}
