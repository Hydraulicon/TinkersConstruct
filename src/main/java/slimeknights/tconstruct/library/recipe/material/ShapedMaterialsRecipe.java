package slimeknights.tconstruct.library.recipe.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import slimeknights.mantle.util.LogicHelper;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tables.TinkerTables;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Shaped recipe with a number of {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient} and
 * {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient} to set the materials of the result.
 */
public class ShapedMaterialsRecipe extends ShapedRecipe implements MaterialsCraftingTableRecipe {
  /** Codec for extra materials, shared with other material crafting recipes */
  static final Codec<List<MaterialVariantId>> EXTRA_MATERIALS_CODEC = MaterialVariantId.CODEC.listOf();
  /** Stream codec for extra materials, shared with other material crafting recipes */
  static final StreamCodec<RegistryFriendlyByteBuf,List<MaterialVariantId>> EXTRA_MATERIALS_STREAM_CODEC = MaterialVariantId.STREAM_CODEC.apply(ByteBufCodecs.list()).cast();

  public static final MapCodec<ShapedMaterialsRecipe> CODEC = RecordCodecBuilder.<Serialized>mapCodec(inst -> inst.group(
    Codec.STRING.optionalFieldOf("group", "").forGetter(Serialized::group),
    CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(Serialized::category),
    ShapedRecipePattern.Data.MAP_CODEC.forGetter(Serialized::data),
    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(Serialized::result),
    Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(Serialized::showNotification),
    Codec.STRING.fieldOf("parts").forGetter(Serialized::parts),
    EXTRA_MATERIALS_CODEC.optionalFieldOf("extra_materials", List.of()).forGetter(Serialized::extraMaterials)
  ).apply(inst, Serialized::new)).flatXmap(Serialized::toRecipe, Serialized::fromRecipe);
  public static final StreamCodec<RegistryFriendlyByteBuf,ShapedMaterialsRecipe> STREAM_CODEC = StreamCodec.of(ShapedMaterialsRecipe::toNetwork, ShapedMaterialsRecipe::fromNetwork);

  /** List of tool parts to search for in the final recipe */
  @Getter
  private final List<Ingredient> parts;
  /**
   * If true, a part may show up multiple times in the inputs, and all copies should match.
   * If false, only the first instance of a part is checked for each input, allowing a tool with the same part multiple times.
   */
  private final boolean checkRepeats;
  /** List of additional materials to add beyond the parts */
  @Getter
  private final List<MaterialVariantId> extraMaterials;

