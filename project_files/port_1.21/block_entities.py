"""Updates block entity save/load overrides to the 1.21 signatures taking a HolderLookup.Provider.
Only touches @Override methods in classes that extend a block entity class.
Usage: python3 -I block_entities.py <java source root>"""
import os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from errfix import add_import

P = 'HolderLookup.Provider registries'
RULES = [
    (r'@Override\n(\s+)public void load\(CompoundTag (\w+)\) \{', r'@Override\n\1protected void loadAdditional(CompoundTag \2, ' + P + ') {', r'super\.load\((\w+)\)', r'super.loadAdditional(\1, registries)'),
    (r'@Override\n(\s+)(public|protected) void saveAdditional\(CompoundTag (\w+)\) \{', r'@Override\n\1\2 void saveAdditional(CompoundTag \3, ' + P + ') {', r'super\.saveAdditional\((\w+)\)', r'super.saveAdditional(\1, registries)'),
    (r'@Override\n(\s+)protected void saveSynced\(CompoundTag (\w+)\) \{', r'@Override\n\1protected void saveSynced(CompoundTag \2, ' + P + ') {', r'super\.saveSynced\((\w+)\)', r'super.saveSynced(\1, registries)'),
    (r'@Override\n(\s+)public CompoundTag getUpdateTag\(\) \{', r'@Override\n\1public CompoundTag getUpdateTag(' + P + ') {', r'super\.getUpdateTag\(\)', r'super.getUpdateTag(registries)'),
    (r'@Override\n(\s+)public void handleUpdateTag\(CompoundTag (\w+)\) \{', r'@Override\n\1public void handleUpdateTag(CompoundTag \2, ' + P + ') {', r'super\.handleUpdateTag\((\w+)\)', r'super.handleUpdateTag(\1, registries)'),
]


def method_body(s, start):
    """returns end index of the method whose body starts at the '{' found after start"""
    i = s.index('{', start)
    depth = 0
    for k in range(i, len(s)):
        if s[k] == '{': depth += 1
        elif s[k] == '}':
            depth -= 1
            if depth == 0: return k + 1
    return len(s)


def convert(s):
    n = 0
    for decl, rep, sup, suprep in RULES:
        pos = 0
        while True:
            m = re.compile(decl).search(s, pos)
            if not m: break
            new = m.expand(rep)
            end = method_body(s, m.start())
            body = s[m.end():end]
            body = re.sub(sup, suprep, body)
            # calls to the no argument save helpers inside the method
            body = re.sub(r'\bsaveWithoutMetadata\(\)', 'saveWithoutMetadata(registries)', body)
            s = s[:m.start()] + new + body + s[end:]
            pos = m.start() + len(new)
            n += 1
    return s, n


if __name__ == '__main__':
    total = 0
    for dp, _, fs in os.walk(sys.argv[1]):
        for f in fs:
            if not f.endswith('.java'): continue
            p = os.path.join(dp, f)
            s = open(p, encoding='utf-8').read()
            if not re.search(r'extends \w*(BlockEntity|TileEntity)\b', s): continue
            s, n = convert(s)
            if n:
                lines = s.split('\n'); add_import(lines, 'net.minecraft.core.HolderLookup')
                open(p, 'w', encoding='utf-8').write('\n'.join(lines))
                total += n
    print(total, 'block entity methods updated')
