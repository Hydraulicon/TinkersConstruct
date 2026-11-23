"""Converts Tinkers' Forge SimpleChannel packets to NeoForge CustomPacketPayload, for the 1.21.1 port.
Handles classes with a FriendlyByteBuf constructor and encode method; others are reported for manual porting."""
import os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from errfix import add_import


def snake(name):
    name = re.sub(r'Packet$', '', name)
    return re.sub(r'(?<!^)(?=[A-Z])', '_', name).lower()


def convert(path):
    src = open(path, encoding='utf-8').read()
    name = os.path.basename(path)[:-5]
    o = src
    be = re.search(r'implements BlockEntityPacket<', src) is not None
    src = re.sub(r'\bimplements IThreadsafePacket\b', 'implements ISimplePacket', src)
    # imports
    src = re.sub(r'^// TODO 1\.21 import: slimeknights\.mantle\.network\.packet\.IThreadsafePacket;\n', '', src, flags=re.M)
    src = re.sub(r'^(// TODO 1\.21 import: |import )net\.minecraftforge\.network\.NetworkEvent(\.Context)?;\n', '', src, flags=re.M)
    src = src.replace('import slimeknights.mantle.network.packet.ISimplePacket;', 'import slimeknights.mantle.network.ISimplePacket;')
    src = src.replace('import slimeknights.mantle.network.packet.BlockEntityPacket;', 'import slimeknights.mantle.network.BlockEntityPacket;')
    # handlers
    src = re.sub(r'public void handleThreadsafe\(Context (\w+)\)', r'public void handle(IPayloadContext \1)', src)
    src = re.sub(r'public void handleBlockEntity\(Context (\w+), ', r'public void handleBlockEntity(IPayloadContext \1, ', src)
    src = re.sub(r'\b(\w+)\.getSender\(\)', r'((ServerPlayer) \1.player())', src)
    if be:
        src = re.sub(r'public Class<(\w+)> type\(\)', r'public Class<\1> blockEntityType()', src)
    # buffers
    src = re.sub(r'public ' + name + r'\(FriendlyByteBuf (\w+)\)', r'public ' + name + r'(RegistryFriendlyByteBuf \1)', src)
    simple = re.search(r'public ' + name + r'\(RegistryFriendlyByteBuf \w+\)', src) and re.search(r'void encode\((Registry)?FriendlyByteBuf \w+\)', src) \
      and 'Type<' + name + '> TYPE' not in src
    src = re.sub(r'\n([ \t]*)@Override\n(\s*public void encode\()(Registry)?FriendlyByteBuf', r'\n\2RegistryFriendlyByteBuf', src)
    src = re.sub(r'public void encode\(FriendlyByteBuf', 'public void encode(RegistryFriendlyByteBuf', src)
    if simple:
        decl = re.search(r'^public (?:final )?(class|record) ' + name + r'\b[^{]*\{\n', src, flags=re.M)
        fields = (f'  public static final Type<{name}> TYPE = new Type<>(TConstruct.getResource("{snake(name)}"));\n'
                  f'  public static final StreamCodec<RegistryFriendlyByteBuf, {name}> CODEC = StreamCodec.ofMember({name}::encode, {name}::new);\n\n')
        src = src[:decl.end()] + fields + src[decl.end():]
        # type() before the last closing brace
        k = src.rstrip().rindex('}')
        src = src[:k] + f'\n  @Override\n  public Type<{name}> type() {{\n    return TYPE;\n  }}\n' + src[k:]
    lines = src.split('\n')
    for imp, cond in [('slimeknights.mantle.network.ISimplePacket', 'ISimplePacket'), ('net.neoforged.neoforge.network.handling.IPayloadContext', 'IPayloadContext'),
                      ('net.minecraft.network.RegistryFriendlyByteBuf', 'RegistryFriendlyByteBuf'), ('net.minecraft.network.codec.StreamCodec', 'StreamCodec<'),
                      ('slimeknights.tconstruct.TConstruct', 'TConstruct.getResource'), ('net.minecraft.server.level.ServerPlayer', '(ServerPlayer)')]:
        if cond in src and f'import {imp};' not in src and not (imp.endswith('TConstruct') and path.endswith('TConstruct.java')):
            add_import(lines, imp)
    src = '\n'.join(lines)
    if src != o:
        open(path, 'w', encoding='utf-8').write(src)
    return bool(simple)


if __name__ == '__main__':
    for p in sys.argv[1:]:
        ok = convert(p)
        print(('ok     ' if ok else 'MANUAL ') + os.path.basename(p))
