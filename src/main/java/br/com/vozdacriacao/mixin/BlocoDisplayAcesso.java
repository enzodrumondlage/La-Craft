package br.com.vozdacriacao.mixin;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(DisplayEntity.BlockDisplayEntity.class)
public interface BlocoDisplayAcesso {
    @Invoker("setBlockState") void lacraft$bloco(BlockState b);
}
