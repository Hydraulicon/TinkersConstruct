"""Phase 1 mechanical rewrites for the 1.21.1 NeoForge port.
Usage: python3 -I rewrite.py <idx dir> <java source root>...
The idx dir is produced by build_index.sh. The scripts are kept for reference and re-runs, and are not part of the mod.
"""
import re, sys, os, json, collections
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import importlib.util
spec = importlib.util.spec_from_file_location('mapimports', os.path.join(os.path.dirname(os.path.abspath(__file__)), 'mapimports.py'))

IDX = sys.argv[1]
ROOTS = sys.argv[2:]
sys.argv = [sys.argv[0], IDX]
mi = importlib.util.module_from_spec(spec); spec.loader.exec_module(mi)

# ---- class maps (old top level FQN -> new top level FQN) ----
SKIP = {'net.minecraftforge.network.NetworkRegistry'}  # internal on NeoForge, handled by the networking rewrite
cmap = {}
for line in open(f'{IDX}/forge_imports.txt'):
    fqn = line.split()[1]
    top, _ = mi.split(fqn)
    if top in SKIP: continue
    new, how = mi.resolve(top)
    if new: cmap[top] = new
# EventBusSubscriber became top level
cmap['net.minecraftforge.fml.common.Mod.EventBusSubscriber'] = 'net.neoforged.fml.common.EventBusSubscriber'

mantle_cls = {l.strip() for l in open(f'{IDX}/mantle.txt') if '.' in l}
mantle_simple = collections.defaultdict(list)
for c in mantle_cls: mantle_simple[c.rsplit('.', 1)[1]].append(c)
for line in open(f'{IDX}/mantle_imports.txt'):
    top, _ = mi.split(line.strip())
    if top not in mantle_cls:
        c = mantle_simple.get(top.rsplit('.', 1)[1], [])
        if len(c) == 1: cmap[top] = c[0]

# simple name renames applied to identifiers in files that referenced the old class
id_renames = {old: new.rsplit('.', 1)[1] for old, new in mi.RENAMES.items() if old != new.rsplit('.', 1)[1]}
id_renames['ForgeConfigSpec'] = 'ModConfigSpec'

# longest keys first so inner-class keys win
keys = sorted(cmap, key=len, reverse=True)
fqn_re = re.compile(r'\b(net\.minecraftforge|slimeknights\.mantle)\.[A-Za-z0-9_.]*[A-Za-z0-9_]')

def map_fqn(m):
    s = m.group(0)
    for k in keys:
        if s == k or s.startswith(k + '.'):
            return cmap[k] + s[len(k):]
    return s

# ---- ResourceLocation constructor ----
def split_args(src, start):
    """src[start] is '('; returns (list of arg strings, index after ')') or None"""
    depth, i, args, cur = 0, start, [], start + 1
    while i < len(src):
        ch = src[i]
        if ch in '"\'':
            q = ch; i += 1
            while src[i] != q:
                i += 2 if src[i] == '\\' else 1
        elif ch in '([{': depth += 1
        elif ch in ')]}':
            depth -= 1
            if depth == 0:
                args.append(src[cur:i]); return args, i + 1
        elif ch == ',' and depth == 1:
            args.append(src[cur:i]); cur = i + 1
        elif src.startswith('//', i):
            i = src.index('\n', i)
        i += 1
    return None

def fix_rl(src):
    out, pos, n = [], 0, 0
    for m in re.finditer(r'new ResourceLocation\s*\(', src):
        if m.start() < pos: continue
        r = split_args(src, m.end() - 1)
        if not r: continue
        args, end = r
        args = [a for a in args]
        if len(args) == 2:
            rep = 'ResourceLocation.fromNamespaceAndPath(' + ','.join(args) + ')'
        elif len(args) == 1 and args[0].strip():
            rep = 'ResourceLocation.parse(' + args[0] + ')'
        else:
            continue
        out.append(src[pos:m.start()]); out.append(rep); pos = end; n += 1
    out.append(src[pos:])
    return ''.join(out), n

stats = collections.Counter()
for root in ROOTS:
    for dp, _, fs in os.walk(root):
        for f in fs:
            if not f.endswith('.java'): continue
            p = os.path.join(dp, f)
            src = open(p, encoding='utf-8').read()
            orig = src
            # renamed identifiers, only where the old forge class was referenced
            for old, new in id_renames.items():
                if re.search(r'net\.minecraftforge\.[a-z.]*\.' + old + r'\b', src):
                    src = re.sub(r'(?<![.\w])' + old + r'\b', new, src)
                    # the import line itself still holds the old FQN tail, fixed by the class map below
                    src = re.sub(r'(net\.minecraftforge\.[a-z.]*\.)' + new + r'\b', r'\g<1>' + old, src)
                    stats['identifier renames'] += 1
            # EventBusSubscriber: Mod.EventBusSubscriber -> EventBusSubscriber, Bus.FORGE -> Bus.GAME
            if 'Mod.EventBusSubscriber' in src or 'fml.common.Mod.EventBusSubscriber' in src:
                had_import = re.search(r'^import net\.minecraftforge\.fml\.common\.Mod\.EventBusSubscriber;', src, re.M)
                src = re.sub(r'(?<![.\w])Mod\.EventBusSubscriber\b', 'EventBusSubscriber', src)
                if not had_import:
                    src = re.sub(r'^(import net\.minecraftforge\.fml\.common\.Mod;)', r'\1\nimport net.minecraftforge.fml.common.Mod.EventBusSubscriber;', src, count=1, flags=re.M)
                stats['event bus subscriber files'] += 1
            if 'EventBusSubscriber' in src:
                src = re.sub(r'\bBus\.FORGE\b', 'Bus.GAME', src)
            # package and class moves
            src = fqn_re.sub(map_fqn, src)
            # ResourceLocation constructors
            src, n = fix_rl(src)
            stats['ResourceLocation constructors'] += n
            # unused import of Mod after the subscriber change
            if re.search(r'^import net\.neoforged\.fml\.common\.Mod;', src, re.M) and not re.search(r'@Mod\b|\bMod\.', src.split('\n', 1)[1] if False else re.sub(r'^import .*$', '', src, flags=re.M)):
                src = re.sub(r'^import net\.neoforged\.fml\.common\.Mod;\n', '', src, flags=re.M)
            # dedupe identical imports created by the rewrite
            seen, lines = set(), []
            for l in src.split('\n'):
                if l.startswith('import '):
                    if l in seen: continue
                    seen.add(l)
                lines.append(l)
            src = '\n'.join(lines)
            if src != orig:
                open(p, 'w', encoding='utf-8').write(src); stats['files changed'] += 1
print(dict(stats))
json.dump(cmap, open(f'{IDX}/applied_class_map.json', 'w'), indent=1, sort_keys=True)
