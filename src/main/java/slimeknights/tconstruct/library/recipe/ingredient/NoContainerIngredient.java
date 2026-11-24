package slimeknights.tconstruct.library.recipe.ingredient;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.shared.TinkerCommons;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Ingredient matching an item with no container item, used to ensure NBT fluid items are empty.
 * @param match  Nested ingredient to match
 */
public record NoContainerIngredient(Ingredient match) implements ICustomIngredient {
  public static final RecordLoadable<NoContainerIngredient> LOADER = RecordLoadable.create(
    Loadables.ITEM_INGREDIENT_NONEMPTY.requiredField("match", NoContainerIngredient::match),
    NoContainerIngredient::new);

  @Override
  public boolean test(ItemStack stack) {
    return match.test(stack) && !stack.hasCraftingRemainingItem();
  }

  @Override
  public Stream<ItemStack> getItems() {
    return Arrays.stream(match.getItems());
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return TinkerCommons.noContainerIngredient.get();
  }


  /* Static constructors */

  /** Creates an instance from the given nested ingredient */
  public static Ingredient of(Ingredient ingredient) {
    return new NoContainerIngredient(ingredient).toVanilla();
  }

  /** Creates an instance from the given items */
  public static Ingredient of(ItemLike... items) {
    return of(Ingredient.of(items));
  }

  /** Creates an instance from the given stacks */
  public static Ingredient of(ItemStack... stacks) {
    return of(Ingredient.of(stacks));
  }

  /** Creates an instance from the given tag */
  public static Ingredient of(TagKey<Item> tag) {
    return of(Ingredient.of(tag));
  }
}
