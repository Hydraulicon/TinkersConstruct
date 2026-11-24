package slimeknights.tconstruct.library.recipe.casting;

import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.library.recipe.MultiRecipeHelper;
import net.minecraft.core.HolderLookup;
import slimeknights.mantle.recipe.NamedRecipe;
import slimeknights.mantle.recipe.MultiNamedRecipe;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;

import java.util.List;

/** Recipe which duplicates the input cast using a fluid */
public class CastDuplicationRecipe extends ItemCastingRecipe implements MultiNamedRecipe<IDisplayableCastingRecipe> {
  public static final RecordLoadable<CastDuplicationRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPE.requiredField(), LoadableRecipeSerializer.SERIALIZER.requiredField(),
    LoadableRecipeSerializer.RECIPE_GROUP,
    Loadables.ITEM_INGREDIENT_NONEMPTY.requiredField("cast", CastDuplicationRecipe::getCast),
    FLUID_FIELD, COOLING_TIME_FIELD,
    CastDuplicationRecipe::new);

  public CastDuplicationRecipe(RecipeType<?> type, RecipeSerializer<?> serializer, String group, Ingredient cast, SizedFluidIngredient fluid, int coolingTime) {
    super(serializer, group, cast, fluid, ItemOutput.EMPTY, coolingTime, false, false);
  }

  @Override
  public ItemStack assemble(ICastingContainer inv, RegistryAccess access) {
    return inv.getItem().copy();
  }

  @Override
  public ItemStack getResultItem(RegistryAccess access) {
    ItemStack[] items = getCast().getItems();
    return items.length == 0 ? ItemStack.EMPTY : items[0];
  }

  /* JEI */
  private List<IDisplayableCastingRecipe> displayRecipes = null;

  /** Gets the display recipes before naming */
  public List<IDisplayableCastingRecipe> getDisplayRecipes(HolderLookup.Provider access) {
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

  /** Named display recipes, cached from {@link #getDisplayRecipes(HolderLookup.Provider)} */
  private List<NamedRecipe<IDisplayableCastingRecipe>> namedRecipes = null;

  @Override
  public List<NamedRecipe<IDisplayableCastingRecipe>> getRecipes(ResourceLocation id, HolderLookup.Provider access) {
    if (namedRecipes == null) {
      namedRecipes = MultiRecipeHelper.name(id, getDisplayRecipes(access));
    }
    return namedRecipes;
  }
}
