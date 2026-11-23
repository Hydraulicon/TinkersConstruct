package slimeknights.tconstruct.library.utils;

import net.minecraft.nbt.IntArrayTag;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

/** Helpers related to Tag */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagUtil {
  /* Helper functions */

  /**
   * Reads a block position from Tag
   * @param parent  Parent tag
   * @param key     Position key
   * @param offset  Amount to offset position by
   * @return  Block position, or null if invalid or missing
   */
  @Nullable
  public static BlockPos readOptionalPos(CompoundTag parent, String key, BlockPos offset) {
    BlockPos pos = readPos(parent, key);
    return pos == null ? null : pos.offset(offset);
  }

  /**
   * Writes a block position to NBT using the vanilla format, an int array as of 1.21
   * @param pos  Position to write
   * @return  Position tag
   */
  public static Tag writePos(BlockPos pos) {
    return NbtUtils.writeBlockPos(pos);
  }

  /**
   * Reads a block position from NBT. Supports both the 1.21 int array format and the 1.20 compound format with X, Y, and Z keys,
   * the latter is found in worlds saved before 1.21.
   * @param tag  Tag to read
   * @return  Block position, or null if invalid or missing
   */
  @Nullable
  public static BlockPos readPos(@Nullable Tag tag) {
    if (tag instanceof IntArrayTag array) {
      int[] ints = array.getAsIntArray();
      if (ints.length == 3) {
        return new BlockPos(ints[0], ints[1], ints[2]);
      }
    } else if (tag instanceof CompoundTag compound && compound.contains("X", Tag.TAG_ANY_NUMERIC) && compound.contains("Y", Tag.TAG_ANY_NUMERIC) && compound.contains("Z", Tag.TAG_ANY_NUMERIC)) {
      return new BlockPos(compound.getInt("X"), compound.getInt("Y"), compound.getInt("Z"));
    }
    return null;
  }

  /**
   * Reads a block position from NBT, see {@link #readPos(Tag)}
   * @param parent  Parent tag
   * @param key     Position key
   * @return  Block position, or null if invalid or missing
   */
  @Nullable
  public static BlockPos readPos(CompoundTag parent, String key) {
    return readPos(parent.get(key));
  }

  /**
   * Checks if the given tag is a numeric type
   * @param tag  Tag to check
   * @return  True if the type matches
   */
  public static boolean isNumeric(Tag tag) {
    byte type = tag.getId();
    return type == Tag.TAG_BYTE || type == Tag.TAG_SHORT || type == Tag.TAG_INT || type == Tag.TAG_LONG || type == Tag.TAG_FLOAT || type == Tag.TAG_DOUBLE;
  }
}
