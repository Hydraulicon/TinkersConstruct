package slimeknights.tconstruct.smeltery.network;

import slimeknights.tconstruct.TConstruct;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.ISimplePacket;
import lombok.AllArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import slimeknights.mantle.inventory.BaseContainerMenu;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;

/**
 * Packet sent when a fluid is clicked in the smeltery UI
 */
@AllArgsConstructor
public class SmelteryFluidClickedPacket implements ISimplePacket {
  public static final Type<SmelteryFluidClickedPacket> TYPE = new Type<>(TConstruct.getResource("smeltery_fluid_clicked"));
  public static final StreamCodec<RegistryFriendlyByteBuf, SmelteryFluidClickedPacket> CODEC = StreamCodec.ofMember(SmelteryFluidClickedPacket::encode, SmelteryFluidClickedPacket::new);

  private final int index;

  public SmelteryFluidClickedPacket(RegistryFriendlyByteBuf buffer) {
    index = buffer.readVarInt();
  }

  public void encode(RegistryFriendlyByteBuf buffer) {
    buffer.writeVarInt(index);
  }

  @Override
  public void handle(IPayloadContext context) {
    ServerPlayer sender = ((ServerPlayer) context.player());
    if (sender != null && !sender.isSpectator()) {
      AbstractContainerMenu container = sender.containerMenu;
      if (container instanceof BaseContainerMenu<?> base && base.getTile() instanceof ISmelteryTankHandler tank) {
        tank.getTank().moveFluidToBottom(index);
      }
    }
  }

  @Override
  public Type<SmelteryFluidClickedPacket> type() {
    return TYPE;
  }
}
