package slimeknights.tconstruct.library.tools.capability;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import slimeknights.mantle.registration.holder.DeferredAttachment;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Allows an entity to store modifiers, used on projectiles fired from modifiable items.
 * As of 1.21 this is a data attachment rather than a capability; the name is kept for compatibility.
 */
public class EntityModifierCapability {
  /** Default instance to use with orElse */
  public static final EntityModifiers EMPTY = new EntityModifiers() {
    @Override
    public ModifierNBT getModifiers() {
      return ModifierNBT.EMPTY;
    }

    @Override
    public void setModifiers(ModifierNBT nbt) {}

    @Override
    public void addModifiers(ModifierNBT nbt) {}
  };

  private EntityModifierCapability() {}

  /* Static helpers */

  /** List of predicates to check if the entity supports this capability */
  private static final List<Predicate<Entity>> ENTITY_PREDICATES = new ArrayList<>();

  /** Attachment ID */
  private static final ResourceLocation ID = TConstruct.getResource("modifiers");
  /** Attachment type */
  public static final DeferredAttachment<Modifiers> TYPE = DeferredAttachment.createType(ID);
  /** Serializer for the attachment */
  private static final IAttachmentSerializer<ListTag,Modifiers> SERIALIZER = new IAttachmentSerializer<>() {
    @Override
    public Modifiers read(IAttachmentHolder holder, ListTag tag, HolderLookup.Provider provider) {
      Modifiers modifiers = new Modifiers();
      modifiers.setModifiers(ModifierNBT.readFromNBT(tag));
      return modifiers;
    }

    @Override
    public ListTag write(Modifiers attachment, HolderLookup.Provider provider) {
      return attachment.getModifiers().serializeToNBT();
    }
  };

  /** Gets the modifiers for the entity or an empty instance if unsupported */
  public static EntityModifiers getCapability(Entity entity) {
    return supportCapability(entity) ? entity.getData(TYPE) : EMPTY;
  }

  /** Gets the data or an empty instance if missing */
  public static ModifierNBT getOrEmpty(Entity entity) {
    return getCapability(entity).getModifiers();
  }

  /** Checks if the given entity supports this capability */
  public static boolean supportCapability(Entity entity) {
    for (Predicate<Entity> entityPredicate : ENTITY_PREDICATES) {
      if (entityPredicate.test(entity)) {
        return true;
      }
    }
    return false;
  }

  /** Registers a predicate of entites that need this capability */
  public static void registerEntityPredicate(Predicate<Entity> predicate) {
    ENTITY_PREDICATES.add(predicate);
  }

  /** Registers the attachment type */
  public static void register(RegisterEvent event) {
    event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ID, () -> AttachmentType.builder(Modifiers::new).serialize(SERIALIZER).build());
  }

  /** Attachment instance */
  public static class Modifiers implements EntityModifiers {
    @Getter @Setter
    private ModifierNBT modifiers = ModifierNBT.EMPTY;
  }

  /** Interface for callers to use */
  public interface EntityModifiers {
    /** Gets the stored modifiers */
    ModifierNBT getModifiers();

    /** Sets the stored modifiers */
    void setModifiers(ModifierNBT nbt);

    /** Adds additional modifiers to the stored modifiers */
    default void addModifiers(ModifierNBT nbt) {
      ModifierNBT existing = getModifiers();
      if (existing.isEmpty()) {
        setModifiers(nbt);
      } else {
        setModifiers(ModifierNBT.builder().add(existing).add(nbt).build());
      }
    }
  }
}