  public ShapedMaterialsRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification, List<Ingredient> parts, List<MaterialVariantId> extraMaterials) {
    super(group, category, pattern, result, showNotification);
    this.parts = parts;
    this.checkRepeats = parts.stream().unordered().distinct().count() == parts.size();
    this.extraMaterials = extraMaterials;
  }

  @Override
  public int getPartCount() {
    return parts.size();
  }

  /**
   * Finds materials for each of the parts
   * @return Array of all matched materials. Array will have no null entries, though the array may be null if no match was found.
   */
  @Nullable
  static MaterialVariantId[] findMaterials(CraftingInput inventory, List<Ingredient> parts, int partCount, boolean checkRepeats) {
    // want one material for each
    MaterialVariantId[] materials = new MaterialVariantId[partCount];
    for (int i = 0; i < inventory.size(); i++) {
      ItemStack stack = inventory.getItem(i);
      if (!stack.isEmpty()) {
        for (int p = 0; p < partCount; p++) {
          MaterialVariantId current = materials[p];
          // if we have not found the material yet, or repeats are considered the same material, test the ingredient
          if ((current == null || checkRepeats) && parts.get(p).test(stack)) {
            MaterialVariantId matched = MaterialRecipeCache.getMaterial(stack);
            // first occurrence? thats our material
            if (current == null) {
              materials[p] = matched;
              break;
            } else if (!current.matchesVariant(matched)) {
              // if same material but different variants, just discard the variant
              if (current.getId().equals(matched.getId())) {
                materials[p] = current.getId();
                break;
              } else {
                // if different materials, no match
                return null;
              }
            }
          }
        }
      }
    }
    // ensure we found all materials needed
    for (int p = 0; p < partCount; p++) {
      if (materials[p] == null) {
        return null;
      }
    }
    return materials;
  }

  @Override
  public boolean matches(CraftingInput inventory, Level level) {
    if (!super.matches(inventory, level)) {
      return false;
    }
    // ensure all part materials matched and we found all parts
    return findMaterials(inventory, parts, parts.size(), checkRepeats) != null;
  }

  /** Common logic to this and {@link ShapedMaterialsRecipe} */
  public static void setMaterial(ItemStack stack, MaterialVariantId material, List<MaterialVariantId> extraMaterials) {
    if (extraMaterials.isEmpty() && stack.getItem() instanceof IMaterialItem materialItem) {
      materialItem.setMaterial(stack, material);
    } else {
      MaterialNBT.Builder builder = MaterialNBT.builder();
      builder.add(material);
      for (MaterialVariantId extraMaterial : extraMaterials) {
        builder.add(extraMaterial);
      }
      ToolStack.from(stack).setMaterials(builder.build());
    }
  }

  /** Sets the material for the given stack */
  @Override
  public void setMaterial(ItemStack stack, MaterialVariantId material) {
    setMaterial(stack, material, extraMaterials);
  }

  /** Assembles the item with material information */
  static ItemStack assemble(ItemStack stack, CraftingInput inventory, List<Ingredient> parts, int partCount, boolean checkRepeats, List<MaterialVariantId> extraMaterials) {
    MaterialVariantId[] materials = findMaterials(inventory, parts, partCount, checkRepeats);
    if (materials != null) {
      // if the result is a tool part, and we only have the one material, set its material
      if (materials.length == 1 && extraMaterials.isEmpty() && stack.getItem() instanceof IMaterialItem materialItem) {
        return materialItem.setMaterial(stack, materials[0]);
      }
      MaterialNBT.Builder builder = MaterialNBT.builder();
      // add each material
      for (MaterialVariantId material : materials) {
        builder.add(material);
      }
      // add extra materials
      builder.add(extraMaterials);
      ToolStack.from(stack).setMaterials(builder.build());
    }
    return stack;
  }

  @Override
  public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registryAccess) {
    return assemble(super.assemble(inventory, registryAccess), inventory, parts, parts.size(), checkRepeats, extraMaterials);
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TinkerTables.shapedMaterialsRecipeSerializer.get();
  }


  /* Serializing */

  /** JSON form of the recipe. Parts are a string of keys from the pattern, saving memory by not having separate copies of each ingredient */
  private record Serialized(String group, CraftingBookCategory category, ShapedRecipePattern.Data data, ItemStack result, boolean showNotification, String parts, List<MaterialVariantId> extraMaterials) {
    /** Converts this into a recipe */
    private DataResult<ShapedMaterialsRecipe> toRecipe() {
      ShapedRecipePattern pattern;
      try {
        pattern = ShapedRecipePattern.of(data.key(), data.pattern());
      } catch (IllegalStateException e) {
        return DataResult.error(e::getMessage);
      }
      // the pattern uses the key's ingredient instances, so parts will match by instance
      List<Ingredient> partList = new ArrayList<>(parts.length());
      for (int i = 0; i < parts.length(); i++) {
        char sym = parts.charAt(i);
        Ingredient ingredient = data.key().get(sym);
        if (ingredient == null) {
          return DataResult.error(() -> "Parts references symbol '" + sym + "' but it's not defined in the key");
        }
        partList.add(ingredient);
      }
      return DataResult.success(new ShapedMaterialsRecipe(group, category, pattern, result, showNotification, List.copyOf(partList), extraMaterials));
    }

    /** Converts the recipe into its JSON form */
    private static DataResult<Serialized> fromRecipe(ShapedMaterialsRecipe recipe) {
      Optional<ShapedRecipePattern.Data> optional = recipe.pattern.data;
      if (optional.isEmpty()) {
        return DataResult.error(() -> "Cannot encode unpacked shaped materials recipe");
      }
      ShapedRecipePattern.Data data = optional.get();
      StringBuilder parts = new StringBuilder();
      for (Ingredient part : recipe.parts) {
        Character key = null;
        for (Entry<Character, Ingredient> entry : data.key().entrySet()) {
          if (entry.getValue() == part) {
            key = entry.getKey();
            break;
          }
        }
        if (key == null) {
          return DataResult.error(() -> "Part " + part + " is not in the recipe key");
        }
        parts.append(key);
      }
      return DataResult.success(new Serialized(recipe.getGroup(), recipe.category(), data, recipe.result, recipe.showNotification(), parts.toString(), recipe.extraMaterials));
    }
  }

  /** Writes the recipe to the network, syncing only unique ingredients (by instance comparison) so parts match by instance on the client */
  private static void toNetwork(RegistryFriendlyByteBuf buffer, ShapedMaterialsRecipe recipe) {
    buffer.writeVarInt(recipe.getWidth());
    buffer.writeVarInt(recipe.getHeight());
    buffer.writeUtf(recipe.getGroup());
    buffer.writeEnum(recipe.category());
    ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
    buffer.writeBoolean(recipe.showNotification());
    EXTRA_MATERIALS_STREAM_CODEC.encode(buffer, recipe.extraMaterials);

    List<Ingredient> inputs = recipe.getIngredients();
    List<Ingredient> distinct = inputs.stream().unordered().distinct().toList();
    buffer.writeVarInt(distinct.size());
    for (Ingredient ingredient : distinct) {
      Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
    }
    // to sync inputs, we just sync the index within the distinct list
    for (Ingredient ingredient : inputs) {
      buffer.writeByte(distinct.indexOf(ingredient));
    }
    // parts size is not determined from ingredients size, so sync it directly
    buffer.writeVarInt(recipe.parts.size());
    for (Ingredient ingredient : recipe.parts) {
      buffer.writeByte(distinct.indexOf(ingredient));
    }
  }

  /** Reads the recipe from the network */
  private static ShapedMaterialsRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
    int width = buffer.readVarInt();
    int height = buffer.readVarInt();
    String group = buffer.readUtf();
    CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
    ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
    boolean showNotification = buffer.readBoolean();
    List<MaterialVariantId> extraMaterials = EXTRA_MATERIALS_STREAM_CODEC.decode(buffer);

    int size = buffer.readVarInt();
    List<Ingredient> distinct = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      distinct.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
    }
    NonNullList<Ingredient> inputs = NonNullList.withSize(width * height, Ingredient.EMPTY);
    for (int i = 0; i < inputs.size(); i++) {
      inputs.set(i, LogicHelper.getOrDefault(distinct, buffer.readByte(), Ingredient.EMPTY));
    }
    size = buffer.readVarInt();
    List<Ingredient> parts = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      parts.add(LogicHelper.getOrDefault(distinct, buffer.readByte(), Ingredient.EMPTY));
    }
    return new ShapedMaterialsRecipe(group, category, new ShapedRecipePattern(width, height, inputs, Optional.empty()), result, showNotification, List.copyOf(parts), extraMaterials);
  }
}
