"""Applies targeted fixes at javac error locations. Used during the 1.21.1 port (see MIGRATION_1.21.md).

Usage:
  python3 -I errfix.py <compile log> list [regex on message|symbol|location]
  python3 -I errfix.py <compile log> rename-method <old> <new> <location regex>
      Renames calls to method <old> where javac could not find it, and the error location type matches the regex.
  python3 -I errfix.py <compile log> stats
"""
import re, sys, collections

ERR = re.compile(r'^(/\S+\.java):(\d+): error: (.*)$')


def parse(log):
    lines = open(log, encoding='utf-8', errors='replace').read().split('\n')
    out = []
    i = 0
    while i < len(lines):
        m = ERR.match(lines[i])
        if not m:
            i += 1; continue
        e = {'file': m.group(1), 'line': int(m.group(2)), 'msg': m.group(3), 'symbol': '', 'location': '', 'col': None}
        j = i + 1
        # source line, then caret line
        if j + 1 < len(lines) and lines[j + 1].strip() == '^':
            e['col'] = lines[j + 1].index('^')
            j += 2
        while j < len(lines) and not ERR.match(lines[j]) and lines[j].startswith('  '):
            s = lines[j].strip()
            if s.startswith('symbol:'): e['symbol'] = re.sub(r'\s+', ' ', s[7:].strip())
            elif s.startswith('location:'): e['location'] = re.sub(r'\s+', ' ', s[9:].strip())
            j += 1
        out.append(e); i = j
    # javac sometimes reports the same error twice
    seen, uniq = set(), []
    for e in out:
        k = (e['file'], e['line'], e['col'], e['msg'], e['symbol'])
        if k not in seen:
            seen.add(k); uniq.append(e)
    return uniq


def location_type(loc):
    """'variable x of type Foo<Bar>' -> 'Foo<Bar>', 'class Foo' -> 'Foo'"""
    m = re.search(r'of type (.*)$', loc)
    if m: return m.group(1)
    m = re.match(r'(?:class|interface|type|enum|record) (.*)$', loc)
    return m.group(1) if m else loc


def rename_method(errs, old, new, loc_re):
    edits = collections.defaultdict(list)
    for e in errs:
        if e['msg'] == 'cannot find symbol' and re.match(r'method ' + re.escape(old) + r'\(', e['symbol']) \
                and re.search(loc_re, location_type(e['location'])) and e['col'] is not None:
            edits[e['file']].append((e['line'], e['col']))
    n = 0
    for f, pos in edits.items():
        src = open(f, encoding='utf-8').read().split('\n')
        for line, col in sorted(set(pos), reverse=True):
            s = src[line - 1]
            # javac points at the '.' before the method name or at the name itself
            k = s.find(old, col)
            if k == -1 or k > col + 2: continue
            src[line - 1] = s[:k] + new + s[k + len(old):]
            n += 1
        open(f, 'w', encoding='utf-8').write('\n'.join(src))
    print(f'{n} call sites renamed in {len(edits)} files')


if __name__ == '__main__':
    errs = parse(sys.argv[1])
    cmd = sys.argv[2]
    if cmd == 'stats':
        print(len(errs), 'unique errors')
        c = collections.Counter()
        for e in errs:
            c[e['msg'] + ('  ' + e['symbol'] if e['symbol'] else '')] += 1
        for k, v in c.most_common(60): print(f'{v:6} {k}')
    elif cmd == 'list':
        rx = re.compile(sys.argv[3] if len(sys.argv) > 3 else '')
        for e in errs:
            if rx.search(e['msg'] + '|' + e['symbol'] + '|' + e['location']):
                print(f"{e['file']}:{e['line']}:{e['col']} {e['msg']} | {e['symbol']} | {e['location']}")
    elif cmd == 'rename-method':
        rename_method(errs, sys.argv[3], sys.argv[4], sys.argv[5])


