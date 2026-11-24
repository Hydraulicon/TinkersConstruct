package slimeknights.tconstruct.library.utils;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.ArrayList;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.BiMap;
import com.google.common.collect.Maps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import slimeknights.mantle.client.ResourceColorManager;
import slimeknights.mantle.data.listener.ISafeManagerReloadListener;
import slimeknights.tconstruct.TConstruct;

import java.util.List;
import java.util.Map;

/**
 * Harvest level display names
 */
public class HarvestTiers {
  private HarvestTiers() {}

  /** Tiers from weakest to strongest. NeoForge 1.21 removed the tier sorting registry, so we keep our own */
  private static final List<Tier> SORTED_TIERS = new ArrayList<>();
  /** Map of tier names to tiers */
  private static final BiMap<ResourceLocation,Tier> TIER_NAMES = HashBiMap.create();
  static {
    // same names and order the tier sorting registry used, so existing JSON keeps working
    register(ResourceLocation.withDefaultNamespace("wood"), Tiers.WOOD, null);
    register(ResourceLocation.withDefaultNamespace("gold"), Tiers.GOLD, Tiers.WOOD);
    register(ResourceLocation.withDefaultNamespace("stone"), Tiers.STONE, Tiers.GOLD);
    register(ResourceLocation.withDefaultNamespace("iron"), Tiers.IRON, Tiers.STONE);
    register(ResourceLocation.withDefaultNamespace("diamond"), Tiers.DIAMOND, Tiers.IRON);
    register(ResourceLocation.withDefaultNamespace("netherite"), Tiers.NETHERITE, Tiers.DIAMOND);
  }

  /**
   * Registers a tier so it can be used as a tool tier stat. Should be called during mod construction.
   * @param name   Tier name, used in JSON
   * @param tier   Tier instance
   * @param after  Tier this tier is directly stronger than, null to make it the weakest tier
   */
  public static synchronized void register(ResourceLocation name, Tier tier, @Nullable Tier after) {
    if (TIER_NAMES.containsKey(name) || TIER_NAMES.containsValue(tier)) {
      throw new IllegalArgumentException("Duplicate harvest tier " + name);
    }
    int index = after == null ? 0 : SORTED_TIERS.indexOf(after) + 1;
    if (after != null && index == 0) {
      throw new IllegalArgumentException("Cannot register tier " + name + " after unregistered tier " + after);
    }
    SORTED_TIERS.add(index, tier);
    TIER_NAMES.put(name, tier);
  }

  /** Gets the tier with the given name, or null if unregistered */
  @Nullable
  public static Tier byName(ResourceLocation name) {
    return TIER_NAMES.get(name);
  }

  /** Gets the ID of the given tier, or null if unregistered */
  @Nullable
  public static ResourceLocation getId(Tier tier) {
    return TIER_NAMES.inverse().get(tier);
  }

  /** Gets all registered tiers from weakest to strongest */
  public static List<Tier> getSortedTiers() {
    return Collections.unmodifiableList(SORTED_TIERS);
  }

  /** Checks if the given tier can harvest the given block */
  public static boolean isCorrectTierForDrops(Tier tier, BlockState state) {
    return !state.is(tier.getIncorrectBlocksForDrops());
  }

  /** Cache of name for each tier */
  private static final Map<Tier, Component> harvestLevelNames = Maps.newHashMap();
  /** Listener to clear name cache so we get new colors */
  public static final ISafeManagerReloadListener RELOAD_LISTENER = manager -> harvestLevelNames.clear();

  /** Makes a translation key for the given name */
  private static MutableComponent makeLevelKey(Tier tier) {
    String key = Util.makeTranslationKey("harvest_tier", getId(tier));
    TextColor color = ResourceColorManager.getTextColor(key);
    return TConstruct.makeTranslation("stat", key).withStyle(style -> style.withColor(color));
  }

  /**
   * Gets the harvest level name for the given level number
   * @param tier  Tier
   * @return  Level name
   */
  public static Component getName(Tier tier) {
    return harvestLevelNames.computeIfAbsent(tier, n ->  makeLevelKey(tier));
  }

  /** Gets the larger of two tiers */
  public static Tier max(Tier a, Tier b) {
    List<Tier> sorted = SORTED_TIERS;
    // note indexOf returns -1 if the tier is missing, so the larger of an unsorted tier and a sorted one is the sorted one
    if (sorted.indexOf(b) > sorted.indexOf(a)) {
      return b;
    }
    return a;
  }

  /** Gets the smaller of two tiers */
  public static Tier min(Tier a, Tier b) {
    List<Tier> sorted = SORTED_TIERS;
    // note indexOf returns -1 if the tier is missing, so the smaller of an unsorted tier and a sorted one is the unsorted one
    if (sorted.indexOf(b) < sorted.indexOf(a)) {
      return b;
    }
    return a;
  }

  /** Gets the smallest tier in the sorting registry */
  public static Tier minTier() {
    List<Tier> sortedTiers = SORTED_TIERS;
    if (sortedTiers.isEmpty()) {
      TConstruct.LOG.error("No sorted tiers exist, this should not happen");
      return Tiers.WOOD;
    }
    return sortedTiers.get(0);
  }
}
