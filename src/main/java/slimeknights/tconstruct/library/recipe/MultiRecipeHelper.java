package slimeknights.tconstruct.library.recipe;

import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.recipe.MultiNamedRecipe;
import slimeknights.mantle.recipe.NamedRecipe;

import java.util.ArrayList;
import java.util.List;

/** Helpers for implementing {@link MultiNamedRecipe} on top of an unnamed list of display recipes */
public final class MultiRecipeHelper {
  private MultiRecipeHelper() {}

  /**
   * Names a list of display recipes from the parent recipe ID. A single recipe reuses the parent ID, while multiple recipes get an index suffix so each has a unique ID for bookmarks.
   * @param id       Parent recipe ID
   * @param recipes  Display recipes
   * @return  Named recipes
   */
  public static <T> List<NamedRecipe<T>> name(ResourceLocation id, List<? extends T> recipes) {
    int size = recipes.size();
    if (size == 0) {
      return List.of();
    }
    if (size == 1) {
      return List.of(NamedRecipe.of(id, recipes.get(0)));
    }
    List<NamedRecipe<T>> named = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      named.add(NamedRecipe.of(id.withSuffix("/" + i), List.of(id), recipes.get(i)));
    }
    return List.copyOf(named);
  }
}
