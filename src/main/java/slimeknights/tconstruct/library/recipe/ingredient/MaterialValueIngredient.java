package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicateField;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.shared.TinkerMaterials;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Ingredient matching material items with the given value. Typically, matches ingots or blocks
 * @param material  Material filter
 * @param minValue  Minimum material value, inclusive
 * @param maxValue  Maximum material value, inclusive
 */
public record MaterialValueIngredient(IJsonPredicate<MaterialVariantId> material, float minValue, float maxValue) implements ICustomIngredient {
  public static final RecordLoadable<MaterialValueIngredient> LOADER = RecordLoadable.create(
    new MaterialPredicateField<>("material", MaterialValueIngredient::material),
    ValueField.INSTANCE,
    (material, range) -> new MaterialValueIngredient(material, range[0], range[1]));

  /** Creates an ingredient matching a range of values */
  public static MaterialValueIngredient of(IJsonPredicate<MaterialVariantId> materials, float minValue, float maxValue) {
    return new MaterialValueIngredient(materials, minValue, maxValue);
  }

  /** Creates an ingredient matching an exact value */
  public static MaterialValueIngredient of(IJsonPredicate<MaterialVariantId> materials, float value) {
    return of(materials, value, value);
  }

  /** Gets this ingredient as a vanilla ingredient */
  public static Ingredient ingredient(IJsonPredicate<MaterialVariantId> materials, float value) {
    return of(materials, value).toVanilla();
  }

  /** Checks the given material recipe against our filters */
  public boolean test(MaterialRecipe material) {
    float value = material.getValue() / (float) material.getNeeded();
    return minValue <= value && value <= maxValue && this.material.matches(material.getMaterial().getVariant());
  }

  @Override
  public boolean test(ItemStack stack) {
    if (stack.isEmpty()) {
      return false;
    }
    MaterialRecipe recipe = MaterialRecipeCache.findRecipe(stack);
    return recipe != MaterialRecipe.EMPTY && test(recipe);
  }

  @Override
  public Stream<ItemStack> getItems() {
    return MaterialRecipeCache.getSortedRecipes().stream()
      .filter(this::test)
      .flatMap(material -> Arrays.stream(material.getIngredient().getItems()));
  }

  @Override
  public boolean isSimple() {
    // depends on the material recipe cache, which syncs to the client
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return TinkerMaterials.materialValueIngredient.get();
  }


  /* Helpers for ShapedMaterialRecipe */

  /** Checks if this ingredient fully contains the range of the other */
  private boolean contains(MaterialValueIngredient other) {
    return this.minValue <= other.minValue && other.maxValue <= this.maxValue;
  }

  /** Creates an ingredient that matches anything either of the two ingredients matches */
  public MaterialValueIngredient merge(MaterialValueIngredient other) {
    if (this == other) return this;
    // if we have the same predicate, we can possibly skip creating a new instance
    IJsonPredicate<MaterialVariantId> predicate = this.material;
    if (this.material.equals(other.material)) {
      if (this.contains(other)) {
        return this;
      }
      if (other.contains(this)) {
        return other;
      }
    } else {
      predicate = MaterialPredicate.or(this.material, other.material);
    }
    return new MaterialValueIngredient(predicate, Math.min(this.minValue, other.minValue), Math.max(this.maxValue, other.maxValue));
  }

  /** Gets the material matching this recipe */
  @Nullable
  public MaterialVariantId getMaterial(ItemStack stack) {
    MaterialRecipe recipe = MaterialRecipeCache.findRecipe(stack);
    return recipe != MaterialRecipe.EMPTY && test(recipe) ? recipe.getMaterial().getVariant() : null;
  }

  /** Field for the value range, either a single number or an object with min and max */
  private enum ValueField implements LoadableField<float[],MaterialValueIngredient> {
    INSTANCE;

    @Override
    public String key() {
      return "value";
    }

    @Override
    public float[] get(JsonObject json, String key, TypedMap context) {
      JsonElement value = GsonHelper.getNonNull(json, key);
      if (value.isJsonPrimitive()) {
        float exact = GsonHelper.convertToFloat(value, key);
        return new float[] { exact, exact };
      }
      JsonObject object = GsonHelper.convertToJsonObject(value, key);
      return new float[] {
        GsonHelper.getAsFloat(object, "min", 0),
        GsonHelper.getAsFloat(object, "max", Float.POSITIVE_INFINITY)
      };
    }

    @Override
    public void serializeInto(MaterialValueIngredient parent, JsonObject json, TypedMap context) {
      if (parent.minValue == parent.maxValue) {
        json.addProperty(key(), parent.minValue);
      } else {
        JsonObject value = new JsonObject();
        if (parent.minValue > 0) {
          value.addProperty("min", parent.minValue);
        }
        if (Float.isFinite(parent.maxValue)) {
          value.addProperty("max", parent.maxValue);
        }
        json.add(key(), value);
      }
    }

    @Override
    public float[] decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
      return new float[] { buffer.readFloat(), buffer.readFloat() };
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, MaterialValueIngredient parent, TypedMap context) {
      buffer.writeFloat(parent.minValue);
      buffer.writeFloat(parent.maxValue);
    }
  }
}
