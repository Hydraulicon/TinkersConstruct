package slimeknights.tconstruct.library.recipe.ingredient;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.Arrays;
import java.util.List;

/**
 * Helpers for sized item ingredients. As of 1.21, Mantle's sized ingredient was replaced by NeoForge's {@link SizedIngredient},
 * these helpers keep the factory methods Tinkers relied on.
 */
public final class SizedIngredients {
  private SizedIngredients() {}

  /** Ingredient matching no item. Sized ingredients require a positive count, so the count is 1 */
  public static final SizedIngredient EMPTY = new SizedIngredient(Ingredient.EMPTY, 1);

  /** Creates a sized ingredient needing 1 item */
  public static SizedIngredient of(Ingredient ingredient) {
    return new SizedIngredient(ingredient, 1);
  }

  /** Creates a sized ingredient */
  public static SizedIngredient of(Ingredient ingredient, int count) {
    return new SizedIngredient(ingredient, count);
  }

  /** Creates a sized ingredient matching the given items */
  public static SizedIngredient fromItems(int count, ItemLike... items) {
    return new SizedIngredient(Ingredient.of(items), count);
  }

  /** Creates a sized ingredient matching one of the given items */
  public static SizedIngredient fromItems(ItemLike... items) {
    return fromItems(1, items);
  }

  /** Creates a sized ingredient matching the given tag */
  public static SizedIngredient fromTag(TagKey<Item> tag, int count) {
    return SizedIngredient.of(tag, count);
  }

  /** Creates a sized ingredient matching one item from the given tag */
  public static SizedIngredient fromTag(TagKey<Item> tag) {
    return fromTag(tag, 1);
  }

  /** Checks if the ingredient matches nothing */
  public static boolean isEmpty(SizedIngredient ingredient) {
    return ingredient.ingredient().isEmpty();
  }

  /** Gets all matching stacks for display, sized to the needed count */
  public static List<ItemStack> getMatchingStacks(SizedIngredient ingredient) {
    return Arrays.asList(ingredient.getItems());
  }
}
