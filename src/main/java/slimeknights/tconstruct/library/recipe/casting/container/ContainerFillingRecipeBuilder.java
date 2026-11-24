package slimeknights.tconstruct.library.recipe.casting.container;

import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.TypedSerializer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import javax.annotation.Nullable;

/**
 * Builder for a container filling recipe. Takes an arbitrary fluid for a specific amount to fill a Forge {@link net.neoforged.neoforge.fluids.capability.IFluidHandlerItem}
 */
@AllArgsConstructor(staticName = "castingRecipe")
@SuppressWarnings({"WeakerAccess", "unused"})
public class ContainerFillingRecipeBuilder extends AbstractRecipeBuilder<ContainerFillingRecipeBuilder> {
  private final ResourceLocation result;
  private final int fluidAmount;
  private final TypedSerializer<? extends ContainerFillingRecipe> recipeSerializer;

  /**
   * Creates a new builder instance using the given result, amount, and serializer
   * @param result            Recipe result
   * @param fluidAmount       Container size
   * @param recipeSerializer  Serializer
   * @return  Builder instance
   */
  public static ContainerFillingRecipeBuilder castingRecipe(ItemLike result, int fluidAmount, TypedSerializer<? extends ContainerFillingRecipe> recipeSerializer) {
    return new ContainerFillingRecipeBuilder(Loadables.ITEM.getKey(result.asItem()), fluidAmount, recipeSerializer);
  }

  /**
   * Creates a new basin recipe builder using the given result, amount, and serializer
   * @param result            Recipe result
   * @param fluidAmount       Container size
   * @return  Builder instance
   */
  public static ContainerFillingRecipeBuilder basinRecipe(ResourceLocation result, int fluidAmount) {
    return castingRecipe(result, fluidAmount, TypedSerializer.of(TinkerRecipeTypes.CASTING_BASIN, TinkerSmeltery.basinFillingRecipeSerializer));
  }

  /**
   * Creates a new basin recipe builder using the given result, amount, and serializer
   * @param result            Recipe result
   * @param fluidAmount       Container size
   * @return  Builder instance
   */
  public static ContainerFillingRecipeBuilder basinRecipe(ItemLike result, int fluidAmount) {
    return castingRecipe(result, fluidAmount, TypedSerializer.of(TinkerRecipeTypes.CASTING_BASIN, TinkerSmeltery.basinFillingRecipeSerializer));
  }

  /**
   * Creates a new table recipe builder using the given result, amount, and serializer
   * @param result            Recipe result
   * @param fluidAmount       Container size
   * @return  Builder instance
   */
  public static ContainerFillingRecipeBuilder tableRecipe(ResourceLocation result, int fluidAmount) {
    return castingRecipe(result, fluidAmount, TypedSerializer.of(TinkerRecipeTypes.CASTING_TABLE, TinkerSmeltery.tableFillingRecipeSerializer));
  }

  /**
   * Creates a new table recipe builder using the given result, amount, and serializer
   * @param result            Recipe result
   * @param fluidAmount       Container size
   * @return  Builder instance
   */
  public static ContainerFillingRecipeBuilder tableRecipe(ItemLike result, int fluidAmount) {
    return castingRecipe(result, fluidAmount, TypedSerializer.of(TinkerRecipeTypes.CASTING_TABLE, TinkerSmeltery.tableFillingRecipeSerializer));
  }

  @Override
  public void save(RecipeOutput consumer) {
    this.save(consumer, this.result);
  }

  @Override
  public void save(RecipeOutput consumerIn, ResourceLocation id) {
    AdvancementHolder advancementId = this.buildOptionalAdvancement(consumerIn, id, "casting");
    consumerIn.accept(new ContainerFillingRecipeBuilder.Result(id, advancementId));
  }

  private class Result extends AbstractFinishedRecipe {
    public Result(ResourceLocation ID, @Nullable ResourceLocation advancementID) {
      super(ID, advancementID);
    }

    @Override
    public RecipeSerializer<?> getType() {
      return recipeSerializer;
    }

    @Override
    public void serializeRecipeData(JsonObject json) {
      if (!group.isEmpty()) {
        json.addProperty("group", group);
      }
      json.addProperty("fluid_amount", fluidAmount);
      // TODO: consider another way to spoof this for datagen?
      json.addProperty("container", result.toString());
    }
  }
}
