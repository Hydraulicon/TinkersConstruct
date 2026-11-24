package slimeknights.tconstruct.library.modifiers.hook.behavior;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.mining.BlockHarvestModifierHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * This interface exposes two methods, {@link #updateEnchantmentLevel(IToolStackView, ModifierEntry, Holder, int)} and {@link #updateEnchantments(IToolStackView, ModifierEntry, HolderLookup.RegistryLookup, Map)}
 * to allow tools to claim to have enchantments to vanilla APIs without modifying NBT. For performance reasons we don't simply have one hook call the other, but their behavior must be consistent.
 * That is, whatever change you make to the level in {@link #updateEnchantmentLevel(IToolStackView, ModifierEntry, Holder, int)} must also be reflected in the map in {@link #updateEnchantments(IToolStackView, ModifierEntry, HolderLookup.RegistryLookup, Map)}.
 * <p>
 * As of 1.21, enchantments are a datapack registry, so they are passed as holders.
 */
public interface EnchantmentModifierHook {
  /** Predicate to remove entries with no level */
  Predicate<Integer> VALUE_REMOVER = value -> value == null || value <= 0;

  /**
   * Updates the level of the given enchantment on the tool
   * @param tool         Tool instance
   * @param modifier     Modifier instance
   * @param enchantment  Enchantment to update
   * @param level        Level before this hook ran
   * @return  New level, may be negative to cancel out levels from other modifiers
   */
  int updateEnchantmentLevel(IToolStackView tool, ModifierEntry modifier, Holder<Enchantment> enchantment, int level);

  /**
   * Updates the map of all enchantments on the tool
   * @param tool      Tool instance
   * @param modifier  Modifier instance
   * @param lookup    Enchantment registry, for fetching enchantments to add
   * @param map       Map of enchantment to level, may contain negative levels while hooks run
   */
  void updateEnchantments(IToolStackView tool, ModifierEntry modifier, HolderLookup.RegistryLookup<Enchantment> lookup, Map<Holder<Enchantment>,Integer> map);

  /** Adds the given level to the enchantment in the map */
  static void addEnchantment(Map<Holder<Enchantment>,Integer> map, Holder<Enchantment> enchantment, int amount) {
    if (amount != 0) {
      map.put(enchantment, map.getOrDefault(enchantment, 0) + amount);
    }
  }

  /** Gets the level of an enchantment on the given stack, including levels from modifiers */
  static int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
    int level = stack.getTagEnchantments().getLevel(enchantment);
    IToolStackView tool = ToolStack.from(stack);
    for (ModifierEntry entry : tool.getModifierList()) {
      level = entry.getHook(ModifierHooks.ENCHANTMENTS).updateEnchantmentLevel(tool, entry, enchantment, level);
    }
    // we allow hooks to return negative, such as to cancel out an enchantment
    return Math.max(level, 0);
  }

  /** Gets all enchantments on the given stack, including levels from modifiers */
  static ItemEnchantments getAllEnchantments(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> lookup) {
    Map<Holder<Enchantment>,Integer> enchantments = new LinkedHashMap<>();
    for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getTagEnchantments().entrySet()) {
      enchantments.put(entry.getKey(), entry.getIntValue());
    }
    IToolStackView tool = ToolStack.from(stack);
    for (ModifierEntry entry : tool.getModifierList()) {
      entry.getHook(ModifierHooks.ENCHANTMENTS).updateEnchantments(tool, entry, lookup, enchantments);
    }
    // we allow hooks to return negative, such as to cancel out an enchantment
    enchantments.values().removeIf(VALUE_REMOVER);
    ItemEnchantments.Mutable result = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
    enchantments.forEach(result::set);
    return result.toImmutable();
  }

  /** Merger that runs all submodules */
  record AllMerger(Collection<EnchantmentModifierHook> modules) implements EnchantmentModifierHook {
    @Override
    public int updateEnchantmentLevel(IToolStackView tool, ModifierEntry modifier, Holder<Enchantment> enchantment, int level) {
      for (EnchantmentModifierHook module : modules) {
        level = module.updateEnchantmentLevel(tool, modifier, enchantment, level);
      }
      return level;
    }

    @Override
    public void updateEnchantments(IToolStackView tool, ModifierEntry modifier, HolderLookup.RegistryLookup<Enchantment> lookup, Map<Holder<Enchantment>,Integer> map) {
      for (EnchantmentModifierHook module : modules) {
        module.updateEnchantments(tool, modifier, lookup, map);
      }
    }
  }

  /** Helper for a hook that adds a single enchantment */
  @SuppressWarnings("unused") // API
  interface SingleEnchantment extends EnchantmentModifierHook {
    /** Gets the enchantment to add */
    ResourceKey<Enchantment> getEnchantment(IToolStackView tool, ModifierEntry modifier);

    /** Gets the level to add */
    default int getEnchantmentLevel(IToolStackView tool, ModifierEntry modifier) {
      return modifier.getLevel();
    }

    @Override
    default int updateEnchantmentLevel(IToolStackView tool, ModifierEntry modifier, Holder<Enchantment> enchantment, int level) {
      if (enchantment.is(getEnchantment(tool, modifier))) {
        level += getEnchantmentLevel(tool, modifier);
      }
      return level;
    }

    @Override
    default void updateEnchantments(IToolStackView tool, ModifierEntry modifier, HolderLookup.RegistryLookup<Enchantment> lookup, Map<Holder<Enchantment>,Integer> map) {
      Optional<Holder.Reference<Enchantment>> enchantment = lookup.get(getEnchantment(tool, modifier));
      enchantment.ifPresent(holder -> addEnchantment(map, holder, getEnchantmentLevel(tool, modifier)));
    }
  }

  /** Helper for a hook that adds a single enchantment only while harvesting */
  interface SingleHarvestEnchantment extends SingleEnchantment, BlockHarvestModifierHook.MarkHarvesting {
    @Override
    default int updateEnchantmentLevel(IToolStackView tool, ModifierEntry modifier, Holder<Enchantment> enchantment, int level) {
      if (BlockHarvestModifierHook.MarkHarvesting.isHarvesting(tool)) {
        return SingleEnchantment.super.updateEnchantmentLevel(tool, modifier, enchantment, level);
      }
      return level;
    }

    @Override
    default void updateEnchantments(IToolStackView tool, ModifierEntry modifier, HolderLookup.RegistryLookup<Enchantment> lookup, Map<Holder<Enchantment>,Integer> map) {
      if (BlockHarvestModifierHook.MarkHarvesting.isHarvesting(tool)) {
        SingleEnchantment.super.updateEnchantments(tool, modifier, lookup, map);
      }
    }
  }
}
