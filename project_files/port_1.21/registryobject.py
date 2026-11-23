"""RegistryObject<T> -> DeferredHolder<R, T> for the 1.21.1 port. R is derived from the type of T."""
import os, re, sys

BASES = [  # (regex on T's outer type name, registry type)
    (r'(Simple|TypeAware)?RecipeSerializer', 'RecipeSerializer<?>'),
    (r'RecipeType', 'RecipeType<?>'),
    (r'BlockEntityType', 'BlockEntityType<?>'),
    (r'EntityType', 'EntityType<?>'),
    (r'MenuType', 'MenuType<?>'),
    (r'Attribute', 'Attribute'),
    (r'\w*Effect', 'MobEffect'),
    (r'CreativeModeTab', 'CreativeModeTab'),
    (r'(Simple)?ParticleType', 'ParticleType<?>'),
    (r'LootItemConditionType', 'LootItemConditionType'),
    (r'LootItemFunctionType', 'LootItemFunctionType<?>'),
    (r'LootPoolEntryType', 'LootPoolEntryType'),
    (r'\w*Feature', 'Feature<?>'),
    (r'TreeDecoratorType', 'TreeDecoratorType<?>'),
    (r'RootPlacerType', 'RootPlacerType<?>'),
    (r'StructurePieceType', 'StructurePieceType'),
    (r'StructureType', 'StructureType<?>'),
    (r'Potion', 'Potion'),
    (r'EntityDataSerializer', 'EntityDataSerializer<?>'),
    (r'\w*Item', 'Item'),
    (r'\w*Block', 'Block'),
]


def generic_arg(s, start):
    """s[start] == '<'; returns (inner text, index after closing '>')"""
    depth = 0
    for i in range(start, len(s)):
        if s[i] == '<': depth += 1
        elif s[i] == '>':
            depth -= 1
            if depth == 0: return s[start + 1:i], i + 1
    return None, None


def convert(src):
    out, pos, n, skipped = [], 0, 0, []
    for m in re.finditer(r'\bRegistryObject<', src):
        if m.start() < pos: continue
        inner, end = generic_arg(src, m.end() - 1)
        if inner is None: continue
        t = re.sub(r'^\?\s*extends\s+', '', inner.strip())
        name = re.match(r'\w+', t).group(0)
        base = next((b for rx, b in BASES if re.fullmatch(rx, name)), None)
        if base is None:
            skipped.append(inner); continue
        out.append(src[pos:m.start()]); out.append(f'DeferredHolder<{base}, {inner}>'); pos = end; n += 1
    out.append(src[pos:])
    return ''.join(out), n, skipped


if __name__ == '__main__':
    total = 0
    for root in sys.argv[1:]:
        for dp, _, fs in os.walk(root):
            for f in fs:
                if not f.endswith('.java'): continue
                p = os.path.join(dp, f)
                s = open(p, encoding='utf-8').read()
                if 'RegistryObject' not in s: continue
                s, n, skipped = convert(s)
                s = s.replace('// TODO 1.21 import: net.minecraftforge.registries.RegistryObject;', 'import net.neoforged.neoforge.registries.DeferredHolder;')
                for k in skipped: print(f'skipped {p}: RegistryObject<{k}>')
                total += n
                open(p, 'w', encoding='utf-8').write(s)
    print(total, 'converted')
