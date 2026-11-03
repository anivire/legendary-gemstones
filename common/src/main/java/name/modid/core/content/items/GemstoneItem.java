package name.modid.core.content.items;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import name.modid.core.api.components.ComponentsRegistry;
import name.modid.core.api.components.PolishingComponent;
import name.modid.core.api.modifiers.config.GemstoneModifier;
import name.modid.core.api.modifiers.config.ModifierConfig;
import name.modid.core.api.modifiers.helpers.ModifierHelper;
import name.modid.core.api.modifiers.types.GemstoneQuality;
import name.modid.core.api.modifiers.types.GemstoneType;
import name.modid.core.api.modifiers.types.ModifierItemCategory;
import name.modid.core.api.tooltips.QualityTooltipData;
import name.modid.core.api.tooltips.TooltipHelper.InlineIcons;
import name.modid.core.api.tooltips.TooltipText;
import name.modid.core.content.items.registries.ItemsRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

public class GemstoneItem extends Item {
  private static final int ITEM_BAR_STEPS = 13;
  private static final int POLISHING_BAR_START_COLOR = 0xFFA64DFF;
  private static final int POLISHING_BAR_END_COLOR = 0xFFFF6FD6;

  protected GemstoneType gemstoneType;
  protected GemstoneQuality rarityType;

  public GemstoneItem(Settings settings, GemstoneType gemstoneType, GemstoneQuality rarityType) {
    super(settings);
    this.gemstoneType = gemstoneType;
    this.rarityType = rarityType;
  }

  public GemstoneType getType() {
    return this.gemstoneType;
  }

  public GemstoneQuality getRarityType() {
    return this.rarityType;
  }

  @Override
  public boolean isItemBarVisible(ItemStack stack) {
    return stack.get(ComponentsRegistry.polishing()) != null;
  }

  @Override
  public int getItemBarStep(ItemStack stack) {
    return Math.round(getPolishingProgress(stack) * ITEM_BAR_STEPS);
  }

  @Override
  public int getItemBarColor(ItemStack stack) {
    return ColorHelper.Argb.lerp(getPolishingProgress(stack), POLISHING_BAR_START_COLOR, POLISHING_BAR_END_COLOR);
  }

  // Progress of the whole polishing to the next quality, from 0 to 1
  private float getPolishingProgress(ItemStack stack) {
    PolishingComponent polishing = stack.get(ComponentsRegistry.polishing());
    int totalStages = this.rarityType.getPolishStages();
    if (polishing == null || totalStages <= 0) {
      return 0.0f;
    }

    float stageProgress = polishing.stageDuration() <= 0
        ? 0.0f
        : (float) polishing.ticksInStage() / polishing.stageDuration();
    return MathHelper.clamp((polishing.completedStages() + stageProgress) / totalStages, 0.0f, 1.0f);
  }

  @Override
  public Optional<TooltipData> getTooltipData(ItemStack stack) {
    GemstoneItem gemstoneItem = (GemstoneItem) stack.getItem();
    List<GemstoneQuality> qualities = getDisplayedQualities(gemstoneItem.getRarityType(), isAmplifierGemstone(stack));
    return Optional.of(new QualityTooltipData(qualities));
  }

  @Override
  public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
    GemstoneItem gemstoneItem = (GemstoneItem) stack.getItem();
    GemstoneType gemstoneType = gemstoneItem.getType();
    Map<ModifierItemCategory, Map<GemstoneQuality, GemstoneModifier>> gemstoneModifiers = new LinkedHashMap<>(
        ModifierHelper.getGemstoneModifiers(gemstoneType, stack.getItem()));
    boolean isAmplifierGemstone = gemstoneModifiers.values().stream()
        .flatMap(rarityMap -> rarityMap.values().stream())
        .anyMatch(modifier -> modifier.getConfig() instanceof ModifierConfig.AmplifierConfig);

