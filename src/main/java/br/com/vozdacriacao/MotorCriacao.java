package br.com.vozdacriacao;
import net.minecraft.block.*;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import java.util.*;
/** Motor procedural limitado: não executa código ou comandos de terceiros. */
public final class MotorCriacao {
    public record Resultado(boolean sucesso,Obra obra){}
    private record Voxel(BlockPos pos,BlockState estado){}
    private static boolean livre(ServerWorld w,ServerPlayerEntity p,BlockPos pos){
        return !w.isOutOfHeightLimit(pos)&&w.isChunkLoaded(pos.getX()>>4,pos.getZ()>>4)&&w.canPlayerModifyAt(p,pos)&&w.getBlockState(pos).isAir()&&!p.getBoundingBox().intersects(new Box(pos))&&w.getOtherEntities(null,new Box(pos)).isEmpty();
    }
    public static final class Obra {
        final ServerWorld mundo;private final List<Voxel> voxels;private final int custo;private int i,colocados;
        Obra(ServerWorld w,List<Voxel> v,int c){mundo=w;voxels=v;custo=c;}
        boolean tick(ServerPlayerEntity p){
            for(int n=0;n<ConfigLaCraft.obter().velocidadeConstrucao&&i<voxels.size();n++){
                Voxel v=voxels.get(i++);
                if(!livre(mundo,p,v.pos()))return true;
                if(mundo.setBlockState(v.pos(),v.estado(),Block.NOTIFY_ALL)){
                    colocados++;mundo.spawnParticles(ParticleTypes.END_ROD,v.pos().getX()+.5,v.pos().getY()+.5,v.pos().getZ()+.5,2,.3,.3,.3,.02);
                }else return true;
            }
            return i>=voxels.size();
        }
        int reembolso(){return custo-(int)Math.ceil(custo*(colocados/(double)voxels.size()));}
    }
    public static Resultado iniciar(ServerPlayerEntity p,Pedido pedido){
        ServerWorld w=p.getServerWorld();
        if(pedido.tipo()==Pedido.Tipo.FONTE){w.spawnParticles(ParticleTypes.END_ROD,p.getX(),p.getY()+1,p.getZ(),70,1,1,1,.12);return new Resultado(true,null);}
        if(pedido.tipo()==Pedido.Tipo.ARCO){
            ItemStack item=new ItemStack(Items.BOW);item.setCustomName(Text.literal("Arco mágico"));item.addEnchantment(Enchantments.INFINITY,1);item.addEnchantment(Enchantments.UNBREAKING,3);
            if(!p.getInventory().insertStack(item))p.dropItem(item,false);return new Resultado(true,null);
        }
        if(pedido.tipo()==Pedido.Tipo.ARMADURA){
            for(Item it:List.of(Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS)){
                ItemStack item=new ItemStack(it);item.setCustomName(Text.literal("Armadura esmeralda"));item.addEnchantment(Enchantments.PROTECTION,4);if(!p.getInventory().insertStack(item))p.dropItem(item,false);
            }return new Resultado(true,null);
        }
        BlockPos origem=p.getBlockPos().offset(p.getHorizontalFacing(),9);
        if(pedido.tipo()==Pedido.Tipo.BURROS){
            int n=0;
            for(int x=-2;x<=2;x++)for(int z=-1;z<=1;z++){
                BlockPos pos=origem.add(x,0,z);
                if(n>=10||!livre(w,p,pos)||!livre(w,p,pos.up()))continue;
                var e=EntityType.DONKEY.create(w);if(e!=null){e.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);if(w.spawnEntity(e)){n++;w.spawnParticles(ParticleTypes.CLOUD,e.getX(),e.getY()+1,e.getZ(),12,.4,.4,.4,.05);}}
            }return new Resultado(n>0,null);
        }
        Map<BlockPos,BlockState> mapa=new LinkedHashMap<>();
        switch(pedido.tipo()){
            case VOXEL -> {
                for(Pedido.Celula c:pedido.celulas()){
                    Block b=net.minecraft.registry.Registries.BLOCK.get(new net.minecraft.util.Identifier("minecraft",c.bloco()));
                    put(mapa,origem,c.x(),c.y(),c.z(),b);
                }
            }
            case CASA -> {
                for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=0;y<=5;y++){
                    boolean borda=Math.abs(x)==3||Math.abs(z)==3;
                    if(y==5||(borda&&y<5&&!(z==-3&&x==0&&y<=1)))put(mapa,origem,x,y,z,y==5?Blocks.SPRUCE_PLANKS:((y==2&&Math.abs(x)==3&&z==0)?Blocks.GLASS:Blocks.OAK_PLANKS));
                }
            }
            case CASTELO,TORRE -> {
                int r=pedido.tipo()==Pedido.Tipo.CASTELO?5:3,h=pedido.tipo()==Pedido.Tipo.CASTELO?6:9;
                for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)for(int y=0;y<=h;y++){
                    boolean borda=Math.abs(x)==r||Math.abs(z)==r;
                    if(borda&&!(z==-r&&x==0&&y<3)&&(y<h||(x+z)%2==0))put(mapa,origem,x,y,z,Blocks.STONE_BRICKS);
                }
            }
            case FOGUETE -> {
                for(int y=0;y<=11;y++){int r=y>8?1:2;
                    for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)if(x*x+z*z<=r*r+1&&(Math.abs(x)==r||Math.abs(z)==r||y==0||y==11))put(mapa,origem,x,y,z,y>8?Blocks.RED_CONCRETE:(y==6?Blocks.LIGHT_BLUE_CONCRETE:Blocks.WHITE_CONCRETE));}
                put(mapa,origem,0,12,0,Blocks.RED_CONCRETE);
            }
            case CAPIVARA -> {
                cubo(mapa,origem,-2,1,-2,2,3,1,Blocks.BROWN_TERRACOTTA);
                cubo(mapa,origem,-1,2,2,1,4,3,Blocks.BROWN_TERRACOTTA);
                cubo(mapa,origem,-1,2,4,1,3,4,Blocks.TERRACOTTA);
                for(int x:new int[]{-2,2})for(int z:new int[]{-2,1})put(mapa,origem,x,0,z,Blocks.BROWN_TERRACOTTA);
                for(int x:new int[]{-1,1}){put(mapa,origem,x,4,2,Blocks.BROWN_WOOL);put(mapa,origem,x,3,3,Blocks.BLACK_CONCRETE);}
            }
            default -> {return new Resultado(false,null);}
        }
        var v=mapa.entrySet().stream().map(e->new Voxel(e.getKey(),e.getValue())).sorted(Comparator.comparingInt(a->a.pos().getY())).toList();
        if(v.isEmpty()||v.size()>1500||v.stream().anyMatch(a->!livre(w,p,a.pos())))return new Resultado(false,null);
        return new Resultado(true,new Obra(w,v,pedido.custo()));
    }
    private static void put(Map<BlockPos,BlockState> m,BlockPos o,int x,int y,int z,Block b){m.put(o.add(x,y,z),b.getDefaultState());}
    private static void cubo(Map<BlockPos,BlockState> m,BlockPos o,int x1,int y1,int z1,int x2,int y2,int z2,Block b){for(int x=x1;x<=x2;x++)for(int y=y1;y<=y2;y++)for(int z=z1;z<=z2;z++)put(m,o,x,y,z,b);}
}
