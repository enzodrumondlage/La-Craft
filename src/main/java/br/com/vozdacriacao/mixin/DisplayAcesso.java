package br.com.vozdacriacao.mixin;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.AffineTransformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(DisplayEntity.class)
public interface DisplayAcesso {
    @Invoker("setTransformation") void lacraft$transformar(AffineTransformation t);
    @Invoker("setInterpolationDuration") void lacraft$duracao(int n);
    @Invoker("setStartInterpolation") void lacraft$inicio(int n);
}
