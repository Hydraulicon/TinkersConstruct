package slimeknights.tconstruct.library.recipe.ingredient;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.tools.TinkerTools;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Ingredient that only matches tools with a specific hook.
 * As the ingredient is simple, NeoForge syncs it to the client as the list of matching items.
 * @param tag   Tag of items to match
 * @param hook  Hook the tool definition must have
 */
public record ToolHookIngredient(TagKey<Item> tag, ModuleHook<?> hook) implements ICustomIngredient {
  public static final RecordLoadable<ToolHookIngredient> LOADER = RecordLoadable.create(
    Loadables.ITEM_TAG.defaultField("tag", TinkerTags.Items.MODIFIABLE, ToolHookIngredient::tag),
    ToolHooks.LOADER.requiredField("hook", ToolHookIngredient::hook),
    ToolHookIngredient::new);

  public static Ingredient of(TagKey<Item> tag, ModuleHook<?> hook) {
    return new ToolHookIngredient(tag, hook).toVanilla();
  }

  public static Ingredient of(ModuleHook<?> hook) {
    return of(TinkerTags.Items.MODIFIABLE, hook);
  }

  /** Checks if the given item has the hook */
  private boolean hasHook(Item item) {
    return item instanceof IModifiable modifiable && modifiable.getToolDefinition().getData().getHooks().hasHook(hook);
  }

  @Override
  public boolean test(ItemStack stack) {
    return stack.is(tag) && hasHook(stack.getItem());
  }

  @SuppressWarnings("deprecation")
  @Override
  public Stream<ItemStack> getItems() {
    List<ItemStack> list = new ArrayList<>();
    // filtered version of tag values
    for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
      if (hasHook(holder.value())) {
        list.add(new ItemStack(holder));
      }
    }
    if (list.isEmpty()) {
      ItemStack barrier = new ItemStack(Blocks.BARRIER);
      barrier.set(DataComponents.CUSTOM_NAME, Component.literal("Empty Tag: " + tag.location()));
      list.add(barrier);
    }
    return list.stream();
  }

  @Override
  public boolean isSimple() {
    return true;
  }

  @Override
  public IngredientType<?> getType() {
    return TinkerTools.toolHookIngredient.get();
  }
}
