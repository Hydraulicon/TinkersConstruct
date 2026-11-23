package slimeknights.tconstruct.library.utils;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

/**
 * Helper for use with our extensions of resource location for some type safety in IDs.
 * Note we left {@link ResourceLocation#withPath(String)} and alike as returning {@link ResourceLocation} as there is not much use extending an ID.
 * <p>
 * {@link ResourceLocation} is final as of 1.21, we remove that and widen its constructor using our access transformer.
 * @see IdParser
 */
public abstract class ResourceId extends ResourceLocation {
  /**
   * Creates a new ID without validating the namespace and path, for use when they are already known to be valid.
   * @param pDummy  Marker to distinguish this constructor, replaces the removed {@code ResourceLocation.Dummy}
   */
  protected ResourceId(String namespace, String path, @Nullable Dummy pDummy) {
    super(namespace, path);
  }

  public ResourceId(ResourceLocation location) {
    this(location.getNamespace(), location.getPath(), null);
  }

  public ResourceId(String namespace, String path) {
    this(ResourceLocation.fromNamespaceAndPath(namespace, path));
  }

  public ResourceId(String location) {
    this(ResourceLocation.parse(location));
  }


  /* Helpers for static constructors */

  /**
   * Creates a new ID from the given string
   * @param string  String
   * @return  ID, or null if invalid
   */
  @Nullable
  protected static <T extends ResourceLocation> T tryParse(String string, BiFunction<String,String,T> constructor) {
    ResourceLocation location = ResourceLocation.tryParse(string);
    if (location == null) {
      return null;
    }
    return constructor.apply(location.getNamespace(), location.getPath());
  }

  /**
   * Creates a new ID from the given namespace and path
   * @param namespace  Namespace
   * @param path       Path
   * @return  ID, or null if invalid
   */
  @Nullable
  protected static <T extends ResourceLocation> T tryBuild(String namespace, String path, BiFunction<String,String,T> constructor) {
    if (isValidNamespace(namespace) && isValidPath(path)) {
      return constructor.apply(namespace, path);
    }
    return null;
  }

  /** Marker type for the unvalidated constructor, replacing the removed {@code ResourceLocation.Dummy} */
  protected static final class Dummy {
    private Dummy() {}
  }
}
