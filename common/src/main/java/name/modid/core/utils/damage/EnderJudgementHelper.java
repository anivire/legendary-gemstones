package name.modid.core.utils.damage;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.LivingEntity;

// Ender Judgement deals exactly the damage needed to kill instead of Float.MAX_VALUE, which other
// multipliers overflowed into Infinity and vanilla absorption math turned into NaN health
public final class EnderJudgementHelper {
  // Covers float rounding when the absorption is subtracted back out of the
  // damage
  private static final float LETHAL_DAMAGE_MARGIN = 1.0F;

  // Melee damage is applied synchronously on the server thread, so a single armed
  // hit is enough
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

  // Called with the final damage after armor, enchantments, effects and other
  // mods, right before absorption
  public static float applyExecute(LivingEntity target, float damage) {
    if (target != armedTarget) {
      return damage;
    }

    // Only the first damage application of the hit is replaced
    disarm();

    // Fully blocked hits (Resistance V, boss immunity phases) must not turn into an
    // execute
    if (damage <= 0.0F) {
      return damage;
    }

    return Math.max(damage, getLethalDamage(target));
  }
}
