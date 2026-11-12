package name.modid.core.mixins.gemstones;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import name.modid.core.api.modifiers.helpers.GemstoneSlotHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;

@Mixin(AnvilScreenHandler.class)
public abstract class PreserveGemstonesOnAnvil {
  @Inject(method = "updateResult", at = @At("RETURN"))
  private void onUpdateResult(CallbackInfo ci) {
    AnvilScreenHandler handler = (AnvilScreenHandler) (Object) this;
    if (GemstoneSlotHelper.hasInsertedGemstones(handler.getSlot(1).getStack())) {
      handler.getSlot(2).setStack(ItemStack.EMPTY);
    }
  }
}