def stub_imports(errs):
    """Replaces imports javac cannot resolve with a marker comment.
    javac skips annotation processing (Lombok) while any import fails to resolve, which floods the log with errors
    about missing generated getters. The marker keeps the old import for whoever ports the code using it."""
    edits = collections.defaultdict(set)
    for e in errs:
        edits[e['file']].add(e['line'])
    n = 0
    for f, lines in edits.items():
        src = open(f, encoding='utf-8').read().split('\n')
        for line in lines:
            s = src[line - 1]
            if s.startswith('import '):
                src[line - 1] = '// TODO 1.21 import: ' + s[len('import '):]
                n += 1
        open(f, 'w', encoding='utf-8').write('\n'.join(src))
    print(f'{n} imports stubbed')


if __name__ == '__main__' and sys.argv[2] == 'stub-imports':
    stub_imports(parse(sys.argv[1]))


def add_import(src, fqn):
    """adds an import after the package line if missing. src is a list of lines"""
    imp = f'import {fqn};'
    if imp in src: return
    for i, l in enumerate(src):
        if l.startswith('import '):
            src.insert(i, imp); return
    for i, l in enumerate(src):
        if l.startswith('package '):
            src.insert(i + 1, ''); src.insert(i + 2, imp); return


def retype_deferred(errs):
    """ItemObject<Item,X> fields assigned from Mantle's item register become DeferredItem<X>, blocks become DeferredBlock<X>"""
    rx = re.compile(r'(DeferredItem|DeferredBlock)<\w*> (?:conforms to|cannot be converted to) ItemObject<')
    edits = collections.defaultdict(set)
    for e in errs:
        if rx.search(e['msg']): edits[e['file']].add(e['line'])
    n = 0
    for f, lines in edits.items():
        src = open(f, encoding='utf-8').read().split('\n')
        changed = set()
        for line in lines:
            # the declaration may start a few lines above the reported initializer
            for k in range(line - 1, max(line - 6, -1), -1):
                s = src[k]
                t = re.sub(r'\bItemObject<Item,\s*([^<>]*(?:<[^<>]*>)?)>', r'DeferredItem<\1>', s)
                t = re.sub(r'\bItemObject<Block,\s*([^<>]*(?:<[^<>]*>)?)>', r'DeferredBlock<\1>', t)
                if t != s:
                    src[k] = t; n += 1
                    changed.add('DeferredItem' if 'DeferredItem<' in t else 'DeferredBlock')
                    break
        for c in changed: add_import(src, 'net.neoforged.neoforge.registries.' + c)
        open(f, 'w', encoding='utf-8').write('\n'.join(src))
    print(f'{n} declarations retyped in {len(edits)} files')


if __name__ == '__main__' and sys.argv[2] == 'retype-deferred':
    retype_deferred(parse(sys.argv[1]))


def drop_get(errs, types):
    """For 'X cannot be converted to Holder<X>' errors where the argument is 'something.get()', drops the .get() as our DeferredHolders are holders"""
    rx = re.compile(r'incompatible types: (' + types + r') cannot be converted to Holder<\1>')
    edits = collections.defaultdict(set)
    for e in errs:
        if rx.search(e['msg']) and e['col'] is not None: edits[e['file']].add((e['line'], e['col']))
    n = 0
    for f, pos in edits.items():
        src = open(f, encoding='utf-8').read().split('\n')
        for line, col in sorted(pos, reverse=True):
            s = src[line - 1]
            # javac points at the '(' of the get() call
            if s[col - 4:col + 2] == '.get()':
                src[line - 1] = s[:col - 4] + s[col + 2:]; n += 1
        open(f, 'w', encoding='utf-8').write('\n'.join(src))
    print(f'{n} .get() calls dropped')


if __name__ == '__main__' and sys.argv[2] == 'drop-get':
    drop_get(parse(sys.argv[1]), sys.argv[3])


