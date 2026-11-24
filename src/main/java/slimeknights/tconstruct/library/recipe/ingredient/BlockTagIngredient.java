package slimeknights.tconstruct.library.recipe.ingredient;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.shared.TinkerCommons;

import java.util.stream.Stream;

/**
 * Item ingredient matching items with a block form in the given tag.
 * As the ingredient is simple, NeoForge syncs it to the client as the list of matching items.
 * @param tag  Block tag to match
 */
public record BlockTagIngredient(TagKey<Block> tag) implements ICustomIngredient {
  public static final RecordLoadable<BlockTagIngredient> LOADER = RecordLoadable.create(
    Loadables.BLOCK_TAG.requiredField("tag", BlockTagIngredient::tag),
    BlockTagIngredient::new);

  /** Creates a vanilla ingredient for the given tag */
  public static Ingredient of(TagKey<Block> tag) {
    return new BlockTagIngredient(tag).toVanilla();
  }

  @Override
  public boolean test(ItemStack stack) {
    // blocks tags are rarely large, so checking the block holder is cheaper than building the item set
    return !stack.isEmpty() && Block.byItem(stack.getItem()).builtInRegistryHolder().is(tag);
  }

  @Override
  public Stream<ItemStack> getItems() {
    return RegistryHelper.getTagValueStream(BuiltInRegistries.BLOCK, tag)
      .map(Block::asItem)
      .filter(item -> item != Items.AIR)
      .distinct()
      .map(ItemStack::new);
  }

  @Override
  public boolean isSimple() {
    return true;
  }

  @Override
  public IngredientType<?> getType() {
    return TinkerCommons.blockTagIngredient.get();
  }
}
