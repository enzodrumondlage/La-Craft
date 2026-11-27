package br.com.vozdacriacao.mixin;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class GiganteRenderer {
    @Inject(method="render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/entity/LivingEntityRenderer;scale(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/util/math/MatrixStack;F)V",shift=At.Shift.AFTER),require=0)
    private void lacraft$gigante(LivingEntity e,float yaw,float delta,MatrixStack m,VertexConsumerProvider v,int light,CallbackInfo ci){
        if(e.hasCustomName()&&e.getCustomName().getString().startsWith("La Craft Gigante "))m.scale(3f,3f,3f);
    }
}
