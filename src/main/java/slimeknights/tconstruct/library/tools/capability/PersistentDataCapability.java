package slimeknights.tconstruct.library.tools.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import slimeknights.mantle.registration.holder.DeferredAttachment;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.network.SyncPersistentDataPacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import java.util.Optional;

/**
 * Persistent modifier data stored on entities. As of 1.21 this is a data attachment rather than a capability; the name is kept for compatibility.
 */
public class PersistentDataCapability {
  private PersistentDataCapability() {}

  /** Attachment ID */
  private static final ResourceLocation ID = TConstruct.getResource("persistent_data");
  /** Attachment type */
  public static final DeferredAttachment<ModDataNBT> TYPE = DeferredAttachment.createType(ID);
  /** Serializer for the attachment */
  private static final IAttachmentSerializer<CompoundTag,ModDataNBT> SERIALIZER = new IAttachmentSerializer<>() {
    @Override
    public ModDataNBT read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
      return ModDataNBT.readFromNBT(tag);
    }

    @Override
    public CompoundTag write(ModDataNBT attachment, HolderLookup.Provider provider) {
      return attachment.getCopy();
    }
  };

  /** Checks if the given entity supports persistent data */
  public static boolean supports(Entity entity) {
    // must be on living entities as we use this for potions, but also support anything else with modifiers, this is their data
    return entity instanceof LivingEntity || EntityModifierCapability.supportCapability(entity);
  }

  /** Gets the data on the entity, or empty if the entity does not support persistent data */
  public static Optional<ModDataNBT> get(Entity entity) {
    return supports(entity) ? Optional.of(entity.getData(TYPE)) : Optional.empty();
  }

  /** Gets the data or warns if its missing */
  public static ModDataNBT getOrWarn(Entity entity) {
    if (!supports(entity)) {
      TConstruct.LOG.warn("Missing Tinkers NBT on entity {}, this should not happen", entity.getType());
      return new ModDataNBT();
    }
    return entity.getData(TYPE);
  }

  /** Registers the attachment type */
  public static void register(RegisterEvent event) {
    event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ID, () -> AttachmentType.builder(() -> new ModDataNBT()).serialize(SERIALIZER).copyOnDeath().build());
  }

  /** Registers event listeners for syncing */
  public static void init() {
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerRespawnEvent.class, PersistentDataCapability::playerRespawn);
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerChangedDimensionEvent.class, PersistentDataCapability::playerChangeDimension);
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerLoggedInEvent.class, PersistentDataCapability::playerLoggedIn);
  }

  /** Syncs the data to the given player */
  private static void sync(Player player) {
    TinkerNetwork.getInstance().sendTo(new SyncPersistentDataPacket(player.getData(TYPE).getCopy()), player);
  }

  /** sync data when the player respawns/returns from the end */
  private static void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    sync(event.getEntity());
  }

  /** sync data when the player changes dimensions */
  private static void playerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    sync(event.getEntity());
  }

  /** sync data when the player logs in */
  private static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    sync(event.getEntity());
  }
}
