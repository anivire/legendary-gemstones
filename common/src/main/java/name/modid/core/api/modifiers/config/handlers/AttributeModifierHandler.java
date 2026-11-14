package name.modid.core.api.modifiers.config.handlers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import name.modid.Gemstones;
import name.modid.core.api.modifiers.config.GemstoneModifier;
import name.modid.core.api.modifiers.config.ModifierConfig.AttributeConfig;
import name.modid.core.api.modifiers.config.ModifierConfig.MultiplyAttributeConfig;
import name.modid.core.api.modifiers.helpers.ModifierHelper;
import name.modid.core.api.modifiers.types.GemstoneQuality;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class AttributeModifierHandler {
  private static final float MIN_SCALE_TOTAL = -0.72F;

  private record AttributeGroupKey(RegistryEntry<EntityAttribute> attribute, Operation operation) {
  }

  public static void apply(ArrayList<GemstoneModifier> gemstoneModifiers, ItemStack itemStack) {
    AttributeModifiersComponent expected = buildComponent(gemstoneModifiers, itemStack);
    if (expected == null) {
      return;
    }

    if (expected.modifiers().isEmpty()) {
      itemStack.remove(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      return;
    }

    itemStack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, expected);
  }

  private static AttributeModifiersComponent buildComponent(
      ArrayList<GemstoneModifier> gemstoneModifiers,
      ItemStack itemStack) {
    List<Entry> gemstoneEntries = buildGemstoneEntries(gemstoneModifiers,
        itemStack.getItem());

    AttributeModifiersComponent current = itemStack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS,
        AttributeModifiersComponent.DEFAULT);

    if (gemstoneEntries.isEmpty() && !hasGemstoneEntries(current)) {
      return null;
    }

    Map<String, Entry> combinedModifiersMap = new LinkedHashMap<>();
    Function<Entry, String> entryKey = e -> String.format("%s.%s.%s", e.modifier().id(),
        e.slot(), e.attribute().value());

    for (Entry entry : getDefaultAttributeEntries(itemStack)) {
      combinedModifiersMap.put(entryKey.apply(entry), entry);
    }

    for (Entry entry : current.modifiers()) {
      if (isGemstoneEntry(entry)) {
        continue;
      }

      combinedModifiersMap.put(entryKey.apply(entry), entry);
    }

    for (Entry entry : gemstoneEntries) {
      combinedModifiersMap.put(entryKey.apply(entry), entry);
    }

    return new AttributeModifiersComponent(new ArrayList<>(combinedModifiersMap.values()), current.showInTooltip());
  }

  @SuppressWarnings("deprecation")
  private static List<Entry> getDefaultAttributeEntries(ItemStack itemStack) {
    Item item = itemStack.getItem();
    List<Entry> entries = new ArrayList<>(item.getComponents()
        .getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT).modifiers());
    entries.addAll(item.getAttributeModifiers().modifiers());
    return entries;
  }

  private static List<AttributeModifiersComponent.Entry> buildGemstoneEntries(
      ArrayList<GemstoneModifier> gemstoneModifiers,
      Item item) {
    // Values are summed only within the same attribute and operation, otherwise
    // a TOTAL penalty could be merged into a BASE bonus (and vice versa)
    Map<AttributeGroupKey, List<GemstoneModifier>> groupedModifiers = new LinkedHashMap<>();
    Map<RegistryEntry<EntityAttribute>, Integer> operationsPerAttribute = new LinkedHashMap<>();

    for (GemstoneModifier modifier : gemstoneModifiers) {
      for (AttributeConfig config : getAttributeConfigs(modifier)) {
        AttributeGroupKey key = new AttributeGroupKey(config.attribute(), config.operation());
        if (!groupedModifiers.containsKey(key)) {
          operationsPerAttribute.merge(config.attribute(), 1, Integer::sum);
        }
        groupedModifiers.computeIfAbsent(key, k -> new ArrayList<>()).add(modifier);
      }
    }

    List<AttributeModifiersComponent.Entry> gemstoneEntries = new ArrayList<>();
    for (Map.Entry<AttributeGroupKey, List<GemstoneModifier>> group : groupedModifiers.entrySet()) {
      AttributeGroupKey key = group.getKey();
      float totalValue = 0f;
      GemstoneModifier firstModifier = null;

      for (GemstoneModifier modifier : group.getValue()) {
        GemstoneQuality rarity = modifier.getRarityType();
        for (AttributeConfig config : getAttributeConfigs(modifier)) {
          if (config.attribute().equals(key.attribute()) && config.operation() == key.operation()) {
            totalValue += config.values().get(rarity);
            if (firstModifier == null) {
              firstModifier = modifier;
            }
          }
        }
      }

      if (firstModifier == null) {
        continue;
      }

      if (key.attribute().equals(EntityAttributes.GENERIC_SCALE) && key.operation() == Operation.ADD_MULTIPLIED_TOTAL) {
        totalValue = Math.max(totalValue, MIN_SCALE_TOTAL);
      }

      EquipmentSlot slot = ModifierHelper.getEquipmentSlot(item);
      String path = String.format("%s.%s.%s",
          firstModifier.getGemstoneType().toString().toLowerCase(),
          firstModifier.getItemCategory().toString().toLowerCase(),
          slot.name().toLowerCase());

      // Keep the old id when the attribute has a single operation, so existing items
      // stay stable
      if (operationsPerAttribute.getOrDefault(key.attribute(), 1) > 1) {
        path = path + "." + key.operation().asString();
      }

      EntityAttributeModifier scaledGemstoneModifier = new EntityAttributeModifier(
          Identifier.of(Gemstones.MOD_ID, path),
          (double) totalValue,
          key.operation());

      gemstoneEntries.add(new AttributeModifiersComponent.Entry(
          key.attribute(),
          scaledGemstoneModifier,
          ModifierHelper.getAttributeModifierSlot(item)));
    }

    return gemstoneEntries;
  }

  public static List<AttributeConfig> getAttributeConfigs(GemstoneModifier modifier) {
    if (modifier.getConfig() instanceof AttributeConfig single) {
      return List.of(single);
    }

    if (modifier.getConfig() instanceof MultiplyAttributeConfig multi) {
      return multi.instances();
    }

    return List.of();
  }

  public static boolean isGemstoneEntry(Entry entry) {
    return entry.modifier().id() != null
        && entry.modifier().id().getNamespace().equalsIgnoreCase(Gemstones.MOD_ID);
  }

  public static boolean hasGemstoneAttributeModifiers(ItemStack itemStack) {
    AttributeModifiersComponent component = itemStack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS,
        AttributeModifiersComponent.DEFAULT);
    return hasGemstoneEntries(component);
  }

  private static boolean hasGemstoneEntries(AttributeModifiersComponent component) {
    return component.modifiers().stream().anyMatch(AttributeModifierHandler::isGemstoneEntry);
  }
}
