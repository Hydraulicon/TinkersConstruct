package slimeknights.tconstruct.tables.network;

import slimeknights.tconstruct.TConstruct;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.ISimplePacket;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;
import slimeknights.tconstruct.tables.menu.TinkerStationContainerMenu;

/** Packet to send to the server to update the name in the UI */
@RequiredArgsConstructor
public class TinkerStationRenamePacket implements ISimplePacket {
  public static final Type<TinkerStationRenamePacket> TYPE = new Type<>(TConstruct.getResource("tinker_station_rename"));
  public static final StreamCodec<RegistryFriendlyByteBuf, TinkerStationRenamePacket> CODEC = StreamCodec.ofMember(TinkerStationRenamePacket::encode, TinkerStationRenamePacket::new);

  private final String name;

  public TinkerStationRenamePacket(RegistryFriendlyByteBuf buf) {
    this.name = buf.readUtf(Short.MAX_VALUE);
  }

  public void encode(RegistryFriendlyByteBuf buf) {
    buf.writeUtf(name);
  }

  @Override
  public void handle(IPayloadContext context) {
    ServerPlayer sender = ((ServerPlayer) context.player());
    if (sender != null && sender.containerMenu instanceof TinkerStationContainerMenu station) {
      TinkerStationBlockEntity tile = station.getTile();
      if (tile != null) {
        station.getTile().setItemName(name);
      }
    }
  }

  @Override
  public Type<TinkerStationRenamePacket> type() {
    return TYPE;
  }
}
