package slimeknights.tconstruct.library.tools.item.armor;

import lombok.Getter;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import slimeknights.mantle.registration.object.IdAwareObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Armor material for Tinkers armor, which bypasses all vanilla armor stats.
 * <p>
 * As of 1.21 vanilla armor materials are registry entries, so each instance owns a registered vanilla material with zero stats
 * and this material's equip sound, see {@link #getHolder()}. All instances are registered by Tinkers in {@link #registerAll(RegisterEvent)}.
 */
@Getter
public class DummyArmorMaterial implements IdAwareObject {
  /** All materials created, registered as vanilla materials in {@link #registerAll(RegisterEvent)} */
  private static final List<DummyArmorMaterial> ALL = new ArrayList<>();

  private final ResourceLocation id;
  private final SoundEvent equipSound;
  /** Vanilla armor material, for use with {@link net.minecraft.world.item.ArmorItem} */
  private final DeferredHolder<ArmorMaterial,ArmorMaterial> holder;

  public DummyArmorMaterial(ResourceLocation id, SoundEvent equipSound) {
    this.id = id;
    this.equipSound = equipSound;
    this.holder = DeferredHolder.create(Registries.ARMOR_MATERIAL, id);
    synchronized (ALL) {
      ALL.add(this);
    }
  }

  @Override
  public ResourceLocation id() {
    return id;
  }

  /** Gets the name of this material */
  public String getName() {
    return id.toString();
  }

  /** Creates the vanilla armor material, with all stats 0 as we handle them ourselves */
  private ArmorMaterial createVanilla() {
    Holder<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(equipSound);
    return new ArmorMaterial(Util.make(new EnumMap<>(Type.class), map -> {
      for (Type type : Type.values()) {
        map.put(type, 0);
      }
    }), 0, sound, () -> Ingredient.EMPTY, List.of(), 0, 0);
  }

  /** Registers the vanilla material for all instances. Called by Tinkers, materials must be created before {@link RegisterEvent} */
  public static void registerAll(RegisterEvent event) {
    synchronized (ALL) {
      for (DummyArmorMaterial material : ALL) {
        event.register(Registries.ARMOR_MATERIAL, material.id, material::createVanilla);
      }
    }
  }
}
