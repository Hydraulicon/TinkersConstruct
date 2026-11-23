package slimeknights.tconstruct.library.utils;

import java.util.Arrays;
import slimeknights.mantle.network.PacketHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.VisibleForTesting;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.common.config.Config;

/** Helpers for a few JSON related tasks */
public class JsonUtils {
  private JsonUtils() {}

  /** Called when the player logs in to send packets */
  public static void syncPackets(OnDatapackSyncEvent event, CustomPacketPayload... packets) {
    if (packets.length > 0) {
      PacketHelper.syncPackets(event, packets[0], Arrays.copyOfRange(packets, 1, packets.length));
    }
  }

  /** Creates a JSON object with the given key set to a resource location */
  public static JsonObject withLocation(String key, ResourceLocation value) {
    JsonObject json = new JsonObject();
    json.addProperty(key, value.toString());
    return json;
  }

  /** Creates a JSON object with the given type set, makes using {@link slimeknights.mantle.data.gson.GenericRegisteredSerializer} easier */
  public static JsonObject withType(ResourceLocation type) {
    return withLocation("type", type);
  }


  private static boolean allowDebugLogging;

  /** Forces the debug logging to be disabled. Used during testing since config is not loaded */
  @Internal
  @VisibleForTesting
  public static void forceDisableDebugLog() {
    allowDebugLogging = false;
  }

  /** Checks if we should log the resource values into the debug log */
  public static boolean debugLogResourceValues() {
    return allowDebugLogging && Config.COMMON.debugLogResourceValues.get();
  }
}
