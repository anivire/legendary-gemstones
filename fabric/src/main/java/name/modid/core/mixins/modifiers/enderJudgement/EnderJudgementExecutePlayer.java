package name.modid.core.mixins.modifiers.enderJudgement;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import name.modid.core.utils.damage.EnderJudgementHelper;
import net.minecraft.entity.player.PlayerEntity;

// PlayerEntity overrides applyDamage, so the LivingEntity injection never runs for players
@Mixin(PlayerEntity.class)
public abstract class EnderJudgementExecutePlayer {
  @ModifyExpressionValue(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;modifyAppliedDamage(Lnet/minecraft/entity/damage/DamageSource;F)F"))
  private float executeEnderJudgement(float damage) {
    return EnderJudgementHelper.applyExecute((PlayerEntity) (Object) this, damage);
  }
}
