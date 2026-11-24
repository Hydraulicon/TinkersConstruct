"""Removes recipe IDs from recipe classes and replaces typed serializers for the 1.21 port.

In 1.21 recipes no longer know their ID, it is stored on the RecipeHolder; Mantle no longer provides the ID while parsing.
Mantle also removed TypeAwareRecipeSerializer in favor of separate type and serializer context fields.

Usage: python3 -I recipe_ids.py <java files>...
Changes made, per file:
 - drops ContextKey.ID.requiredField() from record loaders
 - drops the ResourceLocation id constructor parameter, the id field, this.id = id assignments, getId() overrides, and id super arguments
 - LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField() becomes the TYPE and SERIALIZER fields, with matching constructor parameters
Remaining uses of the id are left for the compiler to report."""
import os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from errfix import add_import, split_call_args


def remove_arg(src, start, pred):
    """src[start] == '('; removes the arguments matching pred from the call or declaration"""
    args, end = split_call_args(src, start)
    if args is None:
        return src
    kept = [a for a in args if not pred(a.strip())]
    if len(kept) == len(args):
        return src
    # keep the original formatting of the first kept argument
    text = ','.join(kept)
    if kept and not kept[0].startswith('\n'):
        text = text.lstrip()
    return src[:start + 1] + text + src[end - 1:]


def convert(path):
    s = open(path, encoding='utf-8').read()
    o = s
    name = os.path.basename(path)[:-5]
    # loader fields
    # keep line breaks: drop the field and the following spaces on the same line, or the whole line if it held only the field
    s = re.sub(r'\n[ \t]*ContextKey\.ID\.requiredField\(\),[ \t]*\n', '\n', s)
    s = re.sub(r'ContextKey\.ID\.requiredField\(\),[ \t]*', '', s)
    s = s.replace('LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField()', 'LoadableRecipeSerializer.TYPE.requiredField(), LoadableRecipeSerializer.SERIALIZER.requiredField()')
    # constructors of this class (and nested classes, which are rare)
    for m in reversed(list(re.finditer(r'(?:public|protected|private)\s+' + re.escape(name) + r'\s*\(', s))):
        s = remove_arg(s, m.end() - 1, lambda a: re.fullmatch(r'(@\w+\s+)?ResourceLocation id', a) is not None)
        s = re.sub(r'(public|protected|private)(\s+' + re.escape(name) + r'\s*\()TypeAwareRecipeSerializer<\?> serializer', r'\1\2RecipeType<?> type, RecipeSerializer<?> serializer', s)
    # super calls passing the id
    for m in reversed(list(re.finditer(r'\b(super|this)\(', s))):
        s = remove_arg(s, m.end() - 1, lambda a: a == 'id')
    s = s.replace('serializer.getType()', 'type')
    s = re.sub(r'\n[ \t]*this\.id = id;', '', s)
    s = re.sub(r'\n([ \t]*@Getter\n)?[ \t]*(private|protected) final ResourceLocation id;', '', s)
    s = re.sub(r'\n[ \t]*@Override\n[ \t]*public ResourceLocation getId\(\) \{\n[ \t]*return id;\n[ \t]*\}\n', '\n', s)
    s = re.sub(r'(private|protected) final TypeAwareRecipeSerializer<\?> serializer;', r'\1 final RecipeSerializer<?> serializer;', s)
    if s != o:
        lines = s.split('\n')
        if 'RecipeType<' in s: add_import(lines, 'net.minecraft.world.item.crafting.RecipeType')
        if 'RecipeSerializer<' in s: add_import(lines, 'net.minecraft.world.item.crafting.RecipeSerializer')
        s = '\n'.join(lines)
        s = re.sub(r'^(import|// TODO 1\.21 import:) slimeknights\.mantle\.recipe\.helper\.TypeAwareRecipeSerializer;\n', '', s, flags=re.M)
        open(path, 'w', encoding='utf-8').write(s)
    return s != o


if __name__ == '__main__':
    n = 0
    for p in sys.argv[1:]:
        if convert(p):
            n += 1
    print(n, 'files changed')
