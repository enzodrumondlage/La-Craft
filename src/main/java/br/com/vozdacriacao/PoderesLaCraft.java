package br.com.vozdacriacao;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.*;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import java.util.*;

public final class PoderesLaCraft {
    static final String VOO="lacraft_voo",GIGANTE="La Craft Gigante ";
    private static final Map<UUID,Integer> voando=new HashMap<>();
    private static final Set<String> PROIBIDOS=Set.of("command_block","chain_command_block","repeating_command_block","command_block_minecart","structure_block","structure_void","jigsaw","barrier","debug_stick","light","knowledge_book");
    static boolean especial(Pedido.Tipo t){return switch(t){case VOO,VELOCIDADE,SALTO,INVISIVEL,LASER,ESPADA,ITEM,MOB,ARTEFATO->true;default->false;};}
    static void registrar(){
        ServerPlayConnectionEvents.JOIN.register((h,s,server)->{if(h.player.getCommandTags().contains(VOO))revogar(h.player);});
        ServerPlayConnectionEvents.DISCONNECT.register((h,server)->revogar(h.player));
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(var p:server.getPlayerManager().getPlayerList()){
                Integer fim=voando.get(p.getUuid());
                if(fim!=null&&(!p.isAlive()||server.getTicks()>=fim))revogar(p);
            }
        });
        UseItemCallback.EVENT.register((player,world,hand)->{
            ItemStack item=player.getStackInHand(hand);
            if(!item.hasNbt()||!item.getNbt().getBoolean("lacraft_luz"))return TypedActionResult.pass(item);
            if(player.isSpectator())return TypedActionResult.pass(item);
            if(player.getItemCooldownManager().isCoolingDown(item.getItem()))return TypedActionResult.fail(item);
            if(!world.isClient&&player instanceof ServerPlayerEntity p){
                p.getItemCooldownManager().set(item.getItem(),18);
                ServerWorld w=p.getServerWorld();Vec3d a=p.getEyePos(),dir=p.getRotationVec(1);
                double dist=Math.min(24,a.distanceTo(p.raycast(24,1,false).getPos()));
                for(double k=0.5;k<=dist;k+=0.5){Vec3d v=a.add(dir.multiply(k));w.spawnParticles(ParticleTypes.END_ROD,v.x,v.y,v.z,1,0,0,0,0);}
                Vec3d end=a.add(dir.multiply(dist));w.spawnParticles(ParticleTypes.FLASH,end.x,end.y,end.z,1,0,0,0,0);
                VozDaCriacao.som(w,p.getBlockPos(),1);
            }
            return TypedActionResult.success(item,world.isClient);
        });
    }
    private static void revogar(ServerPlayerEntity p){
        voando.remove(p.getUuid());
        if(!p.getCommandTags().contains(VOO))return;
        p.removeScoreboardTag(VOO);
        if(!p.isCreative()&&!p.isSpectator()){
            p.getAbilities().allowFlying=false;p.getAbilities().flying=false;p.sendAbilitiesUpdate();
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING,200,0));
        }
    }
    private static void dar(ServerPlayerEntity p,ItemStack i){if(!p.getInventory().insertStack(i))p.dropItem(i,false);}
    static boolean executar(ServerPlayerEntity p,Pedido pedido){
        ServerWorld w=p.getServerWorld();
        switch(pedido.tipo()){
            case ARTEFATO -> {return ArtefatoLaCraft.entregar(p,pedido);}
            case VOO -> {
                if(!p.isCreative()&&!p.isSpectator()){
                    if(p.getAbilities().allowFlying&&!p.getCommandTags().contains(VOO))return true;
                    p.addCommandTag(VOO);voando.put(p.getUuid(),p.getServer().getTicks()+6000);
                    p.getAbilities().allowFlying=true;p.sendAbilitiesUpdate();
                }
                p.sendMessage(Text.literal("§bVoo liberado por 5 minutos. Aperte Espaço duas vezes."),false);return true;
            }
            case VELOCIDADE -> {p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,6000,3));return true;}
            case SALTO -> {p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST,6000,4));p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING,6000,0));return true;}
            case INVISIVEL -> {p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY,6000,0));return true;}
            case LASER -> {ItemStack s=new ItemStack(Items.AMETHYST_SHARD);s.setCustomName(Text.literal("§dCetro de luz"));s.getOrCreateNbt().putBoolean("lacraft_luz",true);dar(p,s);return true;}
            case ESPADA -> {ItemStack s=new ItemStack(Items.DIAMOND_SWORD);s.setCustomName(Text.literal("§bEspada da criação"));s.addEnchantment(Enchantments.SHARPNESS,5);s.addEnchantment(Enchantments.UNBREAKING,3);dar(p,s);return true;}
            case ITEM -> {
                Identifier id=Identifier.tryParse(pedido.alvo());if(id==null||!Registries.ITEM.containsId(id)||PROIBIDOS.contains(id.getPath()))return false;
                Item item=Registries.ITEM.get(id);if(item==Items.AIR)return false;
                int restante=pedido.quantidade();while(restante>0){int n=Math.min(restante,item.getMaxCount());dar(p,new ItemStack(item,n));restante-=n;}return true;
            }
            case MOB -> {
                Identifier id=Identifier.tryParse(pedido.alvo());if(id==null||!Registries.ENTITY_TYPE.containsId(id))return false;
                EntityType<?> tipo=Registries.ENTITY_TYPE.get(id);int feitos=0;
                BlockPos origem=p.getBlockPos().offset(p.getHorizontalFacing(),5);
                for(int k=0;k<pedido.quantidade();k++){
                    BlockPos pos=origem.add((k%3)*2-2,0,(k/3)*2);
                    if(!w.isChunkLoaded(pos.getX()>>4,pos.getZ()>>4)||!w.canPlayerModifyAt(p,pos)||!w.getBlockState(pos).isAir()||!w.getBlockState(pos.up()).isAir())continue;
                    Entity e=tipo.create(w);if(!(e instanceof MobEntity mob)){if(e!=null)e.discard();return false;}
                    mob.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
                    if(!w.isSpaceEmpty(mob,mob.getBoundingBox())){mob.discard();continue;}
                    if(mob instanceof PiglinEntity piglin)piglin.setImmuneToZombification(true);
                    if(pedido.escala()>1){mob.setCustomName(Text.literal(GIGANTE+pedido.nome()));mob.setCustomNameVisible(false);}
                    if(w.spawnEntity(mob)){feitos++;w.spawnParticles(ParticleTypes.END_ROD,mob.getX(),mob.getY()+1,mob.getZ(),16,.4,.6,.4,.02);}
                }return feitos>0;
            }
            default -> {return false;}
        }
    }
}
