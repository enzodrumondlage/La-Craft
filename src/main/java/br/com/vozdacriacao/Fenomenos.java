package br.com.vozdacriacao;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
/** Fenômenos do jogo com movimento real e partículas limitadas; não danificam construções. */
final class Fenomenos {
    static void tick(ServerWorld w,ServerPlayerEntity p,BlockPos o,String tipo,int idade,int dur){
        Vec3d centro=Vec3d.ofCenter(o);double fase=idade/20.0;
        if(tipo.equals("onda"))centro=centro.add(0,0,-7+14.0*idade/(dur*20.0));
        if(!w.isChunkLoaded(BlockPos.ofFloored(centro).getX()>>4,BlockPos.ofFloored(centro).getZ()>>4))return;
        int n=ConfigLaCraft.obter().perfilFPS==0?24:48;
        if(ConfigLaCraft.obter().particulas){
            for(int j=0;j<n;j++){
                double a=j*Math.PI*2/n+fase*4,h=(j%8)*.65,r=tipo.equals("gravidade")?5:1+h*.7;
                Vec3d v=switch(tipo){
                    case "onda"->centro.add((j%n-n/2.0)*.45,1+(j%5),Math.sin(j*.6+fase)*.5);
                    case "tremor"->centro.add(Math.cos(a)*r*2,.15,Math.sin(a)*r*2);
                    case "meteoros"->centro.add(Math.cos(a)*r*2,Math.max(0,12-(idade%40)*.3),Math.sin(a)*r*2);
                    default->centro.add(Math.cos(a)*r,h,Math.sin(a)*r);
                };
                w.spawnParticles(tipo.equals("onda")?ParticleTypes.SPLASH:tipo.equals("gravidade")?ParticleTypes.PORTAL:tipo.equals("meteoros")?ParticleTypes.FLAME:ParticleTypes.CLOUD,v.x,v.y,v.z,2,.12,.12,.12,.015);
            }
        }
        if(idade%20==0&&ConfigLaCraft.obter().sons)w.playSound(null,o,SoundEvents.BLOCK_BEACON_AMBIENT,SoundCategory.BLOCKS,.5f,.7f);
        int feitos=0;
        for(Entity e:w.getOtherEntities(null,new Box(centro.x-9,centro.y-5,centro.z-9,centro.x+9,centro.y+12,centro.z+9),x->x instanceof MobEntity||x instanceof ItemEntity||x==p)){
            if(feitos++>=32)break;
            if(e instanceof ServerPlayerEntity sp&&(sp.isCreative()||sp.isSpectator()))continue;
            Vec3d delta=centro.subtract(e.getPos());double dist=Math.sqrt(delta.x*delta.x+delta.z*delta.z);
            if(dist>9||dist<.1)continue;
            Vec3d horizontal=new Vec3d(delta.x,0,delta.z).normalize();
            Vec3d impulso=switch(tipo){
                case "gravidade"->horizontal.multiply(.28).add(0,.10,0);
                case "redemoinho"->new Vec3d(-horizontal.z,.0,horizontal.x).multiply(.32).add(horizontal.multiply(.11)).add(0,.23,0);
                case "onda"->new Vec3d(0,.16,.36);
                case "tremor"->horizontal.multiply(-.12).add(0,.12,0);
                default->horizontal.multiply(-.14).add(0,.08,0);
            };
            Vec3d vel=e.getVelocity().add(impulso);
            e.setVelocity(MathHelper.clamp(vel.x,-.8,.8),MathHelper.clamp(vel.y,-.5,.6),MathHelper.clamp(vel.z,-.8,.8));e.velocityModified=true;
            if(e instanceof net.minecraft.entity.LivingEntity living)living.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOW_FALLING,80,0,false,false));
        }
    }
}
