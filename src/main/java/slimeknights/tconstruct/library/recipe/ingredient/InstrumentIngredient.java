package slimeknights.tconstruct.library.recipe.ingredient;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.shared.TinkerCommons;

import javax.annotation.Nullable;
import java.util.stream.Stream;

/**
 * Ingredient matching an {@link net.minecraft.world.item.InstrumentItem} with a particular instrument.
 * @param item        Item to match
 * @param instrument  Instrument to match. If null, uses {@code ignore}.
 * @param ignore      Tag of instruments to not match. Used if {@code instrument} is null.
 */
public record InstrumentIngredient(Item item, @Nullable ResourceKey<Instrument> instrument, @Nullable TagKey<Instrument> ignore) implements ICustomIngredient {
  public static final RecordLoadable<InstrumentIngredient> LOADER = RecordLoadable.create(
    Loadables.ITEM.requiredField("item", InstrumentIngredient::item),
    Loadables.resourceKey(Registries.INSTRUMENT).nullableField("instrument", InstrumentIngredient::instrument),
    TinkerLoadables.INSTRUMENT_TAGS.nullableField("ignore", InstrumentIngredient::ignore),
    InstrumentIngredient::new
  ).validate((ingredient, error) -> {
    if (ingredient.instrument == null && ingredient.ignore == null) {
      throw error.create("Invalid InstrumentIngredient: must set either 'instrument' or 'ignore'");
    }
    return ingredient;
  });

  /** Creates a new instance matching the given instrument */
  public static Ingredient of(ItemLike item, ResourceKey<Instrument> instrument) {
    return new InstrumentIngredient(item.asItem(), instrument, null).toVanilla();
  }

  /** Creates a new instance ignoring the given tag */
  public static Ingredient of(ItemLike item, TagKey<Instrument> ignore) {
    return new InstrumentIngredient(item.asItem(), null, ignore).toVanilla();
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public boolean test(ItemStack stack) {
    if (!stack.is(item)) {
      return false;
    }
    Holder<Instrument> holder = stack.get(DataComponents.INSTRUMENT);
    if (holder != null) {
      // if we just want the instrument to match, can just compare by key
      if (this.instrument != null) {
        return holder.is(this.instrument);
      }
      assert this.ignore != null;
      return !holder.is(ignore);
    }
    // if no instrument, its fine as long as we don't have a specific instrument
    return this.instrument == null;
  }

  @Override
  public Stream<ItemStack> getItems() {
    ItemStack stack = new ItemStack(item);
    if (instrument != null) {
      BuiltInRegistries.INSTRUMENT.getHolder(instrument).ifPresent(holder -> stack.set(DataComponents.INSTRUMENT, holder));
    }
    return Stream.of(stack);
  }

  @Override
  public IngredientType<?> getType() {
    return TinkerCommons.instrumentIngredient.get();
  }
}
