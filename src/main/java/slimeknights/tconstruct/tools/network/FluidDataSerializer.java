package slimeknights.tconstruct.tools.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.neoforge.fluids.FluidStack;

/** Serializer for fluid stack data in entities */
public class FluidDataSerializer implements EntityDataSerializer<FluidStack> {
  @Override
  public void write(RegistryFriendlyByteBuf buffer, FluidStack stack) {
    FluidStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
  }

  @Override
  public FluidStack read(RegistryFriendlyByteBuf buffer) {
    return FluidStack.OPTIONAL_STREAM_CODEC.decode(buffer);
  }

  @Override
  public FluidStack copy(FluidStack stack) {
    return stack.copy();
  }
}
