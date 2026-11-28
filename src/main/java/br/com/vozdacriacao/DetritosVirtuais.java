package br.com.vozdacriacao;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import br.com.vozdacriacao.mixin.*;
import java.util.*;
import org.joml.Vector3f;
import org.joml.Quaternionf;
/** Detritos são cópias visuais temporárias, nunca quebram ou duplicam blocos. */
final class DetritosVirtuais {
    private record Peca(DisplayEntity.BlockDisplayEntity e,int i){}
    private static final Map<UUID,List<Peca>> ativos=new HashMap<>();
    static void limpar(UUID id){var lista=ativos.remove(id);if(lista!=null)lista.forEach(p->p.e().discard());}
    static void limparTudo(){for(UUID id:List.copyOf(ativos.keySet()))limpar(id);}
    static Vec3d posicao(Vec3d centro,String tipo,int i,int n,int idade){
        double fase=idade*.1, ang=i*2*Math.PI/n+fase;
        if(tipo.equals("onda"))return centro.add((i%8-3.5)*1.1,1+(i/8)*1.2,Math.sin(i*.6+fase)*.9);
        double h=.8+(i%8)*.65,raio=1.5+h*.65;
        return centro.add(Math.cos(ang)*raio,h+Math.sin(fase+i)*.3,Math.sin(ang)*raio);
    }
    static void tick(ServerWorld w,ServerPlayerEntity p,Vec3d centro,String tipo,int idade,int dur){
        if(!tipo.equals("onda")&&!tipo.equals("redemoinho"))return;
        UUID id=p.getUuid();int n=ConfigLaCraft.obter().perfilFPS==0?12:24;
        if(!ativos.containsKey(id)){
            if(ativos.size()>=4)return;
            List<Peca> lista=new ArrayList<>();ativos.put(id,lista);
            for(int i=0;i<n;i++){
                var e=EntityType.BLOCK_DISPLAY.create(w);if(e==null)continue;
                Vec3d v=posicao(centro,tipo,i,n,idade);BlockPos pos=BlockPos.ofFloored(v);
                if(!w.isChunkLoaded(pos.getX()>>4,pos.getZ()>>4)||!w.canPlayerModifyAt(p,pos)){e.discard();continue;}
                BlockState b=tipo.equals("onda")?(i%3==0?Blocks.LIGHT_BLUE_STAINED_GLASS:Blocks.BLUE_STAINED_GLASS).getDefaultState():w.getBlockState(pos.down(2));
                if(b.isAir()||b.hasBlockEntity()||!b.getFluidState().isEmpty())b=Blocks.STONE.getDefaultState();
                ((BlocoDisplayAcesso)e).lacraft$bloco(b);
                e.addCommandTag("lc_detrito_temporario");e.setNoGravity(true);e.refreshPositionAndAngles(v.x,v.y,v.z,0,0);
                if(w.spawnEntity(e))lista.add(new Peca(e,i));else e.discard();
            }
        }
        for(var a:ativos.getOrDefault(id,List.of())){
            var e=a.e();if(e.isRemoved())continue;
            Vec3d v=posicao(centro,tipo,a.i(),n,idade);BlockPos pos=BlockPos.ofFloored(v);
            if(!w.isChunkLoaded(pos.getX()>>4,pos.getZ()>>4)){e.discard();continue;}
            e.setPosition(v.x,v.y,v.z);
            float s=tipo.equals("onda")?.8f:.55f;
            ((DisplayAcesso)e).lacraft$duracao(5);((DisplayAcesso)e).lacraft$inicio(0);
            ((DisplayAcesso)e).lacraft$transformar(new AffineTransformation(new Vector3f(),new Quaternionf().rotateY(idade*.05f+a.i()),new Vector3f(s,s,s),new Quaternionf()));
        }
    }
    static void registrar(){
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((e,w)->{
            if(e.getCommandTags().contains("lc_detrito_temporario")&&ativos.values().stream().flatMap(List::stream).noneMatch(p->p.e()==e))
                w.getServer().execute(()->{if(ativos.values().stream().flatMap(List::stream).noneMatch(p->p.e()==e))e.discard();});
        });
    }
}
