package name.modid.core.mixins.modifiers.magnet;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import name.modid.core.utils.amber.AmberMagnetHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@Mixin(Block.class)
public class AmberMagnetBlockDrops {
  @Inject(method = "dropStack(Lnet/minecraft/world/World;Ljava/util/function/Supplier;Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
  private static void collectDrop(World world, Supplier<ItemEntity> itemEntitySupplier, ItemStack stack,
      CallbackInfo ci) {
    if (AmberMagnetHelper.tryCollect(world, stack)) {
      ci.cancel();
    }
  }
}
