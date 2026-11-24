package slimeknights.tconstruct.library.recipe.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.tables.TinkerTables;

import java.util.List;

/** Shapeless recipe setting the materials of the result from the first ingredients */
public class ShapelessMaterialsRecipe extends ShapelessRecipe implements MaterialsCraftingTableRecipe {
  public static final MapCodec<ShapelessMaterialsRecipe> CODEC = RecordCodecBuilder.<ShapelessMaterialsRecipe>mapCodec(inst -> inst.group(
    Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
    CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapelessRecipe::category),
    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
    Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(list -> {
      if (list.isEmpty()) {
        return DataResult.error(() -> "No ingredients for shapeless recipe");
      }
      if (list.size() > ShapedRecipePattern.getMaxWidth() * ShapedRecipePattern.getMaxHeight()) {
        return DataResult.error(() -> "Too many ingredients for shapeless recipe. The maximum is: " + ShapedRecipePattern.getMaxWidth() * ShapedRecipePattern.getMaxHeight());
      }
      return DataResult.success(NonNullList.of(Ingredient.EMPTY, list.toArray(Ingredient[]::new)));
    }, DataResult::success).forGetter(ShapelessRecipe::getIngredients),
    Codec.INT.fieldOf("parts").forGetter(r -> r.partCount),
    ShapedMaterialsRecipe.EXTRA_MATERIALS_CODEC.optionalFieldOf("extra_materials", List.of()).forGetter(r -> r.extraMaterials)
  ).apply(inst, ShapelessMaterialsRecipe::new)).validate(recipe -> {
    if (recipe.partCount < 1 || recipe.partCount > recipe.getIngredients().size()) {
      return DataResult.error(() -> "Parts must be between 1 and the number of ingredients " + recipe.getIngredients().size());
    }
    return DataResult.success(recipe);
  });
  public static final StreamCodec<RegistryFriendlyByteBuf,ShapelessMaterialsRecipe> STREAM_CODEC = StreamCodec.composite(
    RecipeSerializer.SHAPELESS_RECIPE.streamCodec(), r -> r,
    ByteBufCodecs.VAR_INT, r -> r.partCount,
    ShapedMaterialsRecipe.EXTRA_MATERIALS_STREAM_CODEC, r -> r.extraMaterials,
    ShapelessMaterialsRecipe::new);

  /** Number of parts to match */
  @Getter
  private final int partCount;
  /** List of additional materials to add beyond the parts */
  @Getter
  private final List<MaterialVariantId> extraMaterials;

  public ShapelessMaterialsRecipe(String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients, int partCount, List<MaterialVariantId> extraMaterials) {
    super(group, category, result, ingredients);
    this.partCount = partCount;
    this.extraMaterials = extraMaterials;
  }

  public ShapelessMaterialsRecipe(ShapelessRecipe recipe, int partCount, List<MaterialVariantId> extraMaterials) {
    this(recipe.getGroup(), recipe.category(), recipe.result, recipe.getIngredients(), partCount, extraMaterials);
  }

  @Override
  public List<Ingredient> getParts() {
    return getIngredients();
  }

  /** Sets the material for the given stack */
  @Override
  public void setMaterial(ItemStack stack, MaterialVariantId material) {
    ShapedMaterialsRecipe.setMaterial(stack, material, extraMaterials);
  }

  @Override
  public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registryAccess) {
    return ShapedMaterialsRecipe.assemble(super.assemble(inventory, registryAccess), inventory, getIngredients(), partCount, false, extraMaterials);
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TinkerTables.shapelessMaterialsRecipeSerializer.get();
  }
}
