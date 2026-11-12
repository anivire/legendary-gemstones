package name.modid.core.mixins.modifiers;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import name.modid.core.content.events.handlers.EventOnBeforeBlockBreak;
import name.modid.core.content.events.handlers.EventOnBlockBreak;
import name.modid.core.utils.amber.AmberMagnetHelper;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;

@Mixin(ServerPlayerInteractionManager.class)
public class OnBlockBreak {
  @Shadow
  @Final
  protected ServerPlayerEntity player;

  @Inject(method = "tryBreakBlock", at = @At("HEAD"))
  private void beginBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir,
      @Share("brokenState") LocalRef<BlockState> brokenState) {
    brokenState.set(this.player.getServerWorld().getBlockState(pos));
    AmberMagnetHelper.beginBreak(this.player);
    EventOnBeforeBlockBreak.beginBreak(this.player, pos);
  }

  @Inject(method = "tryBreakBlock", at = @At("RETURN"))
  private void endBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir,
      @Share("brokenState") LocalRef<BlockState> brokenState) {
    try {
      if (cir.getReturnValue()) {
        EventOnBlockBreak.setupEvent(this.player.getServerWorld(), this.player, pos, brokenState.get(), null);
      }
    } finally {
      EventOnBeforeBlockBreak.endBreak();
      AmberMagnetHelper.endBreak();
    }
  }
}
