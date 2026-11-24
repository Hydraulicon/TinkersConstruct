package slimeknights.tconstruct.library.recipe.casting;

import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.Holder;
import slimeknights.tconstruct.library.recipe.ingredient.FluidIngredients;
import slimeknights.tconstruct.library.recipe.MultiRecipeHelper;
import net.minecraft.core.HolderLookup;
import slimeknights.mantle.recipe.NamedRecipe;
import slimeknights.mantle.recipe.MultiNamedRecipe;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe for casting a fluid onto an item, copying the fluid NBT to the item.
 * TODO 1.21: move to {@link slimeknights.tconstruct.library.recipe.casting.potion}
 */
public class PotionCastingRecipe implements ICastingRecipe, MultiNamedRecipe<IDisplayableCastingRecipe> {
  protected static final LoadableField<SizedFluidIngredient, PotionCastingRecipe> FLUID_FIELD = Loadables.SIZED_FLUID_INGREDIENT.requiredField("fluid", r -> r.fluid);
  protected static final LoadableField<Integer, PotionCastingRecipe> COOLING_TIME_FIELD = IntLoadable.FROM_ONE.defaultField("cooling_time", 5, r -> r.coolingTime);
  public static final RecordLoadable<PotionCastingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPE.requiredField(), LoadableRecipeSerializer.SERIALIZER.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP,
    Loadables.ITEM_INGREDIENT_NONEMPTY.requiredField("bottle", r -> r.bottle),
    FLUID_FIELD,
    Loadables.ITEM.requiredField("result", r -> r.result),
    COOLING_TIME_FIELD,
    PotionCastingRecipe::new);

  protected final RecipeType<?> type;
  @Getter
  protected final RecipeSerializer<?> serializer;
  @Getter
  protected final String group;
  /** Input on the casting table, always consumed */
  protected final Ingredient bottle;
  /** Potion ingredient, typically just the potion tag */
  protected final SizedFluidIngredient fluid;
  /** Potion item result, will be given the proper NBT */
  protected final Item result;
  /** Cooling time for this recipe, used for tipped arrows */
  protected final int coolingTime;

  public PotionCastingRecipe(RecipeType<?> type, RecipeSerializer<?> serializer, String group, Ingredient bottle, SizedFluidIngredient fluid, Item result, int coolingTime) {
    this.type = type;
    this.serializer = serializer;
    this.group = group;
    this.bottle = bottle;
    this.fluid = fluid;
    this.result = result;
    this.coolingTime = coolingTime;
    CastingRecipeLookup.registerCastable(result);
  }

  @Override
  public RecipeType<?> getType() {
    return type;
  }

  @Override
  public boolean matches(ICastingContainer inv, Level level) {
    return bottle.test(inv.getItem()) && FluidIngredients.test(fluid, inv.getFluid());
  }

  @Override
  public int getFluidAmount(ICastingContainer inv) {
    return FluidIngredients.getAmount(fluid, inv.getFluid());
  }

  @Override
  public boolean isConsumed() {
    return true;
  }

  @Override
  public boolean switchSlots() {
    return false;
  }

  @Override
  public int getCoolingTime(ICastingContainer inv) {
    return coolingTime;
  }

  @Override
  public ItemStack assemble(ICastingContainer inv, HolderLookup.Provider access) {
    ItemStack result = new ItemStack(this.result);
    result.applyComponents(inv.getFluidComponents());
    return result;
  }


  /* JEI */
  // TODO 1.21: consider making this a display recipe instead of a multirecipe
  protected List<IDisplayableCastingRecipe> displayRecipes = null;

  /** Gets the display recipes before naming */
  public List<IDisplayableCastingRecipe> getDisplayRecipes(ResourceLocation id, HolderLookup.Provider access) {
    if (displayRecipes == null) {
      List<Holder.Reference<Potion>> potions = BuiltInRegistries.POTION.holders().toList();
      List<ItemStack> results = new ArrayList<>(potions.size());
      // first, make all the potion items
      for (Holder<Potion> potion : potions) {
        results.add(PotionContents.createItemStack(this.result, potion));
      }
      // next, it's time to do the fluids
      // we want an order of Mod 1 Potion 1, Mod 1 Potion 2, ..., Mod 2 Potion 1, ...
      // this is potentially displaying multiple fluids from the fluid tag
      List<FluidStack> potionFluids = FluidIngredients.getFluids(this.fluid);
      int totalSize = results.size() * potionFluids.size();
      List<ItemStack> displayResults = new ArrayList<>(totalSize);
      List<FluidStack> displayFluids = new ArrayList<>(totalSize);
      for (FluidStack fluid : potionFluids) {
        displayResults.addAll(results);
        for (ItemStack result : results) {
          displayFluids.add(new FluidStack(fluid.getFluidHolder(), fluid.getAmount(), result.getComponentsPatch()));
        }
      }
      this.displayRecipes = List.of(DisplayCastingRecipe.from(id, this)
        .cast(bottle).consumed()
        .fluids(List.copyOf(displayFluids))
        .results(List.copyOf(displayResults)).linkFluidsToOutput()
        .coolingTime(coolingTime)
        .build());
    }
    return displayRecipes;
  }


  /* Recipe interface methods */

  @Override
  public NonNullList<Ingredient> getIngredients() {
    return NonNullList.of(Ingredient.EMPTY, bottle);
  }

  /** @deprecated use {@link #assemble(ICastingContainer, HolderLookup.Provider)} */
  @Deprecated
  @Override
  public ItemStack getResultItem(HolderLookup.Provider access) {
    return new ItemStack(this.result);
  }


  /* JEI helpers */

  /** Gets a list of all potion IDs ready for usage in building tool data */
  public static List<String> getPotionIds() {
    return BuiltInRegistries.POTION.holders()
      .map(holder -> holder.key().location().toString())
      .toList();
  }

  /** Named display recipes, cached from {@link #getDisplayRecipes(ResourceLocation, HolderLookup.Provider)} */
  private List<NamedRecipe<IDisplayableCastingRecipe>> namedRecipes = null;

  @Override
  public List<NamedRecipe<IDisplayableCastingRecipe>> getRecipes(ResourceLocation id, HolderLookup.Provider access) {
    if (namedRecipes == null) {
      namedRecipes = MultiRecipeHelper.name(id, getDisplayRecipes(id, access));
    }
    return namedRecipes;
  }
}
