package slimeknights.tconstruct.library.recipe.ingredient;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicateField;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.TinkerMaterials;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Ingredient to display materials on items and support matching by materials.
 * @param match     Nested ingredient to match
 * @param material  Material filter
 */
public record MaterialIngredient(Ingredient match, IJsonPredicate<MaterialVariantId> material) implements ICustomIngredient {
  public static final RecordLoadable<MaterialIngredient> LOADER = RecordLoadable.create(
    Loadables.ITEM_INGREDIENT_NONEMPTY.requiredField("match", MaterialIngredient::match),
    new MaterialPredicateField<>("material", MaterialIngredient::material),
    MaterialIngredient::new);

  /** Creates an ingredient matching the given materials */
  public static Ingredient of(Ingredient ingredient, IJsonPredicate<MaterialVariantId> material) {
    return new MaterialIngredient(ingredient, material).toVanilla();
  }

  /** Creates an ingredient matching the given materials */
  public static Ingredient of(ItemLike item, IJsonPredicate<MaterialVariantId> material) {
    return of(Ingredient.of(item), material);
  }

  /** Creates an ingredient matching any material */
  public static Ingredient of(Ingredient ingredient) {
    return of(ingredient, MaterialPredicate.ANY);
  }

  /** Creates an ingredient matching a single material */
  public static Ingredient of(Ingredient ingredient, MaterialVariantId material) {
    return of(ingredient, MaterialPredicate.variant(material));
  }

  /** Creates an ingredient matching a material tag */
  public static Ingredient of(Ingredient ingredient, TagKey<IMaterial> tag) {
    return of(ingredient, MaterialPredicate.tag(tag));
  }

  /** Creates a new instance from an item with a fixed material */
  public static Ingredient of(ItemLike item, MaterialVariantId material) {
    return of(Ingredient.of(item), material);
  }

  /** Creates a new instance from an item with a tagged material */
  public static Ingredient of(ItemLike item, TagKey<IMaterial> tag) {
    return of(Ingredient.of(item), tag);
  }

  /** Creates a new ingredient matching any material from items */
  public static Ingredient of(ItemLike item) {
    return of(Ingredient.of(item));
  }

  /** Creates a new ingredient from a tag with the given material */
  public static Ingredient of(TagKey<Item> tag, MaterialVariantId material) {
    return of(Ingredient.of(tag), material);
  }

  /** Creates a new ingredient matching any material from a tag */
  public static Ingredient of(TagKey<Item> tag) {
    return of(Ingredient.of(tag));
  }

  @Override
  public boolean test(ItemStack stack) {
    // check nested first, should be faster
    if (stack.isEmpty() || !match.test(stack)) {
      return false;
    }
    // no need to read the material if the material is the any predicate
    if (material != MaterialPredicate.ANY) {
      return material.matches(IMaterialItem.getMaterialFromStack(stack));
    }
    return true;
  }

  @Override
  public Stream<ItemStack> getItems() {
    Stream<ItemStack> items = Arrays.stream(match.getItems());
    // TODO 1.21: vanilla caches ingredient items, so if this is called before materials load the display will lack materials
    if (!MaterialRegistry.isFullyLoaded()) {
      return items;
    }
    // find all materials matching the filter; note this only shows craftable material variants
    return items.flatMap(stack -> MaterialRecipeCache.getAllVariants().stream()
      .filter(material::matches)
      .map(mat -> IMaterialItem.withMaterial(stack, mat))
      .filter(result -> result.has(TinkerCommons.material)))
      .distinct();
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return TinkerMaterials.materialIngredient.get();
  }
}
