package slimeknights.tconstruct.library.recipe.casting;

import java.util.Optional;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentPatch;
import slimeknights.mantle.recipe.input.SingleItemInput;
import net.minecraft.world.level.material.Fluid;
// TODO 1.21 import: slimeknights.mantle.recipe.container.SingleItemInput;

import javax.annotation.Nullable;

/**
 * Inventory containing a single item and a fluid
 */
public interface ICastingContainer extends SingleItemInput {
  /**
   * Gets the contained fluid in this inventory
   * @return  Contained fluid
   */
  Fluid getFluid();

  /**
   * Gets the components for the contained fluid
   * @return  Fluid's component patch
   */
  default DataComponentPatch getFluidComponents() {
    return DataComponentPatch.EMPTY;
  }

  /**
   * Gets the potion on the contained fluid
   * @return  Potion, or null if the fluid has no potion
   */
  @Nullable
  default Holder<Potion> getFluidPotion() {
    Optional<? extends PotionContents> contents = getFluidComponents().get(DataComponents.POTION_CONTENTS);
    //noinspection OptionalAssignedToNull  patch get returns null when the component is not in the patch
    if (contents != null && contents.isPresent()) {
      return contents.get().potion().orElse(null);
    }
    return null;
  }
}
