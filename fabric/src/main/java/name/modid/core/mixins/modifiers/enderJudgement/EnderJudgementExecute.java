package name.modid.core.mixins.modifiers.enderJudgement;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import name.modid.core.utils.damage.EnderJudgementHelper;
import net.minecraft.entity.LivingEntity;

@Mixin(LivingEntity.class)
public abstract class EnderJudgementExecute {
  @ModifyExpressionValue(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;modifyAppliedDamage(Lnet/minecraft/entity/damage/DamageSource;F)F"))
  private float executeEnderJudgement(float damage) {
    return EnderJudgementHelper.applyExecute((LivingEntity) (Object) this, damage);
  }
}