LOADABLE_OVERRIDES = [
    # Loadable#serialize(T) -> serialize(T, TypedMap)
    (re.compile(r'\bJsonElement serialize\(((?:final )?[\w.<>?, \[\]]+?) (\w+)\)'), r'JsonElement serialize(\1 \2, TypedMap context)'),
    # RecordLoadable/RecordField#serialize(T, JsonObject) -> serializeInto(T, JsonObject, TypedMap)
    (re.compile(r'\bvoid serialize\(((?:final )?[\w.<>?, \[\]]+?) (\w+), JsonObject (\w+)\)'), r'void serializeInto(\1 \2, JsonObject \3, TypedMap context)'),
    # Streamable#decode
    (re.compile(r'\bdecode\(FriendlyByteBuf (\w+), TypedMap (\w+)\)'), r'decode(RegistryFriendlyByteBuf \1, TypedMap \2)'),
    # Streamable#encode(buffer, value) -> encode(buffer, value, context)
    (re.compile(r'\bvoid encode\(FriendlyByteBuf (\w+), ((?:final )?[\w.<>?, \[\]]+?) (\w+)\)'), r'void encode(RegistryFriendlyByteBuf \1, \2 \3, TypedMap context)'),
    # StringLoadable#getString(T) -> getString(T, TypedMap)
    (re.compile(r'\bString getString\(((?:final )?[\w.<>?, \[\]]+?) (\w+)\)'), r'String getString(\1 \2, TypedMap context)'),
]


def loadable_overrides(errs):
    """Updates overrides of Mantle loadable methods to their 1.21 signatures, only where javac reports a broken override"""
    edits = collections.defaultdict(set)
    for e in errs:
        if e['msg'] == 'method does not override or implement a method from a supertype': edits[e['file']].add(e['line'])
    n = 0
    for f, lines in edits.items():
        src = open(f, encoding='utf-8').read().split('\n')
        changed = False
        for line in lines:
            for k in range(line - 1, min(line + 4, len(src))):
                s = src[k]
                if '(' not in s or s.strip().startswith('@'): continue
                for rx, rep in LOADABLE_OVERRIDES:
                    t = rx.sub(rep, s, count=1)
                    if t != s:
                        src[k] = t; n += 1; changed = True; break
                break
        if changed:
            text = '\n'.join(src)
            if 'TypedMap' in text: add_import(src, 'slimeknights.mantle.util.typed.TypedMap')
            if 'RegistryFriendlyByteBuf' in '\n'.join(src): add_import(src, 'net.minecraft.network.RegistryFriendlyByteBuf')
        open(f, 'w', encoding='utf-8').write('\n'.join(src))
    print(f'{n} loadable overrides updated')


if __name__ == '__main__' and sys.argv[2] == 'loadable-overrides':
    loadable_overrides(parse(sys.argv[1]))


def upgrade_buffers(errs):
    """FriendlyByteBuf -> RegistryFriendlyByteBuf for the declaration of a variable passed where a registry buffer is needed.
    Run repeatedly, each pass moves the change one caller further out."""
    edits = collections.defaultdict(set)
    for e in errs:
        if 'FriendlyByteBuf cannot be converted to RegistryFriendlyByteBuf' in e['msg'] and e['col'] is not None:
            edits[e['file']].add((e['line'], e['col']))
    n = 0
    for f, pos in edits.items():
        src = open(f, encoding='utf-8').read().split('\n')
        for line, col in pos:
            m = re.compile(r'\w+').match(src[line - 1], col)
            if not m: continue
            name = m.group(0)
            decl = re.compile(r'(?<![\w.])FriendlyByteBuf(\s+' + re.escape(name) + r'\b)')
            for k in range(line - 1, -1, -1):
                if decl.search(src[k]):
                    src[k] = decl.sub(r'RegistryFriendlyByteBuf\1', src[k], count=1); n += 1; break
        if any('RegistryFriendlyByteBuf' in l for l in src): add_import(src, 'net.minecraft.network.RegistryFriendlyByteBuf')
        open(f, 'w', encoding='utf-8').write('\n'.join(src))
    print(f'{n} buffer declarations upgraded')


if __name__ == '__main__' and sys.argv[2] == 'upgrade-buffers':
    upgrade_buffers(parse(sys.argv[1]))
