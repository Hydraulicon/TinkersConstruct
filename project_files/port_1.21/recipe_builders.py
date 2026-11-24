"""Updates datagen recipe builders to the 1.21 RecipeOutput API.

 - Consumer<FinishedRecipe> -> RecipeOutput
 - ResourceLocation adv = buildOptionalAdvancement(id, folder) -> AdvancementHolder adv = buildOptionalAdvancement(output, id, folder) (same for buildAdvancement)
 - output.accept(new LoadableFinishedRecipe<>(recipe, LOADER, adv)) -> output.accept(id, recipe, adv)
 - drops the id argument from recipe constructor calls, as recipes no longer store their ID
Usage: python3 -I recipe_builders.py <java files>..."""
import os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from errfix import add_import, split_call_args


def enclosing_output(s, pos):
    """Finds the name of the RecipeOutput parameter of the method containing pos"""
    for m in reversed(list(re.finditer(r'RecipeOutput (\w+)[,)]', s[:pos]))):
        return m.group(1)
    return None


def convert(path):
    s = open(path, encoding='utf-8').read()
    o = s
    s = re.sub(r'Consumer<FinishedRecipe> (\w+)', r'RecipeOutput \1', s)
    # advancements need the output
    def adv(m):
        output = enclosing_output(s, m.start()) or 'output'
        return f'AdvancementHolder {m.group(1)} = {m.group(2) or ""}{m.group(3)}({output}, '
    s = re.sub(r'ResourceLocation (\w+) = (this\.|super\.)?(buildOptionalAdvancement|buildAdvancement)\(', adv, s)
    # unwrap finished recipes
    out = []
    pos = 0
    for m in re.finditer(r'(\w+)\.accept\(new LoadableFinishedRecipe<>\(', s):
        if m.start() < pos:
            continue
        outer_open = m.start() + len(m.group(1)) + len('.accept')
        outer_args, outer_end = split_call_args(s, outer_open)
        inner_open = m.end() - 1
        inner_args, inner_end = split_call_args(s, inner_open)
        if inner_args is None or outer_args is None or len(inner_args) < 2:
            continue
        recipe = inner_args[0].strip()
        advancement = inner_args[2].strip() if len(inner_args) > 2 else 'null'
        id_name = 'id'
        out.append(s[pos:m.start()])
        out.append(f'{m.group(1)}.accept({id_name}, {recipe}, {advancement})')
        pos = outer_end
    out.append(s[pos:])
    s = ''.join(out)
    # recipe constructors no longer take the id
    for m in reversed(list(re.finditer(r'new \w+(?:Recipe|Fuel|Salvage|Recycle|Swapping|SwapCasting)\w*(?:<[^>]*>)?\(', s))):
        args, end = split_call_args(s, m.end() - 1)
        if args and any(a.strip() == 'id' for a in args):
            kept = [a for a in args if a.strip() != 'id']
            text = ','.join(kept).lstrip()
            s = s[:m.end()] + text + s[end - 1:]
    if s != o:
        lines = s.split('\n')
        if 'RecipeOutput' in s: add_import(lines, 'net.minecraft.data.recipes.RecipeOutput')
        if 'AdvancementHolder' in s: add_import(lines, 'net.minecraft.advancements.AdvancementHolder')
        s = '\n'.join(lines)
        s = re.sub(r'^(import|// TODO 1\.21 import:) (net\.minecraft\.data\.recipes\.FinishedRecipe|slimeknights\.mantle\.recipe\.data\.LoadableFinishedRecipe|slimeknights\.mantle\.recipe\.helper\.LoadableFinishedRecipe);\n', '', s, flags=re.M)
        if 'Consumer<' not in s and 'Consumer;' in s:
            s = re.sub(r'^import java\.util\.function\.Consumer;\n', '', s, flags=re.M)
        open(path, 'w', encoding='utf-8').write(s)
    return s != o


if __name__ == '__main__':
    n = sum(1 for p in sys.argv[1:] if convert(p))
    print(n, 'files changed')
