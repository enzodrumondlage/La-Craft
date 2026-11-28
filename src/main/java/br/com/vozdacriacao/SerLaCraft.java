package br.com.vozdacriacao;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.data.*;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
/** Entidade própria com visual persistente, caminhada e montaria opcional. */
public final class SerLaCraft extends PathAwareEntity {
    private static final TrackedData<String> VISUAL=DataTracker.registerData(SerLaCraft.class,TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Boolean> MONTAVEL=DataTracker.registerData(SerLaCraft.class,TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> ANDA=DataTracker.registerData(SerLaCraft.class,TrackedDataHandlerRegistry.BOOLEAN);
    static final EntityType<SerLaCraft> TIPO=EntityType.Builder.create(SerLaCraft::new,SpawnGroup.CREATURE).setDimensions(1f,1.5f).maxTrackingRange(8).build("vozdacriacao:ser");
    public SerLaCraft(EntityType<? extends PathAwareEntity> tipo,World w){super(tipo,w);}
    static void registrar(){
        Registry.register(Registries.ENTITY_TYPE,new Identifier("vozdacriacao","ser"),TIPO);
        FabricDefaultAttributeRegistry.register(TIPO,MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,24).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,.24));
    }
    @Override protected void initDataTracker(){super.initDataTracker();dataTracker.startTracking(VISUAL,"");dataTracker.startTracking(MONTAVEL,false);dataTracker.startTracking(ANDA,false);}
    @Override protected void initGoals(){
        goalSelector.add(0,new SwimGoal(this));
        goalSelector.add(3,new WanderAroundFarGoal(this,1.0){
            @Override public boolean canStart(){return dataTracker.get(ANDA)&&!hasPassengers()&&super.canStart();}
            @Override public boolean shouldContinue(){return dataTracker.get(ANDA)&&!hasPassengers()&&super.shouldContinue();}
        });
        goalSelector.add(5,new LookAroundGoal(this));
    }
    void aplicar(SerSpec s){dataTracker.set(VISUAL,s.visual());dataTracker.set(MONTAVEL,s.montavel());dataTracker.set(ANDA,s.anda());setPersistent();}
    String visual(){return dataTracker.get(VISUAL);}
    boolean montavel(){return dataTracker.get(MONTAVEL);}
    @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putString("lc_visual",visual());n.putBoolean("lc_montavel",montavel());n.putBoolean("lc_anda",dataTracker.get(ANDA));}
    @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);try{aplicar(new SerSpec(n.getBoolean("lc_montavel"),n.getBoolean("lc_anda"),n.getString("lc_visual")));}catch(RuntimeException ex){dataTracker.set(VISUAL,"");dataTracker.set(MONTAVEL,false);dataTracker.set(ANDA,false);}}
    @Override protected ActionResult interactMob(PlayerEntity p,Hand h){
        if(montavel()&&!p.isSneaking()&&!hasPassengers()&&!p.isSpectator()){
            if(!getWorld().isClient)p.startRiding(this);return ActionResult.success(getWorld().isClient);
        }
        return super.interactMob(p,h);
    }
    @Override public LivingEntity getControllingPassenger(){return montavel()&&getFirstPassenger() instanceof PlayerEntity p?p:null;}
    @Override public void travel(Vec3d movement){
        if(getControllingPassenger() instanceof PlayerEntity p){
            setYaw(p.getYaw());setPitch(p.getPitch()*.25f);bodyYaw=getYaw();headYaw=getYaw();
            if(isLogicalSideForUpdatingMovement()){
                setMovementSpeed(.25f);super.travel(new Vec3d(p.sidewaysSpeed*.5,0,p.forwardSpeed<0?p.forwardSpeed*.4:p.forwardSpeed));
            }else setVelocity(Vec3d.ZERO);
            return;
        }
        super.travel(movement);
    }
    static boolean criar(ServerPlayerEntity p,Pedido pedido){
        var w=p.getServerWorld();SerSpec spec=SerSpec.ler(pedido.alvo());String dono="lc_ser:"+p.getUuid();
        if(w.getEntitiesByType(TIPO,e->e.getCommandTags().contains(dono)).size()>=6||w.getEntitiesByType(TIPO,e->true).size()>=32)return false;
        BlockPos pos=p.getBlockPos().offset(p.getHorizontalFacing(),4);
        if(!w.isChunkLoaded(pos.getX()>>4,pos.getZ()>>4)||!w.canPlayerModifyAt(p,pos)||w.isOutOfHeightLimit(pos))return false;
        SerLaCraft e=TIPO.create(w);if(e==null)return false;
        e.aplicar(spec);e.setCustomName(Text.literal(pedido.nome()));e.addCommandTag(dono);e.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,p.getYaw(),0);
        if(!w.isSpaceEmpty(e,e.getBoundingBox())||!SpawnDesejo.invocar(w,e)){e.discard();return false;}return true;
    }
}
