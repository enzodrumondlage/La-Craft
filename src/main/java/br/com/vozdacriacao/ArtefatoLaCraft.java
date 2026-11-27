package br.com.vozdacriacao;
import net.minecraft.item.*;
import net.minecraft.entity.effect.*;
import net.minecraft.registry.*;
import net.minecraft.particle.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.joml.Vector3f;
import java.util.List;
import net.minecraft.client.item.TooltipContext;

public final class ArtefatoLaCraft extends Item {
    static final Identifier ID=new Identifier("vozdacriacao","artefato");
    static final ArtefatoLaCraft ITEM=new ArtefatoLaCraft();
    private ArtefatoLaCraft(){super(new Item.Settings().maxCount(1).rarity(Rarity.RARE));}
    static void registrar(){Registry.register(Registries.ITEM,ID,ITEM);}
    static boolean entregar(ServerPlayerEntity p,Pedido pedido){
        try{ArtefatoSpec.ler(pedido.alvo());}catch(RuntimeException e){return false;}
        ItemStack s=new ItemStack(ITEM);s.getOrCreateNbt().putString("lacraft_spec",pedido.alvo());s.setCustomName(Text.literal(pedido.nome()));
        if(!p.getInventory().insertStack(s))p.dropItem(s,false);return true;
    }
    static ArtefatoSpec spec(ItemStack s){
        try{return ArtefatoSpec.ler(s.getOrCreateNbt().getString("lacraft_spec"));}catch(RuntimeException e){return null;}
    }
    @Override public TypedActionResult<ItemStack> use(World w,net.minecraft.entity.player.PlayerEntity player,Hand hand){
        ItemStack s=player.getStackInHand(hand);ArtefatoSpec a=spec(s);
        if(a==null||player.isSpectator()||player.getItemCooldownManager().isCoolingDown(this))return TypedActionResult.fail(s);
        if(!w.isClient&&player instanceof ServerPlayerEntity p){
            var sw=p.getServerWorld();int ticks=a.duracao()*20;
            p.getItemCooldownManager().set(this,a.recarga()*20);
            for(String efeito:a.efeitos()){
                StatusEffect ef=switch(efeito){
                    case "respirar"->StatusEffects.WATER_BREATHING;
                    case "velocidade"->StatusEffects.SPEED;
                    case "salto"->StatusEffects.JUMP_BOOST;
                    case "invisivel"->StatusEffects.INVISIBILITY;
                    case "resistencia"->StatusEffects.RESISTANCE;
                    case "levitacao"->StatusEffects.SLOW_FALLING;
                    case "brilho"->StatusEffects.GLOWING;
                    default->null;
                };
                if(ef!=null)p.addStatusEffect(new StatusEffectInstance(ef,ticks,0));
                if(efeito.equals("cura"))p.heal(4);
                if(efeito.equals("levitacao"))p.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION,Math.min(40,ticks),0));
                if(efeito.equals("nuvem"))sw.spawnParticles(ParticleTypes.CLOUD,p.getX(),p.getY()+1,p.getZ(),35,.8,.4,.8,.04);
                if(efeito.equals("musica"))sw.playSound(null,p.getBlockPos(),net.minecraft.sound.SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,net.minecraft.sound.SoundCategory.PLAYERS,.6f,1.2f);
            }
            int c=a.cor();var dust=new DustParticleEffect(new Vector3f(((c>>16)&255)/255f,((c>>8)&255)/255f,(c&255)/255f),1.3f);
            sw.spawnParticles(dust,p.getX(),p.getY()+1,p.getZ(),40,.6,.7,.6,.05);
            VozDaCriacao.som(sw,p.getBlockPos(),1);
        }
        return TypedActionResult.success(s,w.isClient);
    }
    @Override public boolean hasGlint(ItemStack s){return spec(s)!=null;}
    @Override public void appendTooltip(ItemStack s,World w,List<Text> tooltip,TooltipContext ctx){
        ArtefatoSpec a=spec(s);if(a!=null){
            tooltip.add(Text.literal("Efeitos: "+String.join(", ",a.efeitos())));
            tooltip.add(Text.literal("Duração: "+a.duracao()+" s | Recarga: "+a.recarga()+" s"));
            tooltip.add(Text.literal("Botão direito para ativar"));
        }
    }
}
