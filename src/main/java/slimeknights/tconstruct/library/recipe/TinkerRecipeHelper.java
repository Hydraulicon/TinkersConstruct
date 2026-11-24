package slimeknights.tconstruct.library.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.Optional;

/** Recipe manager helpers, replacing those removed from Mantle's RecipeHelper in 1.21 */
public final class TinkerRecipeHelper {
  private TinkerRecipeHelper() {}

  /**
   * Gets all recipes of the given type that are instances of the given class
   * @param manager  Recipe manager
   * @param type     Recipe type
   * @param clazz    Recipe class to filter
   * @return  List of matching recipes
   */
  public static <I extends RecipeInput, R extends Recipe<I>, C> List<C> getRecipes(RecipeManager manager, RecipeType<R> type, Class<C> clazz) {
    return manager.getAllRecipesFor(type).stream()
      .map(RecipeHolder::value)
      .filter(clazz::isInstance)
      .map(clazz::cast)
      .toList();
  }

  /**
   * Gets a recipe by ID if it is an instance of the given class
   * @param manager  Recipe manager
   * @param id       Recipe ID
   * @param clazz    Recipe class
   * @return  Recipe, or empty if missing or the wrong class
   */
  public static <C> Optional<C> getRecipe(RecipeManager manager, ResourceLocation id, Class<C> clazz) {
    return manager.byKey(id).map(RecipeHolder::value).filter(clazz::isInstance).map(clazz::cast);
  }
}
