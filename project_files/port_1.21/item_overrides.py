"""Applies the standard 1.21 item override changes to Tinkers item classes (see MANTLE_API.md, Tools and items).
Usage: python3 -I item_overrides.py <java files>..."""
import re, sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from errfix import add_import

REPLACEMENTS = [
    # enchanting table: only curses
    (r'  @Override\n  public boolean canApplyAtEnchantingTable\(ItemStack (\w+), Enchantment (\w+)\) \{\n    return \2\.isCurse\(\) && super\.canApplyAtEnchantingTable\(\1, \2\);\n  \}',
     '  @Override\n  public boolean supportsEnchantment(ItemStack \\1, Holder<Enchantment> \\2) {\n    // only curses can be applied directly, everything else is a modifier\n    return \\2.is(EnchantmentTags.CURSE) && super.supportsEnchantment(\\1, \\2);\n  }\n\n'
     '  @Override\n  public boolean isPrimaryItemFor(ItemStack \\1, Holder<Enchantment> \\2) {\n    return \\2.is(EnchantmentTags.CURSE) && super.isPrimaryItemFor(\\1, \\2);\n  }'),
    (r'public int getEnchantmentLevel\(ItemStack (\w+), Enchantment (\w+)\)', r'public int getEnchantmentLevel(ItemStack \1, Holder<Enchantment> \2)'),
    (r'public Map<Enchantment,\s*Integer> getAllEnchantments\(ItemStack (\w+)\) \{\n    return EnchantmentModifierHook\.getAllEnchantments\(\1\);',
     r'public ItemEnchantments getAllEnchantments(ItemStack \1, HolderLookup.RegistryLookup<Enchantment> lookup) {\n    return EnchantmentModifierHook.getAllEnchantments(\1, lookup);'),
    # loading
    (r'public void verifyTagAfterLoad\(CompoundTag (\w+)\) \{\n    ToolStack\.verifyTag\(this, \1, getToolDefinition\(\)\);',
     r'public void verifyComponentsAfterLoad(ItemStack stack) {\n    ToolStack.verifyStack(stack, getToolDefinition());'),
    # rarity is a component, mirrored by ToolStack
    (r'\n  @Override\n  public Rarity getRarity\(ItemStack \w+\) \{\n    return RarityModule\.getRarity\(\w+\);\n  \}\n', '\n'),
    # item entity destroyed
    (r'(  @Deprecated\n)?  @Override\n  public void onDestroyed\(ItemEntity (\w+)\) \{', r'  @Override\n  public void onDestroyed(ItemEntity \2, DamageSource damageSource) {'),
    # durability
    (r'public <T extends LivingEntity> int damageItem\(ItemStack (\w+), int (\w+), T (\w+), Consumer<T> (\w+)\)',
     r'public <T extends LivingEntity> int damageItem(ItemStack \1, int \2, @Nullable T \3, Consumer<Item> \4)'),
    (r'(\w+)\.accept\(damager\);', r'\1.accept(stack.getItem());'),
    # use duration
    (r'public int getUseDuration\(ItemStack (\w+)\) \{', r'public int getUseDuration(ItemStack \1, LivingEntity entity) {'),
]


def convert(path):
    s = open(path, encoding='utf-8').read()
    o = s
    for rx, rep in REPLACEMENTS:
        s = re.sub(rx, rep, s)
    if s != o:
        lines = s.split('\n')
        for imp, cond in [('net.minecraft.core.Holder', 'Holder<'), ('net.minecraft.core.HolderLookup', 'HolderLookup.'),
                          ('net.minecraft.tags.EnchantmentTags', 'EnchantmentTags.'), ('net.minecraft.world.item.enchantment.ItemEnchantments', 'ItemEnchantments'),
                          ('net.minecraft.world.damagesource.DamageSource', 'DamageSource '), ('net.minecraft.world.item.Item', 'Consumer<Item>'),
                          ('javax.annotation.Nullable', '@Nullable')]:
            if cond in s: add_import(lines, imp)
        open(path, 'w', encoding='utf-8').write('\n'.join(lines))
    return s != o


if __name__ == '__main__':
    for p in sys.argv[1:]:
        print(('changed ' if convert(p) else 'same    ') + p)
