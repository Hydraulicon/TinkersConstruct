package slimeknights.tconstruct.library.recipe.display;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Interface for vanilla recipes to allow them to expose filtered recipes. Used since JEI will not filter the list to remove filtered recipes from the recipe manager. */
public interface VanillaFilteredRecipe<T extends FilteredRecipe> {
  /**
   * Gets a list of filtered recipes for adding to a recipe manager plugin.
   * @param id      ID of this recipe
   * @param access  Registry access instance
   * @return  List of recipes
   */
  List<T> getFilteredRecipes(ResourceLocation id, HolderLookup.Provider access);


  /** Gets a  modifiable list of filtered recipes matching the given recipe type. */
  static <T extends FilteredRecipe, I extends RecipeInput, R extends Recipe<I>> List<T> getRecipes(HolderLookup.Provider access, RecipeManager manager, RecipeType<R> type, Class<T> recipeClass) {
    return manager.getAllRecipesFor(type).stream()
      .flatMap(holder -> holder.value() instanceof VanillaFilteredRecipe<?> filtered ? filtered.getFilteredRecipes(holder.id(), access).stream() : Stream.empty())
      .filter(recipeClass::isInstance).map(recipeClass::cast).collect(Collectors.toList());
  }
}
