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