    if (!gemstoneModifiers.isEmpty()) {
      tooltip.add(Text.empty());
      tooltip.add(Text.translatable("tooltip.gemstones.gemstone_bonus").formatted(Formatting.GRAY));
    }

    List<ModifierItemCategory> modifierOrder = Arrays.asList(
        ModifierItemCategory.ALL,
        ModifierItemCategory.MELEE,
        ModifierItemCategory.RANGED,
        ModifierItemCategory.TOOLS,
        ModifierItemCategory.ARMOR);

    gemstoneModifiers.entrySet().stream()
        .sorted(Comparator.comparingInt(entry -> modifierOrder.indexOf(entry.getKey())))
        .forEachOrdered(entry -> {
          Map<GemstoneQuality, GemstoneModifier> rarityMap = entry.getValue();
          GemstoneModifier modifier = rarityMap.get(gemstoneItem.getRarityType());

          if (modifier != null && gemstoneType != GemstoneType.LOCKED && gemstoneType != GemstoneType.EMPTY) {
            tooltip.add(modifier.getTooltipText(gemstoneItem.getRarityType(), true));
          }
        });

    if (gemstoneItem.getRarityType() == GemstoneQuality.MYTHIC) {
      tooltip.add(Text.empty());
      tooltip.add(TooltipText.info(
          Text.translatable("tooltip.gemstones.mythic_quality_warning").formatted(Formatting.GRAY)));
    }

    if (isAmplifierGemstone) {
      tooltip.add(Text.empty());
      tooltip.add(TooltipText.info(
          Text.translatable("tooltip.gemstones.amplifier_quality_warning").formatted(Formatting.GRAY)));
      tooltip.add(TooltipText.indented(
          Text.translatable("tooltip.gemstones.amplifier_quality_values").formatted(Formatting.GRAY)));
    }

    PolishingComponent polishing = stack.get(ComponentsRegistry.polishing());
    if (polishing != null) {
      int totalStages = gemstoneItem.getRarityType().getPolishStages();
      int currentStage = Math.min(polishing.completedStages() + 1, totalStages);
      int totalPercent = (int) (getPolishingProgress(stack) * 100);
      // The current stage is not counted once it is paid, a stage is paid when it starts
      int unpaidStages = totalStages - polishing.completedStages() - (polishing.stagePaid() ? 1 : 0);
      int dustLeft = Math.max(unpaidStages, 0) * gemstoneItem.getRarityType().getPolishDustCost();

      tooltip.add(Text.empty());
      tooltip.add(TooltipText.line(
          TooltipText.icon(InlineIcons.POLISHING),
          Text.literal(" "),
          Text.translatable("tooltip.gemstones.polishing.progress",
              Text.literal(currentStage + " / " + totalStages).formatted(Formatting.GOLD),
              totalPercent)
              .formatted(Formatting.GRAY)));
      if (dustLeft > 0) {
        tooltip.add(TooltipText.line(
            TooltipText.icon(InlineIcons.POLISHING_INDENT),
            Text.translatable("tooltip.gemstones.polishing.dust_left",
                Text.literal(String.valueOf(dustLeft)).formatted(Formatting.GOLD),
                ItemsRegistry.gemstoneDust().getDefaultStack().toHoverableText())
                .formatted(Formatting.GRAY)));
      }
    }
  }

  private boolean isAmplifierGemstone(ItemStack stack) {
    GemstoneItem gemstoneItem = (GemstoneItem) stack.getItem();
    return ModifierHelper.getGemstoneModifiers(gemstoneItem.getType(), stack.getItem()).values().stream()
        .flatMap(rarityMap -> rarityMap.values().stream())
        .anyMatch(modifier -> modifier.getConfig() instanceof ModifierConfig.AmplifierConfig);
  }

  private static List<GemstoneQuality> getDisplayedQualities(GemstoneQuality quality, boolean amplifier) {
    return amplifier ? List.of(GemstoneQuality.AMPLIFIER, quality) : List.of(quality);
  }
}
