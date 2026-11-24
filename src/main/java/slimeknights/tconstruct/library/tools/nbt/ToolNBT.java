package slimeknights.tconstruct.library.tools.nbt;

import java.util.function.Consumer;
import slimeknights.tconstruct.shared.TinkerCommons;
import net.minecraft.world.item.ItemStack;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import com.mojang.serialization.Codec;

/**
 * Data component holding the full tool NBT, read and written by {@link ToolStack}.
 * <p>
 * This is the first step of the 1.21 port described in {@code MIGRATION_1.21.md}: the 1.20 stack NBT is kept as a single compound
 * so the {@link ToolStack} API is unchanged. A later step may split it into typed components.
 * <p>
 * Data components must be immutable. {@link ToolStack} never modifies a tag once it is stored in this record,
 * it copies the tag before its first modification. Code outside of {@link ToolStack} should treat {@link #tag()} as read only.
 * @param tag  Tool NBT, must not be modified
 */
public record ToolNBT(CompoundTag tag) {
  /** Empty tool data */
  public static final ToolNBT EMPTY = new ToolNBT(new CompoundTag());
  /** Codec for saving the component */
  public static final Codec<ToolNBT> CODEC = CompoundTag.CODEC.xmap(ToolNBT::new, ToolNBT::tag);
  /** Codec for syncing the component */
  public static final StreamCodec<ByteBuf,ToolNBT> STREAM_CODEC = ByteBufCodecs.TRUSTED_COMPOUND_TAG.map(ToolNBT::new, ToolNBT::tag);

  /**
   * Gets the tool NBT on the given stack for reading
   * @param stack  Stack
   * @return  Tool NBT, or an empty tag if missing. Must not be modified
   */
  public static CompoundTag read(ItemStack stack) {
    ToolNBT data = stack.get(TinkerCommons.toolData);
    return data == null ? EMPTY.tag : data.tag;
  }

  /**
   * Modifies the tool NBT on the given stack. For tools, prefer {@link ToolStack} which avoids extra copies.
   * @param stack  Stack to modify
   * @param edit   Logic modifying a copy of the tag
   */
  public static void update(ItemStack stack, Consumer<CompoundTag> edit) {
    CompoundTag tag = read(stack).copy();
    edit.accept(tag);
    stack.set(TinkerCommons.toolData, new ToolNBT(tag));
  }

  @Override
  public String toString() {
    return "ToolNBT" + tag;
  }
}
