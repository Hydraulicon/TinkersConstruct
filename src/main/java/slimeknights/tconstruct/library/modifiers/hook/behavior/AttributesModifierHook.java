package slimeknights.tconstruct.library.modifiers.hook.behavior;

import java.util.function.Function;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EntityInteractionModifierHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.Collection;
import java.util.function.BiConsumer;

/**
 * Modifier hook for adding attributes to a tool when in the correct slot.
 * <p>
 * As of 1.21, attribute modifiers are identified by a {@link ResourceLocation} which must be unique per slot,
 * or modifiers from the same tool in different slots replace each other. Use {@link #slotId(ResourceLocation, EquipmentSlot)} to make one.
 */
public interface AttributesModifierHook {
  /** Base ID for armor on held tools */
  ResourceLocation HELD_ARMOR = TConstruct.getResource("held_armor");
  /** Base ID for toughness on held tools */
  ResourceLocation HELD_TOUGHNESS = TConstruct.getResource("held_toughness");
  /** Base ID for knockback resistance on held tools */
  ResourceLocation HELD_KNOCKBACK_RESISTANCE = TConstruct.getResource("held_knockback_resistance");

  /**
   * Adds attributes from this modifier's effect
   * @param tool      Current tool instance
   * @param modifier  Modifier level
   * @param slot      Slot for the attributes
   * @param consumer  Attribute consumer
   */
  void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<Holder<Attribute>,AttributeModifier> consumer);

  /** Creates an attribute modifier ID unique to the given slot */
  static ResourceLocation slotId(ResourceLocation base, EquipmentSlot slot) {
    return base.withSuffix("." + slot.getSerializedName());
  }

  /** Gets attribute modifiers for a weapon with melee capability */
  static Multimap<Holder<Attribute>,AttributeModifier> getHeldAttributeModifiers(IToolStackView tool, EquipmentSlot slot) {
    ImmutableMultimap.Builder<Holder<Attribute>,AttributeModifier> builder = ImmutableMultimap.builder();
    if (!tool.isBroken()) {
      // base melee stats - skip if not melee
      StatsNBT statsNBT = tool.getStats();
      if (slot == EquipmentSlot.MAINHAND && EntityInteractionModifierHook.isMeleeWeapon(tool)) {
        // vanilla IDs are displayed as the base stats of the tool
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, statsNBT.get(ToolStats.ATTACK_DAMAGE), AttributeModifier.Operation.ADD_VALUE));
        // base attack speed is 4, but our numbers start from 4
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, statsNBT.get(ToolStats.ATTACK_SPEED) - 4d, AttributeModifier.Operation.ADD_VALUE));
      }
      if (slot.getType() == Type.HAND) {
        // shields and slimestaffs can get armor
        if (tool.hasTag(TinkerTags.Items.ARMOR)) {
          double value = statsNBT.get(ToolStats.ARMOR);
          if (value != 0) {
            builder.put(Attributes.ARMOR, new AttributeModifier(slotId(HELD_ARMOR, slot), value, AttributeModifier.Operation.ADD_VALUE));
          }
          value = statsNBT.get(ToolStats.ARMOR_TOUGHNESS);
          if (value != 0) {
            builder.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(slotId(HELD_TOUGHNESS, slot), value, AttributeModifier.Operation.ADD_VALUE));
          }
          value = statsNBT.get(ToolStats.KNOCKBACK_RESISTANCE);
          if (value != 0) {
            builder.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(slotId(HELD_KNOCKBACK_RESISTANCE, slot), value, AttributeModifier.Operation.ADD_VALUE));
          }
        }
        // grab attributes from modifiers, only do for hands (other slots would just be weird)
        BiConsumer<Holder<Attribute>,AttributeModifier> attributeConsumer = builder::put;
        for (ModifierEntry entry : tool.getModifierList()) {
          entry.getHook(ModifierHooks.ATTRIBUTES).addAttributes(tool, entry, slot, attributeConsumer);
        }
      }
    }
    return builder.build();
  }

  /**
   * Converts per slot attribute modifiers into the vanilla attribute modifier component
   * @param getter         Gets the modifiers for a slot
   * @param showInTooltip  If false, vanilla will not display these modifiers in the tooltip
   * @param slots          Slots to include
   * @return  Attribute modifiers for the stack
   */
  static ItemAttributeModifiers toItemAttributes(Function<EquipmentSlot,Multimap<Holder<Attribute>,AttributeModifier>> getter, boolean showInTooltip, EquipmentSlot... slots) {
    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
    for (EquipmentSlot slot : slots) {
      EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slot);
      getter.apply(slot).forEach((attribute, modifier) -> builder.add(attribute, modifier, group));
    }
    ItemAttributeModifiers modifiers = builder.build();
    return showInTooltip ? modifiers : modifiers.withTooltip(false);
  }

  /** Merger that runs all hooks */
  record AllMerger(Collection<AttributesModifierHook> modules) implements AttributesModifierHook {
    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<Holder<Attribute>,AttributeModifier> consumer) {
      for (AttributesModifierHook module : modules) {
        module.addAttributes(tool, modifier, slot, consumer);
      }
    }
  }
}
