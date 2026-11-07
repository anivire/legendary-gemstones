package name.modid.core.utils.damage;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.LivingEntity;

// Ender Judgement deals exactly the damage needed to kill instead of Float.MAX_VALUE, which other
// multipliers overflowed and vanilla absorption math turned into NaN health
public final class EnderJudgementHelper {
  private static final float LETHAL_DAMAGE_MARGIN = 1.0F;

  @Nullable
  private static LivingEntity armedTarget;

  private EnderJudgementHelper() {
  }

  public static void arm(LivingEntity target) {
    armedTarget = target;
  }

  public static void disarm() {
    armedTarget = null;
  }

  public static float getLethalDamage(LivingEntity target) {
    return target.getHealth() + target.getAbsorptionAmount() + LETHAL_DAMAGE_MARGIN;
  }

  public static float applyExecute(LivingEntity target, float damage) {
    if (target != armedTarget) {
      return damage;
    }

    disarm();

    // Fully blocked hits (Resistance V, boss immunity phases) must not turn into an
    // execute
    if (damage <= 0.0F) {
      return damage;
    }

    return Math.max(damage, getLethalDamage(target));
  }
}
