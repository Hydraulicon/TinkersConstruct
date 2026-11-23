"""Map net.minecraftforge imports to NeoForge 21.1 equivalents. Prints the mapping and the unresolved list."""
import re, sys, json, collections
IDX = sys.argv[1]
classes = set()
for f in ['neo_mc.txt', 'fml.txt', 'bus.txt', 'mantle.txt']:
    classes |= {l.strip() for l in open(f'{IDX}/{f}') if '.' in l}
by_simple = collections.defaultdict(list)
for c in classes:
    by_simple[c.rsplit('.', 1)[1]].append(c)

# Known class renames in NeoForge (simple name old -> FQN new). Identifiers are renamed in code as well.
RENAMES = {
    'MinecraftForge': 'net.neoforged.neoforge.common.NeoForge',
    'ForgeConfigSpec': 'net.neoforged.neoforge.common.ModConfigSpec',
    'ForgeHooks': 'net.neoforged.neoforge.common.CommonHooks',
    'ForgeHooksClient': 'net.neoforged.neoforge.client.ClientHooks',
    'ForgeEventFactory': 'net.neoforged.neoforge.event.EventHooks',
    'ForgeMod': 'net.neoforged.neoforge.common.NeoForgeMod',
    'ForgeFlowingFluid': 'net.neoforged.neoforge.fluids.BaseFlowingFluid',
    'ForgeSpawnEggItem': 'net.neoforged.neoforge.common.DeferredSpawnEggItem',
    'ForgeRenderTypes': 'net.neoforged.neoforge.client.NeoForgeRenderTypes',
    'ForgeI18n': 'net.neoforged.fml.I18nExtension',
    'ToolAction': 'net.neoforged.neoforge.common.ItemAbility',
    'ToolActions': 'net.neoforged.neoforge.common.ItemAbilities',
    'IForgeItem': 'net.neoforged.neoforge.common.extensions.IItemExtension',
    'IForgeBlock': 'net.neoforged.neoforge.common.extensions.IBlockExtension',
    'IForgeItemStack': 'net.neoforged.neoforge.common.extensions.IItemStackExtension',
    'IForgeBakedModel': 'net.neoforged.neoforge.client.extensions.IBakedModelExtension',
    'IForgeBlockEntity': 'net.neoforged.neoforge.common.extensions.IBlockEntityExtension',
    'IForgeEntity': 'net.neoforged.neoforge.common.extensions.IEntityExtension',
    'IForgeVertexConsumer': 'net.neoforged.neoforge.client.extensions.IVertexConsumerExtension',
    'IForgeMenuType': 'net.neoforged.neoforge.common.extensions.IMenuTypeExtension',
    'IForgeShearable': 'net.neoforged.neoforge.common.IShearable',
    'ForgeAdvancementProvider': 'net.minecraft.data.advancements.AdvancementProvider',
    'ForgeTier': 'net.neoforged.neoforge.common.SimpleTier',
    'Dist': 'net.neoforged.api.distmarker.Dist',
    'OnlyIn': 'net.neoforged.api.distmarker.OnlyIn',
    'ShieldBlockEvent': 'net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent',
    'IEntityAdditionalSpawnData': 'net.neoforged.neoforge.entity.IEntityWithComplexSpawn',
    'SpawnPlacementRegisterEvent': 'net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent',
    'EmptyHandler': 'net.neoforged.neoforge.items.wrapper.EmptyItemHandler',
}
# packages for direct moves
PKG = [('net.minecraftforge.fml.', 'net.neoforged.fml.'),
       ('net.minecraftforge.eventbus.api.', 'net.neoforged.bus.api.'),
       ('net.minecraftforge.', 'net.neoforged.neoforge.')]

def split(fqn):
    """returns (top level class fqn, rest) e.g. a.b.C.D.e -> (a.b.C, .D.e)"""
    parts = fqn.split('.')
    for i, p in enumerate(parts):
        if p[:1].isupper():
            return '.'.join(parts[:i+1]), ''.join('.' + x for x in parts[i+1:])
    return fqn, ''  # wildcard / package

def resolve(top):
    simple = top.rsplit('.', 1)[1]
    if simple in RENAMES: return RENAMES[simple], 'rename'
    for a, b in PKG:
        if top.startswith(a) and top.replace(a, b, 1) in classes: return top.replace(a, b, 1), 'move'
    c = [x for x in by_simple.get(simple, []) if x.startswith('net.neoforged')]
    if len(c) == 1: return c[0], 'simple'
    if len(c) > 1:
        # prefer the most similar package
        old = top.split('.')
        best = max(c, key=lambda x: len(set(x.split('.')) & set(old)))
        return best, 'ambiguous:' + ','.join(c)
    return None, 'missing'

if __name__ == '__main__':
    out = {}
    for line in open(f'{IDX}/forge_imports.txt'):
        n, fqn = line.split()
        top, rest = split(fqn)
        new, how = resolve(top)
        out[top] = (new, how, int(n) + out.get(top, (0, 0, 0))[2])
    json.dump({k: v[0] for k, v in out.items() if v[0]}, open(f'{IDX}/forge_map.json', 'w'), indent=1)
    for k, (new, how, n) in sorted(out.items(), key=lambda kv: -kv[1][2]):
        if how != 'move':
            print(f'{n:5} {how[:9]:9} {k} -> {new}' + (f'   [{how[10:]}]' if how.startswith('amb') else ''))
    print(collections.Counter(v[1].split(':')[0] for v in out.values()))
