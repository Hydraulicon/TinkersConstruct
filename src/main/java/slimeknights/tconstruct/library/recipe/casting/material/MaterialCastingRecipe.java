package slimeknights.tconstruct.library.recipe.casting.material;

import slimeknights.tconstruct.library.recipe.MultiRecipeHelper;
import net.minecraft.core.HolderLookup;
import slimeknights.mantle.recipe.NamedRecipe;
import slimeknights.mantle.recipe.MultiNamedRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.casting.CastingRecipeLookup;
import slimeknights.tconstruct.library.recipe.casting.DisplayCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Casting recipe that takes an arbitrary fluid of a given amount and set the material on the output based on that fluid
 */
public class MaterialCastingRecipe extends AbstractMaterialCastingRecipe implements MultiNamedRecipe<IDisplayableCastingRecipe> {
  protected static final LoadableField<IMaterialItem,MaterialCastingRecipe> RESULT_FIELD = TinkerLoadables.MATERIAL_ITEM.requiredField("result", r -> r.result);
  public static final RecordLoadable<MaterialCastingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPE.requiredField(), LoadableRecipeSerializer.SERIALIZER.requiredField(),
    LoadableRecipeSerializer.RECIPE_GROUP, CAST_FIELD,
    ITEM_COST_FIELD, RESULT_FIELD, MATERIALS_FIELD, CAST_CONSUMED_FIELD, SWITCH_SLOTS_FIELD,
    MaterialCastingRecipe::new);

  protected final IMaterialItem result;

  public MaterialCastingRecipe(RecipeType<?> type, RecipeSerializer<?> serializer, String group, Ingredient cast, int itemCost, IMaterialItem result, IJsonPredicate<MaterialVariantId> materials, boolean consumed, boolean switchSlots) {
    super(serializer, group, cast, itemCost, consumed, switchSlots, materials);
    this.result = result;
    CastingRecipeLookup.registerCastable(result);
    MaterialCastingLookup.registerItemCost(result, itemCost);
  }


  @Override
  public boolean matches(ICastingContainer inv, Level worldIn) {
    if (!this.getCast().test(inv.getItem())) {
      return false;
    }
    MaterialFluidRecipe fluid = getFluidRecipe(inv);
    return fluid != MaterialFluidRecipe.EMPTY && result.canUseMaterial(fluid.getOutput().getId());
  }

  @Override
  public ItemStack getResultItem(RegistryAccess access) {
    return new ItemStack(result);
  }

  @Override
  public ItemStack assemble(ICastingContainer inv, RegistryAccess access) {
    return result.withMaterial(getFluidRecipe(inv).getOutput().getVariant());
  }


  /* JEI */
  protected List<IDisplayableCastingRecipe> multiRecipes;

  /** Gets the display recipes before naming */
  public List<IDisplayableCastingRecipe> getDisplayRecipes(HolderLookup.Provider access) {
    if (multiRecipes == null) {
      List<MaterialFluidRecipe> recipes = MaterialCastingLookup.getSortedCastingFluids();
      List<FluidStack> fluids = new ArrayList<>(recipes.size());
      List<ItemStack> results = new ArrayList<>(recipes.size());
      int maxTime = 0;
      for (MaterialFluidRecipe recipe : recipes) {
        // must support this material
        MaterialVariant output = recipe.getOutput();
        MaterialVariantId outputId = output.getVariant();
        if (!result.canUseMaterial(output.getId()) || !this.materials.matches(outputId)) {
          continue;
        }

        // add all fluids to our builders
        List<FluidStack> newFluids = resizeFluids(recipe.getFluids());
        fluids.addAll(newFluids);
        ItemStack result = this.result.withMaterial(outputId);
        for (FluidStack fluid : newFluids) {
          // add one copy of result per fluid
          results.add(result);
          // use the maximum time for cooling time. Will be recomputed dynamically but need a fallback
          int time = ICastingRecipe.calcCoolingTime(recipe.getTemperature(), fluid.getAmount());
          if (time > maxTime) {
            maxTime = time;
          }
        }
      }
      if (fluids.isEmpty()) {
        multiRecipes = List.of();
      } else {
        multiRecipes = List.of(DisplayCastingRecipe.from(this)
          .cast(getCast()).consumed(isConsumed())
          .fluids(List.copyOf(fluids))
          .results(List.copyOf(results))
          .coolingTime(maxTime).materialCasting(false)
          .build());
      }
    }
    return multiRecipes;
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
