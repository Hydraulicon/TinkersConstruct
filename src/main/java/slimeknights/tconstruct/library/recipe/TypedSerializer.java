package slimeknights.tconstruct.library.recipe;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.function.Supplier;

/**
 * Pair of recipe type and serializer, for recipes whose serializer determines their type.
 * Replaces Mantle's {@code TypeAwareRecipeSerializer}, which was removed in 1.21 as types are now provided through the loadable context.
 * Mainly used by datagen builders, which need to construct the recipe.
 * @param type        Recipe type
 * @param serializer  Recipe serializer
 */
public record TypedSerializer<T extends Recipe<?>>(RecipeType<?> type, RecipeSerializer<T> serializer) {
  /** Creates an instance from registry suppliers */
  public static <T extends Recipe<?>> TypedSerializer<T> of(Supplier<? extends RecipeType<?>> type, Supplier<? extends RecipeSerializer<T>> serializer) {
    return new TypedSerializer<>(type.get(), serializer.get());
  }
}
