package slimeknights.tconstruct.library.recipe.casting.material;

import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.TypedSerializer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import javax.annotation.Nullable;

/** Builder for a composite part recipe, should exist for each part */
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "composite")
public class CompositeCastingRecipeBuilder extends AbstractRecipeBuilder<CompositeCastingRecipeBuilder> {
  private final IMaterialItem result;
  private final int itemCost;
  @Setter @Nullable
  private MaterialStatsId castingStatConflict = null;
  private final TypedSerializer<? extends CompositeCastingRecipe> serializer;
  @Setter
  private IJsonPredicate<MaterialVariantId> allowedMaterials = MaterialPredicate.ANY;

  public static CompositeCastingRecipeBuilder basin(IMaterialItem result, int itemCost) {
    return composite(result, itemCost, TypedSerializer.of(TinkerRecipeTypes.CASTING_BASIN, TinkerSmeltery.basinCompositeSerializer));
  }

  public static CompositeCastingRecipeBuilder table(IMaterialItem result, int itemCost) {
    return composite(result, itemCost, TypedSerializer.of(TinkerRecipeTypes.CASTING_TABLE, TinkerSmeltery.tableCompositeSerializer));
  }

  @Override
  public void save(RecipeOutput consumer) {
    save(consumer, Loadables.ITEM.getKey(result.asItem()));
  }

  @Override
  public void save(RecipeOutput consumer, ResourceLocation id) {
    AdvancementHolder advancementId = this.buildOptionalAdvancement(consumer, id, "casting");
    consumer.accept(id, new CompositeCastingRecipe(serializer.type(), serializer.serializer(), group, itemCost, result, allowedMaterials, castingStatConflict), advancementId);
  }
}
