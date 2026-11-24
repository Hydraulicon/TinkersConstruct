package slimeknights.tconstruct.library.recipe;

import slimeknights.mantle.recipe.input.SingleItemInput;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.item.ItemStack;
// TODO 1.21 import: slimeknights.mantle.recipe.container.SingleItemInput;

/** Simple class for an inventory containing just one item */
public class SingleItemContainer implements SingleItemInput {
  @Getter @Setter
  private ItemStack stack = ItemStack.EMPTY;
}
