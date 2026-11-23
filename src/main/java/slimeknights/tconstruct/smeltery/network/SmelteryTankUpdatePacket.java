package slimeknights.tconstruct.smeltery.network;

import slimeknights.tconstruct.TConstruct;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.network.BlockEntityPacket;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent whenever the contents of the smeltery tank change.
 * TODO 1.21: make record
 */
@AllArgsConstructor
public class SmelteryTankUpdatePacket implements BlockEntityPacket<ISmelteryTankHandler> {
  public static final Type<SmelteryTankUpdatePacket> TYPE = new Type<>(TConstruct.getResource("smeltery_tank_update"));
  public static final StreamCodec<RegistryFriendlyByteBuf, SmelteryTankUpdatePacket> CODEC = StreamCodec.ofMember(SmelteryTankUpdatePacket::encode, SmelteryTankUpdatePacket::new);

  private final BlockPos pos;
  private final List<FluidStack> fluids;

  public SmelteryTankUpdatePacket(RegistryFriendlyByteBuf buffer) {
    pos = buffer.readBlockPos();
    int size = buffer.readVarInt();
    fluids = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      fluids.add(FluidStack.OPTIONAL_STREAM_CODEC.decode(buffer));
    }
  }

  public void encode(RegistryFriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    buffer.writeVarInt(fluids.size());
    for (FluidStack fluid : fluids) {
      FluidStack.OPTIONAL_STREAM_CODEC.encode(buffer, fluid);
    }
  }

  @Override
  public BlockPos pos() {
    return pos;
  }

  @Override
  public Class<ISmelteryTankHandler> blockEntityType() {
    return ISmelteryTankHandler.class;
  }

  @Override
  public void handleBlockEntity(IPayloadContext context, ISmelteryTankHandler be) {
    be.updateFluidsFromPacket(fluids);
  }

  @Override
  public Type<SmelteryTankUpdatePacket> type() {
    return TYPE;
  }
}
