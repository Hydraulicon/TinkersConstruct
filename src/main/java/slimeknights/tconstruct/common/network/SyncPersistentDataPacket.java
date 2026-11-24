package slimeknights.tconstruct.common.network;

import slimeknights.tconstruct.TConstruct;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.ISimplePacket;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;

/** Packet to sync player persistent data to the client */
@RequiredArgsConstructor
public class SyncPersistentDataPacket implements ISimplePacket {
  public static final Type<SyncPersistentDataPacket> TYPE = new Type<>(TConstruct.getResource("sync_persistent_data"));
  public static final StreamCodec<RegistryFriendlyByteBuf, SyncPersistentDataPacket> CODEC = StreamCodec.ofMember(SyncPersistentDataPacket::encode, SyncPersistentDataPacket::new);

  private final CompoundTag data;

  public SyncPersistentDataPacket(RegistryFriendlyByteBuf buffer) {
    data = buffer.readNbt();
  }

  public void encode(RegistryFriendlyByteBuf buffer) {
    buffer.writeNbt(data);
  }

  @Override
  public void handle(IPayloadContext context) {
    HandleClient.handle(this);
  }

  /** Handles client side only code safely */
  private static class HandleClient {
    private static void handle(SyncPersistentDataPacket packet) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
        PersistentDataCapability.get(player).ifPresent(data -> data.copyFrom(packet.data));
      }
    }
  }

  @Override
  public Type<SyncPersistentDataPacket> type() {
    return TYPE;
  }
}
