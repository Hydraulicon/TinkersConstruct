package slimeknights.tconstruct.library.tools.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiFunction;

/**
 * NBT representing extra data on the tool for modifiers, with a wrapper around the compound for to enforce namespacing data.
 * On a typical tool, there are two copies of this class, one for persistent data, and one that rebuilds when the modifiers refresh.
 * Note unlike other NBT classes, the data inside this one is mutable as most of it is directly used by the tools.
 * <p>
 * All access goes through {@link #getData()} for reading and {@link #getDataForWrite()} plus {@link #onChanged()} for writing.
 * This lets {@link ToolStack} back the data with its copy-on-write tool NBT, as data components are immutable in 1.21.
 */
public class ModDataNBT implements IModDataView {
  /** Compound representing modifier data */
  private final CompoundTag data;

  protected ModDataNBT(CompoundTag data) {
    this.data = data;
  }

  /**
   * Creates a new mod data containing empty data
   */
  public ModDataNBT() {
    this(new CompoundTag());
  }

  /** Gets the data for reading. Callers must not modify the returned tag, use {@link #getDataForWrite()} */
  protected CompoundTag getData() {
    return data;
  }

  /** Gets the data for writing, call {@link #onChanged()} after modifying it */
  protected CompoundTag getDataForWrite() {
    return data;
  }

  /** Called after the data is modified */
  protected void onChanged() {}

  @Override
  public <T> T get(ResourceLocation name, BiFunction<CompoundTag,String,T> function) {
    return function.apply(getData(), name.toString());
  }

  @Override
  public ListTag getList(ResourceLocation name, int type) {
    // save generation of the extra lambda object
    return getData().getList(name.toString(), type);
  }

  @Override
  public boolean contains(ResourceLocation name) {
    return getData().contains(name.toString());
  }

  @Override
  public boolean contains(ResourceLocation name, int type) {
    return getData().contains(name.toString(), type);
  }

  /**
   * Sets the given NBT into the data
   * @param name  Key name
   * @param nbt   NBT value
   */
  public void put(ResourceLocation name, Tag nbt) {
    getDataForWrite().put(name.toString(), nbt);
    onChanged();
  }

  /**
   * Sets an integer from the mod data
   * @param name  Name
   * @param value  Integer value
   */
  public void putInt(ResourceLocation name, int value) {
    getDataForWrite().putInt(name.toString(), value);
    onChanged();
  }

  /**
   * Sets an boolean from the mod data
   * @param name  Name
   * @param value  Boolean value
   */
  public void putBoolean(ResourceLocation name, boolean value) {
    getDataForWrite().putBoolean(name.toString(), value);
    onChanged();
  }

  /**
   * Sets an float from the mod data
   * @param name  Name
   * @param value  Float value
   */
  public void putFloat(ResourceLocation name, float value) {
    getDataForWrite().putFloat(name.toString(), value);
    onChanged();
  }

  /**
   * Reads a string from the mod data
   * @param name  Name
   * @param value  String value
   */
  public void putString(ResourceLocation name, String value) {
    getDataForWrite().putString(name.toString(), value);
    onChanged();
  }

  /**
   * Removes the given key from the NBT
   * @param name  Key to remove
   */
  public void remove(ResourceLocation name) {
    getDataForWrite().remove(name.toString());
    onChanged();
  }


  /* Networking */

  /** Gets a copy of the internal data, generally should only be used for syncing, no reason to call directly */
  public CompoundTag getCopy() {
    return getData().copy();
  }

  /**
   * Called to merge this NBT data from another
   * @param data  data
   */
  public void copyFrom(CompoundTag data) {
    CompoundTag tag = getDataForWrite();
    tag.getAllKeys().clear();
    tag.merge(data);
    onChanged();
  }

  /**
   * Parses the data from NBT
   * @param data  data
   * @return  Parsed mod data
   */
  public static ModDataNBT readFromNBT(CompoundTag data) {
    return new ModDataNBT(data);
  }

  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof ModDataNBT mod && getData().equals(mod.getData());
  }

  @Override
  public int hashCode() {
    return getData().hashCode();
  }
}
