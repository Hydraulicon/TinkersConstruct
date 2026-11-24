package slimeknights.tconstruct.library.recipe.casting;

import slimeknights.mantle.recipe.input.SingleItemInput;
import net.minecraft.nbt.CompoundTag;
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
   * Gets the NBT for the contained fluid
   * @return  Fluid's NBT
   */
  @Nullable
  default CompoundTag getFluidTag() {
    return null;
  }
}
