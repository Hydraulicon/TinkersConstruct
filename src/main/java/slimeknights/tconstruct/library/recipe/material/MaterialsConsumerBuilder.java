package slimeknights.tconstruct.library.recipe.material;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Recipe output wrapper converting vanilla shaped and shapeless recipes into {@link ShapedMaterialsRecipe} and {@link ShapelessMaterialsRecipe}.
 * Meant to wrap the output passed to the vanilla recipe builders.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MaterialsConsumerBuilder {
  private final String parts;
  private final int partCount;
  private final List<MaterialVariantId> materials = new ArrayList<>();

  /** Creates a new shaped recipe with the given ingredients as parts */
  public static MaterialsConsumerBuilder shaped(String parts) {
    if (parts.isEmpty()) {
      throw new IllegalArgumentException("Parts may not be empty");
    }
    return new MaterialsConsumerBuilder(parts, 0);
  }

  /** Creates a new shapeless recipe with the first ingredients as parts */
  public static MaterialsConsumerBuilder shapeless(int parts) {
    if (parts <= 0) {
      throw new IllegalArgumentException("Parts must be greater than 0");
    }
    return new MaterialsConsumerBuilder("", parts);
  }

  /** Adds a material to the builder */
  public MaterialsConsumerBuilder material(MaterialVariantId material) {
    materials.add(material);
    return this;
  }

  /** Converts the vanilla recipe into the materials recipe */
  private Recipe<?> convert(ResourceLocation id, Recipe<?> recipe) {
    List<MaterialVariantId> materials = List.copyOf(this.materials);
    if (partCount > 0) {
      if (recipe instanceof ShapelessRecipe shapeless) {
        return new ShapelessMaterialsRecipe(shapeless, partCount, materials);
      }
      throw new IllegalArgumentException("Shapeless materials recipe " + id + " must wrap a shapeless recipe, got " + recipe.getClass());
    }
    if (recipe instanceof ShapedRecipe shaped) {
      ShapedRecipePattern.Data data = shaped.pattern.data.orElseThrow(() -> new IllegalArgumentException("Shaped materials recipe " + id + " is missing its pattern key"));
      List<Ingredient> partList = new ArrayList<>(parts.length());
      for (int i = 0; i < parts.length(); i++) {
        char sym = parts.charAt(i);
        Ingredient ingredient = data.key().get(sym);
        if (ingredient == null) {
          throw new IllegalArgumentException("Parts references symbol '" + sym + "' but it's not defined in the key for " + id);
        }
        partList.add(ingredient);
      }
      return new ShapedMaterialsRecipe(shaped.getGroup(), shaped.category(), shaped.pattern, shaped.result, shaped.showNotification(), List.copyOf(partList), materials);
    }
    throw new IllegalArgumentException("Shaped materials recipe " + id + " must wrap a shaped recipe, got " + recipe.getClass());
  }

  /** Builds the wrapped consumer */
  public RecipeOutput build(RecipeOutput output) {
    return new RecipeOutput() {
      @Override
      public Advancement.Builder advancement() {
        return output.advancement();
      }

      @Override
      public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
        output.accept(id, convert(id, recipe), advancement, conditions);
      }
    };
  }
}
