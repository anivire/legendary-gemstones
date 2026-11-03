package name.modid.datapack.geodes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

import name.modid.core.api.modifiers.types.GemstoneQuality;
import name.modid.core.api.modifiers.types.GemstoneType;
import net.minecraft.util.Identifier;

public class GeodesConfig {
  @SerializedName("geode_id")
  public String geodeId;
  @SerializedName("drop_chance")
  public Float dropChance;
  @SerializedName("ore_tags")
  public List<Identifier> oreTags = List.of();
  public List<Identifier> ores = List.of();
  public Map<GemstoneType, Float> gemstones;
  public Map<GemstoneQuality, Float> qualities;
  @SerializedName("quality_overrides")
  public Map<GemstoneType, Map<GemstoneQuality, Float>> qualityOverrides = Map.of();

  public void normalize() {
    if (oreTags == null) {
      oreTags = List.of();
    }
    if (ores == null) {
      ores = List.of();
    }

    gemstones = withoutInvalidEntries(gemstones);
    qualities = withoutInvalidEntries(qualities);
    qualityOverrides = withoutInvalidEntries(qualityOverrides);
    qualityOverrides.replaceAll((type, overrides) -> withoutInvalidEntries(overrides));
  }

  // Gson turns an unknown enum name into a null key
  private static <K, V> Map<K, V> withoutInvalidEntries(Map<K, V> values) {
    Map<K, V> result = values != null ? new LinkedHashMap<>(values) : new LinkedHashMap<>();
    result.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
    return result;
  }
}
