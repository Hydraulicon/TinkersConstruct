package slimeknights.tconstruct.library.materials.traits;

import slimeknights.tconstruct.TConstruct;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.ISimplePacket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialId;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public class UpdateMaterialTraitsPacket implements ISimplePacket {
  public static final Type<UpdateMaterialTraitsPacket> TYPE = new Type<>(TConstruct.getResource("update_material_traits"));
  public static final StreamCodec<RegistryFriendlyByteBuf, UpdateMaterialTraitsPacket> CODEC = StreamCodec.ofMember(UpdateMaterialTraitsPacket::encode, UpdateMaterialTraitsPacket::new);

  protected final Map<MaterialId,MaterialTraits> materialToTraits;

  public UpdateMaterialTraitsPacket(RegistryFriendlyByteBuf buffer) {
    int materialCount = buffer.readInt();
    materialToTraits = new HashMap<>(materialCount);
    for (int i = 0; i < materialCount; i++) {
      MaterialId id = new MaterialId(buffer.readResourceLocation());
      MaterialTraits traits = MaterialTraits.read(buffer);
      materialToTraits.put(id, traits);
    }
  }

  public void encode(RegistryFriendlyByteBuf buffer) {
    buffer.writeInt(materialToTraits.size());
    materialToTraits.forEach((materialId, traits) -> {
      buffer.writeResourceLocation(materialId);
      traits.write(buffer);
    });
  }

  @Override
  public void handle(IPayloadContext context) {
    MaterialRegistry.updateMaterialTraitsFromServer(this);
  }

  @Override
  public Type<UpdateMaterialTraitsPacket> type() {
    return TYPE;
  }
}
