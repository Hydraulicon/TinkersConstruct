"""Converts Mantle's IMultiRecipe<T> to MultiNamedRecipe<T>.

The old `List<T> getRecipes(RegistryAccess)` becomes `List<T> getDisplayRecipes(HolderLookup.Provider)`, and each class declaring
the interface gains a `getRecipes(ResourceLocation, HolderLookup.Provider)` naming those recipes via MultiRecipeHelper.
Usage: python3 -I multi_recipe.py <java files>..."""
import os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from errfix import add_import

WRAPPER = '''
  /** Named display recipes, cached from {{@link #getDisplayRecipes(HolderLookup.Provider)}} */
  private List<NamedRecipe<{t}>> namedRecipes = null;

  @Override
  public List<NamedRecipe<{t}>> getRecipes(ResourceLocation id, HolderLookup.Provider access) {{
    if (namedRecipes == null) {{
      namedRecipes = MultiRecipeHelper.name(id, getDisplayRecipes(access));
    }}
    return namedRecipes;
  }}
'''

def convert(path):
    s = open(path, encoding='utf-8').read()
    o = s
    m = re.search(r'implements [^{]*\bIMultiRecipe<([\w.]+)>', s)
    s = re.sub(r'\bIMultiRecipe<', 'MultiNamedRecipe<', s)
    s = re.sub(r'(@Override\s+)?public (List<[\w.?]+(?: extends [\w.]+)?>) getRecipes\(RegistryAccess (\w+)\)',
               lambda g: f'/** Gets the display recipes before naming */\n  public {g.group(2)} getDisplayRecipes(HolderLookup.Provider {g.group(3)})', s)
    s = re.sub(r'\bsuper\.getRecipes\((\w+)\)', r'super.getDisplayRecipes(\1)', s)
    if m:
        # insert wrapper before the final closing brace of the top level class
        end = s.rstrip().rfind('}')
        s = s[:end] + WRAPPER.format(t=m.group(1)) + s[end:]
    if s != o:
        lines = s.split('\n')
        lines = [l for l in lines if 'slimeknights.mantle.recipe.IMultiRecipe;' not in l]
        for imp in ('slimeknights.mantle.recipe.MultiNamedRecipe', 'slimeknights.mantle.recipe.NamedRecipe', 'net.minecraft.core.HolderLookup',
                    'net.minecraft.resources.ResourceLocation', 'java.util.List', 'slimeknights.tconstruct.library.recipe.MultiRecipeHelper'):
            if imp.rsplit('.', 1)[1] in '\n'.join(lines) and not any(l == f'import {imp};' for l in lines):
                add_import(lines, imp)
        s = '\n'.join(lines)
        open(path, 'w', encoding='utf-8').write(s)
    return s != o

if __name__ == '__main__':
    print(sum(1 for p in sys.argv[1:] if convert(p)), 'files changed')
