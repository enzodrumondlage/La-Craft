package br.com.vozdacriacao.mixin;
import br.com.vozdacriacao.SpawnDesejo;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerWorld.class)
public abstract class SpawnDesejoMixin {
    @Inject(method="spawnEntity",at=@At("HEAD"),cancellable=true,require=1)
    private void lacraft$spawn(Entity entidade,CallbackInfoReturnable<Boolean> cir){if(SpawnDesejo.bloquear(entidade))cir.setReturnValue(false);}
}
