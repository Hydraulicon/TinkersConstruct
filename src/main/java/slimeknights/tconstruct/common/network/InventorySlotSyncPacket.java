package slimeknights.tconstruct.common.network;

import slimeknights.tconstruct.TConstruct;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.ISimplePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
// TODO 1.21 import: net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class InventorySlotSyncPacket implements ISimplePacket {
  public static final Type<InventorySlotSyncPacket> TYPE = new Type<>(TConstruct.getResource("inventory_slot_sync"));
  public static final StreamCodec<RegistryFriendlyByteBuf, InventorySlotSyncPacket> CODEC = StreamCodec.ofMember(InventorySlotSyncPacket::encode, InventorySlotSyncPacket::new);


  public final ItemStack itemStack;
  public final int slot;
  public final BlockPos pos;

  public InventorySlotSyncPacket(ItemStack itemStack, int slot, BlockPos pos) {
    this.itemStack = itemStack;
    this.slot = slot;
    this.pos = pos;
  }

  public InventorySlotSyncPacket(RegistryFriendlyByteBuf buffer) {
    this.itemStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
    this.slot = buffer.readShort();
    this.pos = buffer.readBlockPos();
  }

  public void encode(RegistryFriendlyByteBuf packetBuffer) {
    ItemStack.OPTIONAL_STREAM_CODEC.encode(packetBuffer, this.itemStack);
    packetBuffer.writeShort(this.slot);
    packetBuffer.writeBlockPos(this.pos);
  }

  @Override
  public void handle(IPayloadContext context) {
    HandleClient.handle(this);
  }

  /** Safely runs client side only code in a method only called on client */
  private static class HandleClient {
    private static void handle(InventorySlotSyncPacket packet) {
      Level world = Minecraft.getInstance().level;
      if (world != null) {
        BlockEntity te = world.getBlockEntity(packet.pos);
        if (te != null) {
          te.getCapability(ForgeCapabilities.ITEM_HANDLER)
            .filter(cap -> cap instanceof IItemHandlerModifiable)
            .ifPresent(cap -> {
              ((IItemHandlerModifiable)cap).setStackInSlot(packet.slot, packet.itemStack);
              //noinspection ConstantConditions
              Minecraft.getInstance().levelRenderer.blockChanged(null, packet.pos, null, null, 0);
            });
        }
      }
    }
  }

  @Override
  public Type<InventorySlotSyncPacket> type() {
    return TYPE;
  }
}
