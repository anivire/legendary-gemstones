package name.modid.core.api.modifiers.config.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import name.modid.core.api.modifiers.config.GemstoneModifier;
import name.modid.core.api.modifiers.config.ModifierConfig;
import name.modid.core.api.modifiers.config.ModifierConfig.AfterDeathConfig;
import name.modid.core.api.modifiers.config.ModifierContext;
import name.modid.core.api.modifiers.config.ModifierHandler;
import name.modid.core.api.modifiers.types.ModifierItemCategory;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;

public class AfterDeathHandler implements ModifierHandler<ModifierConfig.AfterDeathConfig> {
  private final Map<String, java.util.function.BiConsumer<List<GemstoneModifier>, ModifierContext>> handlers = Map.of(
      "AFTER_DEATH_ADDITIONAL_EXP_GAIN", this::handleBonusExpGain);

  private static final List<String> ORDER = List.of(
      "AFTER_DEATH_ADDITIONAL_EXP_GAIN");

  @Override
  public boolean supports(GemstoneModifier modifier) {
    return modifier.getItemCategory() == ModifierItemCategory.ARMOR;
  }

  @Override
  public void apply(ArrayList<GemstoneModifier> modifiers, ModifierContext ctx) {
    if (modifiers.isEmpty()) {
      return;
    }

    Map<String, List<GemstoneModifier>> grouped = modifiers.stream()
        .collect(Collectors.groupingBy(inst -> ((AfterDeathConfig) inst.getConfig()).eventType().getName()));

    for (String key : ORDER) {
      var group = grouped.get(key);
      if (group == null || group.isEmpty())
        continue;

      var fn = handlers.get(key);
      if (fn != null)
        fn.accept(group, ctx);
    }
  }

  private void handleBonusExpGain(
      List<GemstoneModifier> modifiers,
      ModifierContext ctx) {
    if (!(ctx.getOwner() instanceof LivingEntity owner)
        || !(ctx.getTarget() instanceof LivingEntity target)) {
      return;
    }

    float bonusPercent = (float) modifiers.stream()
        .mapToDouble(m -> ((AfterDeathConfig) m.getConfig()).values().get(m.getRarityType())).sum();

    if (bonusPercent <= 0.0f) {
      return;
    }

    int vanillaXp = target.getXpToDrop(ctx.getWorld(), owner);

    if (vanillaXp <= 0) {
      return;
    }

    float result = vanillaXp * bonusPercent;
    int xp = Math.max(0, Math.round(result));

    if (xp <= 0) {
      return;
    }

    ctx.getWorld().spawnEntity(new ExperienceOrbEntity(
        ctx.getWorld(),
        target.getX(),
        target.getY(),
        target.getZ(),
        xp));
  }
}
