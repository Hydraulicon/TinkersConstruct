package slimeknights.tconstruct.library.recipe.casting;

import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
// TODO 1.21 import: slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
// TODO 1.21 import: slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;

import java.util.List;

/** Recipe which duplicates the input cast using a fluid */
public class CastDuplicationRecipe extends ItemCastingRecipe implements IMultiRecipe<IDisplayableCastingRecipe> {
  public static final RecordLoadable<CastDuplicationRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPE.requiredField(), LoadableRecipeSerializer.SERIALIZER.requiredField(),
    LoadableRecipeSerializer.RECIPE_GROUP,
    IngredientLoadable.DISALLOW_EMPTY.requiredField("cast", CastDuplicationRecipe::getCast),
    FLUID_FIELD, COOLING_TIME_FIELD,
    CastDuplicationRecipe::new);

  public CastDuplicationRecipe(RecipeType<?> type, RecipeSerializer<?> serializer, String group, Ingredient cast, SizedFluidIngredient fluid, int coolingTime) {
    super(serializer, group, cast, fluid, ItemOutput.EMPTY, coolingTime, false, false);
  }

  @Override
  public ItemStack assemble(ICastingContainer inv, RegistryAccess access) {
    return inv.getStack().copy();
  }

  @Override
  public ItemStack getResultItem(RegistryAccess access) {
    ItemStack[] items = getCast().getItems();
    return items.length == 0 ? ItemStack.EMPTY : items[0];
  }

  /* JEI */
  private List<IDisplayableCastingRecipe> displayRecipes = null;

  @Override
  public List<IDisplayableCastingRecipe> getRecipes(RegistryAccess access) {
    if (displayRecipes == null) {
      List<ItemStack> casts = List.of(getCast().getItems());
      displayRecipes = List.of(DisplayCastingRecipe.from(this)
        .casts(casts).results(casts).linkCastToOutput()
        .fluids(fluid.getFluids())
        .coolingTime(coolingTime)
        .build());
    }
    return displayRecipes;
  }
}
