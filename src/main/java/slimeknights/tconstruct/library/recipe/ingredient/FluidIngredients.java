package slimeknights.tconstruct.library.recipe.ingredient;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.CompoundFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.Arrays;
import java.util.List;

/**
 * Helpers for fluid ingredients. As of 1.21, Mantle's fluid ingredient was replaced by NeoForge's {@link SizedFluidIngredient},
 * these helpers provide the operations Tinkers relied on.
 * <p>
 * Unlike Mantle's ingredient, a sized fluid ingredient has a single amount, so compound ingredients must use the same amount for each fluid.
 */
public final class FluidIngredients {
  private FluidIngredients() {}

  /** Ingredient matching no fluid. Sized ingredients require a positive amount, so the amount is 1 */
  public static final SizedFluidIngredient EMPTY = new SizedFluidIngredient(FluidIngredient.empty(), 1);

  /** Checks if the ingredient matches nothing */
  public static boolean isEmpty(SizedFluidIngredient ingredient) {
    return ingredient.ingredient().isEmpty();
  }

  /** Checks if the ingredient matches the given fluid, ignoring amount */
  public static boolean test(SizedFluidIngredient ingredient, Fluid fluid) {
    FluidStack stack = new FluidStack(fluid, 1);
    return !stack.isEmpty() && ingredient.ingredient().test(stack);
  }

  /** Gets the amount needed of the given fluid. As sized ingredients have a single amount, this is the same for every fluid */
  public static int getAmount(SizedFluidIngredient ingredient, Fluid fluid) {
    return ingredient.amount();
  }

  /** Gets all fluids matched by the ingredient, for display */
  public static List<FluidStack> getFluids(SizedFluidIngredient ingredient) {
    return Arrays.asList(ingredient.getFluids());
  }

  /** Creates an ingredient matching a single fluid */
  public static SizedFluidIngredient of(Fluid fluid, int amount) {
    return SizedFluidIngredient.of(fluid, amount);
  }

  /** Creates an ingredient matching a fluid tag */
  public static SizedFluidIngredient of(TagKey<Fluid> tag, int amount) {
    return SizedFluidIngredient.of(tag, amount);
  }

  /** Creates an ingredient matching the fluid in the stack, including its components */
  public static SizedFluidIngredient of(FluidStack stack) {
    return SizedFluidIngredient.of(stack);
  }

  /**
   * Creates an ingredient matching any of the given ingredients.
   * @throws IllegalArgumentException if the ingredients have different amounts, which sized ingredients cannot represent
   */
  public static SizedFluidIngredient of(SizedFluidIngredient... ingredients) {
    return of(List.of(ingredients));
  }

  /**
   * Creates an ingredient matching any of the given ingredients.
   * @throws IllegalArgumentException if the ingredients have different amounts, which sized ingredients cannot represent
   */
  public static SizedFluidIngredient of(List<SizedFluidIngredient> ingredients) {
    if (ingredients.isEmpty()) {
      return EMPTY;
    }
    if (ingredients.size() == 1) {
      return ingredients.get(0);
    }
    int amount = ingredients.get(0).amount();
    for (SizedFluidIngredient ingredient : ingredients) {
      if (ingredient.amount() != amount) {
        throw new IllegalArgumentException("Compound fluid ingredients must use the same amount for each fluid, got " + ingredients);
      }
    }
    return new SizedFluidIngredient(CompoundFluidIngredient.of(ingredients.stream().map(SizedFluidIngredient::ingredient)), amount);
  }
}
