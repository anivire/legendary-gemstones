package name.modid.core.utils.tooltip;

import java.util.Map;

import name.modid.core.api.modifiers.config.ModifierConfig;
import name.modid.core.api.modifiers.helpers.GemstoneAttributeCapHelper;
import name.modid.core.api.modifiers.types.ModifierItemCategory;
import name.modid.core.content.registries.AttributesRegistry;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class AttributeValueFormat {
  private static final Map<Identifier, String> NAME_OVERRIDES = Map.of(
      Identifier.ofVanilla("generic.scale"), "tooltip.gemstones.attribute_name.player_scale");

  private AttributeValueFormat() {
  }

  public static boolean hasDiminishingStack(ModifierConfig.AttributeConfig cfg, ModifierItemCategory category) {
    return cfg.attribute().value() == AttributesRegistry.EVASION
        || GemstoneAttributeCapHelper.isCapped(cfg, category);
  }

  public static MutableText name(ModifierConfig.AttributeConfig cfg) {
    String key = cfg.attribute().getKey()
        .map(k -> NAME_OVERRIDES.get(k.getValue()))
        .orElse(null);
    return Text.translatable(key != null ? key : cfg.attribute().value().getTranslationKey().toLowerCase());
  }

  private static boolean isPercent(ModifierConfig.AttributeConfig cfg) {
    return cfg.operation() != EntityAttributeModifier.Operation.ADD_VALUE;
  }

  public static double toDisplay(ModifierConfig.AttributeConfig cfg, double value) {
    return isPercent(cfg) ? value * 100 : value;
  }

  public static String postfix(ModifierConfig.AttributeConfig cfg) {
    return isPercent(cfg) ? "%" : "";
  }
}
