package br.com.vozdacriacao.mixin;
import br.com.vozdacriacao.SpawnDesejo;
import net.minecraft.entity.Entity;
import net.minecraft.world.ChunkRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ChunkRegion.class)
public abstract class SpawnChunkMixin {
    @Inject(method="spawnEntity",at=@At("HEAD"),cancellable=true,require=0)
    private void lacraft$spawn(Entity entidade,CallbackInfoReturnable<Boolean> cir){if(SpawnDesejo.bloquear(entidade))cir.setReturnValue(false);}
}
