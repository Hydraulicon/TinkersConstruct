package slimeknights.tconstruct.library.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import slimeknights.mantle.util.RegistryHelper;

import javax.annotation.Nullable;

/**
 * Finds registry access for code that serializes item and fluid stacks without a level available.
 * <p>
 * As of 1.21, stacks need registry access to read and write NBT, as their components may reference datapack registries such as enchantments.
 * Tinkers stores stacks inside tool NBT (tool tanks and inventories), which is often accessed without a level.
 * Prefer passing registry access from a level when one is available.
 */
public final class RegistryAccessHelper {
  private RegistryAccessHelper() {}

  /**
   * Gets the best available registry access: the running server, then the client connection, then the built-in registries.
   * The built-in fallback cannot resolve datapack registries.
   */
  public static HolderLookup.Provider get() {
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    if (server != null) {
      return server.registryAccess();
    }
    if (FMLEnvironment.dist == Dist.CLIENT) {
      HolderLookup.Provider client = ClientAccess.get();
      if (client != null) {
        return client;
      }
    }
    return RegistryHelper.BUILTIN_REGISTRY_ACCESS;
  }

  /** Separate class to avoid loading client classes on the server */
  private static class ClientAccess {
    @Nullable
    private static HolderLookup.Provider get() {
      ClientPacketListener connection = Minecraft.getInstance().getConnection();
      return connection == null ? null : connection.registryAccess();
    }
  }
}
