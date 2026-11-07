package name.modid.core.mixins.gemstones.grindstone;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import name.modid.core.api.modifiers.helpers.GemstoneSlotHelper;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GrindstoneScreenHandler;

@Mixin(GrindstoneScreenHandler.class)
public abstract class PreserveGemstonesOnGrindstone {
  @Shadow
  Inventory input;

  @Inject(method = "updateResult", at = @At("TAIL"))
  private void onUpdateResult(CallbackInfo ci) {
    GrindstoneScreenHandler handler = (GrindstoneScreenHandler) (Object) this;
    ItemStack result = handler.getSlot(2).getStack();
    if (result.isEmpty())
      return;

    // Sockets of only one item can be kept, so combining two items with gemstones #
    // would destroy gems
    if (GemstoneSlotHelper.hasInsertedGemstones(input.getStack(0))
        && GemstoneSlotHelper.hasInsertedGemstones(input.getStack(1))) {
      handler.getSlot(2).setStack(ItemStack.EMPTY);
      return;
    }

    GemstoneSlotHelper.copyGemstones(
        GemstoneSlotHelper.pickSocketSource(List.of(input.getStack(0), input.getStack(1))), result);
  }
}
