package slimeknights.tconstruct.library.recipe.fuel;

import slimeknights.mantle.recipe.input.NoItemInput;
import net.minecraft.world.level.material.Fluid;
// TODO 1.21 import: slimeknights.mantle.recipe.container.NoItemInput;

/**
 * Inventory containing just a single fluid
 */
public interface IFluidContainer extends NoItemInput {
  /**
   * Gets the fluid contained in this inventory
   * @return  Contained fluid
   */
  Fluid getFluid();
}
