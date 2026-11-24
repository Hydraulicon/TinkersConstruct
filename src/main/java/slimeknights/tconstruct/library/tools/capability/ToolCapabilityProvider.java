package slimeknights.tconstruct.library.tools.capability;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Capability providers for tool stacks.
 * As of 1.21, capabilities are looked up on demand, so providers are registered per capability and queried with a {@link ToolStack} linked to the stack.
 */
public class ToolCapabilityProvider {
  private static final Map<ItemCapability<?,Void>,List<IToolCapabilityProvider<?>>> PROVIDERS = new LinkedHashMap<>();

  private ToolCapabilityProvider() {}

  /**
   * Registers a tool capability provider. Must be called before {@link RegisterCapabilitiesEvent}, typically during mod construction.
   * @param capability  Capability provided
   * @param provider    Provider instance, returns null if the capability is not present
   */
  public static <T> void register(ItemCapability<T,Void> capability, IToolCapabilityProvider<T> provider) {
    PROVIDERS.computeIfAbsent(capability, c -> new ArrayList<>()).add(provider);
  }

  /** Registers all providers for all modifiable items */
  public static void registerCapabilities(RegisterCapabilitiesEvent event) {
    Item[] items = BuiltInRegistries.ITEM.stream().filter(item -> item instanceof IModifiable).toArray(Item[]::new);
    if (items.length > 0) {
      PROVIDERS.forEach((capability, providers) -> registerCapability(event, capability, providers, items));
    }
  }

  /** Registers a single capability for all modifiable items */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static <T> void registerCapability(RegisterCapabilitiesEvent event, ItemCapability<T,Void> capability, List<IToolCapabilityProvider<?>> providers, Item[] items) {
    List<IToolCapabilityProvider<T>> casted = (List<IToolCapabilityProvider<T>>)(List)providers;
    event.registerItem(capability, (stack, context) -> {
      // the tool is linked to the stack, so changes made by the capability are written back to it
      ToolStack tool = ToolStack.from(stack);
      // return the first successful provider
      for (IToolCapabilityProvider<T> provider : casted) {
        T value = provider.getCapability(stack, tool);
        if (value != null) {
          return value;
        }
      }
      return null;
    }, items);
  }

  /** Interface to get a capability on a tool */
  @FunctionalInterface
  public interface IToolCapabilityProvider<T> {
    /** Gets a capability on the given tool, or null if the tool does not have it */
    @Nullable
    T getCapability(ItemStack stack, IToolStackView tool);
  }
}
