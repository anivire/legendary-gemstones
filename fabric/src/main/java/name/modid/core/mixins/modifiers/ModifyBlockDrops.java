package name.modid.core.mixins.modifiers;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import name.modid.core.content.events.handlers.EventOnBeforeBlockBreak;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

@Mixin(Block.class)
public class ModifyBlockDrops {
  @ModifyReturnValue(method = "getDroppedStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;", at = @At("RETURN"))
  private static List<ItemStack> modifyGemstoneDrops(List<ItemStack> drops,
      @Local(argsOnly = true) BlockState state,
      @Local(argsOnly = true) ServerWorld world,
      @Local(argsOnly = true) BlockPos pos,
      @Local(argsOnly = true) BlockEntity blockEntity,
      @Local(argsOnly = true) Entity entity) {
    return EventOnBeforeBlockBreak.modifyDrops(world, pos, state, blockEntity, entity, drops);
  }
}
