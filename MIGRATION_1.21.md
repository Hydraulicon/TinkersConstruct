# Port plan: Minecraft 1.20.1 (Forge) → 1.21.1 (NeoForge)

Status: **Phases 0 and 1 complete; phase 2 in progress.** The tool-data approach (two steps, below) is approved.

## Workspace setup (during the port)
```
git clone -b 1.21 https://github.com/SlimeKnights/Mantle ../Mantle
(cd ../Mantle && git checkout f631160 && ./gradlew build -x test)
./gradlew compileJava
```
The Mantle jar is resolved from `mantle_libs` (default `../Mantle/build/libs`) in `gradle.properties`.
When bumping the pinned commit, update `mantle_version` to match the built jar name.

## Decisions

| Topic | Decision |
|---|---|
| Target | Minecraft 1.21.1, NeoForge 21.1.x (matching Mantle's `1.21` branch: NeoForge 21.1.265) |
| Build plugin | NeoForged ModDevGradle (replaces ForgeGradle 6 and the Parchment librarian plugin) |
| Java | 21 (required by 1.21) |
| Mantle | Built from source from upstream `SlimeKnights/Mantle` branch `1.21` and consumed from `../Mantle/build/libs` (the existing `flatDir` hook). Pin a specific Mantle commit per phase. |
| Branch | All work on the dev branch. `1.20.1` stays as the stable line. |

### Risk: Mantle is a moving target
Mantle's `1.21` branch is itself still being ported. Its latest commits (Nov 2026) are still renaming
data-component and serializer APIs. About 980 of our 1,940 source files import Mantle, so every
Mantle API change ripples through. Mitigation: pin a Mantle commit, and bump it only at phase boundaries.
If upstream Tinkers publishes a 1.21 branch at some point, re-evaluate. Rebasing onto it may be cheaper than continuing.

**Verified:** Mantle `1.21` @ `f631160` builds cleanly in this environment (`./gradlew build`, 3m21s, NeoForge maven reachable),
producing `Mantle-1.21.1-1.12.DEV.f631160.jar`. Mantle's own maven (dvs1.progwml6.com) returned 502, so building from source is required for now.

## Scale (measured on current `1.20.1` head)

~199k lines of Java across ~1,940 main files and 66 test files, plus ~7,100 generated JSON files.

| Breaking area in 1.21 / NeoForge | Files affected |
|---|---|
| `net.minecraftforge.*` imports → `net.neoforged.*` | 569 |
| Fluid API (`FluidStack`, `IFluidHandler`) | 207 |
| `CompoundTag` / NBT (much of this moves to data components) | 183 |
| Recipes (`Recipe<Container>` → `RecipeInput`, `MapCodec` + `StreamCodec` serializers) | 157 |
| ItemStack NBT (`getTag` / `getOrCreateTag` / `setTag`) | 124 |
| `FriendlyByteBuf` → `RegistryFriendlyByteBuf` / `StreamCodec` | 99 |
| Capabilities (`LazyOptional` / `getCapability`) → NeoForge capability rework | 89 |
| `new ResourceLocation(...)` → `ResourceLocation.fromNamespaceAndPath` / `parse` | 51 |
| Rendering (`PoseStack`, vertex API changes) | 51 |
| Enchantments (now data-driven registry) | 37 |
| Networking (`SimpleChannel`) → `CustomPacketPayload` + `RegisterPayloadHandlersEvent` | 33 |
| `@SubscribeEvent` (event class and bus changes) | 34 |
| Worldgen / structures | 31 |
| `DeferredRegister` / `RegistryObject` → `DeferredHolder` | 31 |
| Loot functions, conditions and GLMs (codec-based) | 30 |
| `AttributeModifier` (UUID → `ResourceLocation` id) | 27 |
| `ForgeRegistries` → `BuiltInRegistries` / `Registries` | 20 |
| `ArmorMaterial` (now a registry entry) | 18 |
| Tool data model (`ToolStack` / `IToolStackView` users) | 505 |

Data-pack folders were renamed to singular in 1.21 (`recipes`→`recipe`, `loot_tables`→`loot_table`,
`advancements`→`advancement`, `tags/items`→`tags/item`, `tags/blocks`→`tags/block`, `functions`→`function`, …).
`forge:` conditions and tags become `neoforge:` / `c:`. Most of this is fixed by re-running datagen once the
code compiles. Hand-written files in `src/main/resources/data` must be moved manually.

## The central design question: tool data storage

Tools store everything (materials, modifiers, stats, persistent data) in ItemStack NBT, accessed through
`ToolStack`, `ModDataNBT`, `MaterialNBT`, `ModifierNBT`, `StatsNBT` and related classes in `library/tools/nbt`. 1.21 removes stack NBT.
Data components must be immutable and need both a `Codec` and a `StreamCodec`.

**Recommended approach: two steps.**
1. **Compatibility step (this port):** Register one `tconstruct:tool_data` data component that holds an
   immutable `CompoundTag` snapshot (or a small record of tags). `ToolStack` keeps its mutable working copy and
   writes back a fresh component on `updateStack()`. This keeps the `ToolStack` API, which 505 files depend on,
   largely unchanged. Most NBT work stays inside `library/tools/nbt`.
2. **Later (separate effort):** Split this into proper typed components (`materials`, `modifiers`, `stats`, `persistent_data`) for
   better syncing, item comparison and tooltip performance.

Non-tool stack NBT (slime sling, retextured blocks, tanks, casts, potion fluids, and similar) is converted directly to
components, using Mantle's new `DataComponentTypeDeferredRegister` / `DataComponentHelper`.

Old worlds: Vanilla's data fixer does not know our NBT. A 1.20.1 → 1.21.1 world upgrade needs a
fixer that moves `tag.*` tool keys into the new component. This is optional but should be decided before release. Findings so far:
- Vanilla's 1.21 item fixer moves unknown root stack NBT into `minecraft:custom_data`. So a tool from a 1.20 world arrives with its
  Tinkers keys in `custom_data`, and could be migrated into `tconstruct:tool_data` in `Item#verifyComponentsAfterLoad`.
- Stacks stored inside tool NBT (tool inventories, and fluids in tool tanks) use the 1.20 stack format (`Count`, `tag`) and are not
  fixed by vanilla. Reading them needs a legacy fallback, or the inventory contents are lost.
- Block positions in block entity NBT already read both formats (`TagUtil.readPos`).

## Phases

Each phase ends with a commit. From phase 2 on, it also ends with a compiling (or compiling-so-far) state, where practical.

### Phase 0: Toolchain and dependencies
- Build Mantle `1.21` locally and record the pinned commit.
- Replace `build.gradle` with a ModDevGradle setup modelled on Mantle's (Java 21, Parchment 1.21.1, NeoForge 21.1.265).
- Rename `META-INF/mods.toml` → `neoforge.mods.toml` and update dependency entries (`neoforge`, `mantle`, JEI 19.x).
- Migrate `accesstransformer.cfg` (198 lines). This means re-mapping names for 1.21.1 and dropping obsolete entries.
- Temporarily exclude optional compat (`plugin/`: JEI, IE, JSON Things, Diet, CraftingTweaks) from compilation.
- **Exit:** Gradle configures and decompiles. Compilation fails with a long list of errors, which is expected.
- **Done.** Notes:
  - Gradle 9.2.1 wrapper, ModDevGradle 2.0.141 and NeoForge 21.1.265, all matching Mantle.
  - The AT was converted from SRG to Mojang names using MCPConfig 1.20.1 and the official mappings.
    The comment names were unreliable because many were old MCP names. 11 entries were removed that were already dead in 1.20.1.
    26 entries whose targets were removed or changed in 1.21.1 are kept, commented out, under `TODO 1.21` at the bottom of the file.
    Resolve each one as the code using it is ported.
  - `plugin/**` is excluded from compilation, and the compat hooks in `TConstruct` are commented out.
    JEI 19.64 stays on the classpath.
  - Baseline: 25,980 javac errors, mostly unresolved `net.minecraftforge` symbols. javac reports in stages,
    so the count will rise once imports resolve and type checking begins.
  - Not yet updated: the `Jenkinsfile` (still uses `JDK_17`; it needs a JDK 21 on the CI agent) and the test setup (phase 6).

### Phase 1: Mechanical renames (scripted)
- `net.minecraftforge` → `net.neoforged` package moves (with the per-class mapping table, since not everything moved 1:1).
- `new ResourceLocation(a, b)` / `(s)` → `ResourceLocation.fromNamespaceAndPath` / `parse`.
- `RegistryObject` → `DeferredHolder`, `ForgeRegistries` → `BuiltInRegistries`, `ForgeConfigSpec` → `ModConfigSpec`.
- Event renames (`TickEvent.*` → split `*TickEvent.Pre/Post`, `LivingHurtEvent` → `LivingIncomingDamageEvent`, and others).
- **Exit:** The error count is dominated by real API changes, not renames.
- **Done.** The scripts are in `project_files/port_1.21/` (`build_index.sh`, `mapimports.py`, `rewrite.py`). Results:
  - 548 files changed. Forge imports were mapped by direct package move (140), by unique class name in NeoForge (17)
    or by a hand-written rename table (17: `MinecraftForge`→`NeoForge`, `ForgeConfigSpec`→`ModConfigSpec`,
    `ToolAction(s)`→`ItemAbility`/`ItemAbilities`, `ForgeHooks`→`CommonHooks`, `ForgeEventFactory`→`EventHooks`, …).
    Identifiers were renamed only in files that imported the old class.
  - 17 Mantle classes that moved packages in Mantle 1.21 had their imports rewritten.
  - `@Mod.EventBusSubscriber` → `@EventBusSubscriber` (now top level), with `Bus.FORGE` → `Bus.GAME`. `@Cancelable` was removed from our
    teleport events, since their NeoForge parent already implements `ICancellableEvent`.
  - 116 `new ResourceLocation(...)` → `ResourceLocation.fromNamespaceAndPath` / `parse`.
  - `AttributeModifier.Operation` constants (`ADDITION`→`ADD_VALUE`, …) and `FrameType`→`AdvancementType`, applied only
    where the vanilla class is imported. Tinkers has its own `FrameType` and `StatOperation`, which were left alone.
  - Errors: 25,980 → 17,014.
- **Left on purpose for phases 2–3** (they need redesign, not renaming): 38 Forge classes with no NeoForge
  counterpart (`LazyOptional`, `Capability`/`ForgeCapabilities`, `NetworkEvent`, `ForgeRegistries`, `RegistryObject`,
  `FMLJavaModLoadingContext`, `DistExecutor`, `TickEvent`, `LivingHurtEvent`, `TierSortingRegistry`, recipe conditions/serializers, …)
  and 34 Mantle classes removed in 1.21 (`FinishedRecipe` helpers, `IRecipeContainer`, `NetworkWrapper`,
  `ElementScreen`, Mantle `FluidIngredient`/`SizedIngredient` → NeoForge `FluidIngredient`/`SizedFluidIngredient`, …).
  - Data to watch in phase 5: datapack JSON that names attribute operations (`addition`, …) must use the 1.21 names.

### Phase 2: `library` core
Order: `library/utils`, `json`, `data` → `materials` → `modifiers` → `tools` (incl. tool data component) → `recipe` → `module`.
- Codecs: Mantle loadables gain `StreamCodec` support, so we follow Mantle's patterns.
- `FriendlyByteBuf` → `RegistryFriendlyByteBuf` in network-synced data (material and modifier sync, recipes).
- Recipes: `Container` → `RecipeInput` implementations for the tinker station, part builder, casting, melting, alloying and modifier recipes.
- Attribute modifiers: UUIDs → `ResourceLocation` ids.
- Enchantment-related modifiers and hooks: adapt to data-driven `Holder<Enchantment>`.

- **Progress so far** (errors 17,014 → 3,926). Porting notes and helper scripts are in `project_files/port_1.21/MANTLE_API.md`.
  - **Build unblocked for Lombok.** javac skips annotation processing after certain early errors, which hid every Lombok member.
    Fixed with `-XDshould-stop.ifError=FLOW` (temporary) and by stubbing unresolvable imports with `// TODO 1.21 import:` markers.
  - **Typed IDs:** `ResourceLocation` is final in 1.21. The AT removes `final` and widens its constructor, so `MaterialId`, `ModifierId`,
    `MaterialStatsId`, `ToolStatId` and `Pattern` keep extending it (decided with the user over a wrapper redesign).
  - **Registration:** `DeferredHolder`/`DeferredItem`/`DeferredBlock`, Mantle's specialized registers, the injected mod event bus,
    and registry aliases instead of missing mappings.
  - **Networking:** all 30 packets are `CustomPacketPayload`s; `TinkerNetwork` is a facade over `PacketDistributor` (network version 6).
  - **Conditions:** codec based. `ConfigEnabledCondition` serves both recipe and loot conditions. The deprecated-for-removal
    `TagNotEmptyCondition`, `TagIntersectionPresentCondition`, `TagDifferencePresentCondition` and `TagPreferenceLootEntry` were removed
    in favour of their Mantle replacements; the books now use `mantle:tag_filled`.
  - **Item display contexts** are enum extensions (`META-INF/enumextensions.json`). Serialized names are unchanged.
  - **Block positions in NBT:** 1.21 writes them as int arrays. `TagUtil.readPos` reads both that and the 1.20 compound format,
    so block entities from 1.20 worlds still load their multiblock state.
  - `TinkerToolEvent` has its own `Result` enum, because NeoForge removed event results.
  - Fixed while porting: `JsonRedirect` parsed its whole object as the condition instead of the nested `condition` field.
- **Tool data, step 1 done:** tool NBT lives in the `tconstruct:tool_data` component (`ToolNBT`). `ToolStack` stays linked to its stack.
  Reads share the stored tag, the first modification copies it, and each change is written back as a new immutable component.
  This keeps the 1.20 shared-NBT behaviour that ~150 `ToolStack.from(stack)` call sites rely on. The durability stat and rarity are mirrored
  to vanilla's `MAX_DAMAGE`/`DAMAGE`/`RARITY` components. Use `ToolNBT.read(stack)` and `ToolNBT.update(stack, …)` for raw access.
- **Entity data:** persistent data, entity modifiers and tinker data are data attachments. `LazyOptional` holders became `Optional`.
- **Item capabilities:** registered per capability for all modifiable items in `RegisterCapabilitiesEvent` (`ToolCapabilityProvider`).
  Providers must be registered before that event. `BlockItemProviderCapability` is an `ItemCapability`.
- **Hook API changes (addon facing):** `EnchantmentModifierHook` takes enchantment holders, and single-enchantment hooks use `ResourceKey`s.
  `AttributesModifierHook` takes attribute holders and needs per-slot modifier IDs (`AttributesModifierHook.slotId`).
- **Block breaking:** the `onBlockStartBreak` item hook is gone, so `ToolHarvestLogic` handles a lowest-priority `BreakEvent`.
  XP now drops through vanilla's block drops instead of being popped by Tinkers.
- **Harvest tiers:** `TierSortingRegistry` is gone, so `HarvestTiers` keeps the tier order (vanilla seeded, same names).
  Other mods' tiers must now be registered with `HarvestTiers.register`; Forge used to sort them automatically.
- **Tooltips:** vanilla now displays enchantments, as tooltip hide flags were removed; they appear above the modifier list instead of below it.
  Vanilla attribute lines are still hidden while holding shift/ctrl, through the attribute component.
- Fixed while porting: `RarityModule` could index past the end of the rarity values.
- **Armor:** vanilla armor materials are registry entries; each `DummyArmorMaterial` registers a zero-stat vanilla material for its
  equip sound. Armor uses vanilla's per-slot modifier IDs.
- **Material parts:** typed `tconstruct:material` component holding the `MaterialVariantId`. Part rarity is set as a component from the material.
- **Station layout icons:** JSON uses `components` instead of `nbt`.
- **Open: modifier looting.** NeoForge removed `LootingLevelEvent`; looting is an enchantment read by loot functions without the kill context.
  Context-based looting (projectile modifiers, the blamed slot, bleeding kills, armor looting) is disabled until a new hook is designed.
  The logic is kept in `ModifierLootingHandler#getLootingLevel`.
- **Recipes (done, `library/recipe` compiles).**
  - Recipes no longer store IDs; the ID comes from the `RecipeHolder`. Recipe type and serializer are loadable context fields.
    Builders pair them with `TypedSerializer`.
  - Containers became recipe inputs: Mantle's `NoItemInput`/`SingleItemInput`, crafting recipes use `CraftingInput`.
  - Multi recipes are Mantle's `MultiNamedRecipe`. `getDisplayRecipes(id, provider)` builds the unnamed list and `MultiRecipeHelper` names it
    from the parent ID (`/<index>` suffixes when there are several). Display recipes keep the parent ID; `getRecipeId()` on base recipes is gone,
    JEI takes the ID from the holder or `NamedRecipe` (phase 7).
  - Custom ingredients are NeoForge `ICustomIngredient` records with Mantle loadables. Their factories return vanilla `Ingredient`s;
    `instanceof` checks go through `Ingredient#getCustomIngredient()`. Nested ingredients (`no_container`, `material`) always use the `match` key,
    so the 1.20 flattened JSON form is no longer read. JSON uses NeoForge's `neoforge:ingredient_type` key.
  - Sized item and fluid ingredients are NeoForge's (`SizedIngredients`/`FluidIngredients` helpers keep the old factories).
    Compound fluid ingredients need equal amounts.
  - The shaped and shapeless material crafting recipes use codec serializers mirroring vanilla's, plus `parts` and `extra_materials`.
    `ShapedRecipePattern.data` is opened by the AT so `parts` can be written back out.
  - Container filling recipes take an item at datagen; the `ResourceLocation` overloads that spoofed unloaded items are gone.
  - Casting containers expose fluid components (`getFluidComponents`, `getFluidPotion`) instead of fluid NBT.
    Potion casting copies the fluid's component patch onto the result.
  - Datagen output wrappers (`MaterialsConsumerBuilder`, `ShapedMaterialConsumerBuilder`) wrap `RecipeOutput` and convert the vanilla recipe.
  - Mantle's removed `RecipeHelper` lookups are in `TinkerRecipeHelper`.
- **Remaining in phase 2:** the remaining modifier modules,
  `library/json` (loot functions and entries with `MapCodec`), `library/data` (datagen providers) and `library/client`.

### Phase 3: Content modules
`common` → `shared` → `fluids` → `smeltery` → `tables` → `gadgets` → `world` → `tools`.
- Capabilities: register through `RegisterCapabilitiesEvent`. Block entities lose `getCapability`/`LazyOptional`.
  The fluid and item handler code in smeltery, tanks, tables and duct are the biggest consumers.
- Networking: rewrite `TinkerNetwork` and all 33 packet classes as `CustomPacketPayload` records.
- Armor materials become registry entries. Tinker armor rendering and `ArmorDefinitions` need updating.
- Worldgen and structures: update to 1.21 codecs and registry bootstraps (slime islands, trees, geodes).
- Loot: GLMs and loot functions move to `MapCodec`. Loot tables are accessed by `ResourceKey`.

### Phase 4: Client
Model loaders (the material, tool and fluid models, via Mantle), `BlockEntityRenderer`s, screens/menus, tooltips,
particles, the 1.21 vertex consumer API (`addVertex`/`setColor`) and `RenderType` changes.

### Phase 5: Datagen and resources
- Port data providers (they use `CompletableFuture<HolderLookup.Provider>`, singular folder names and `neoforge:` conditions).
- Delete and regenerate `src/generated`. Move the hand-written `src/main/resources/data` files.
- Convert `forge:` tags to `c:` tags, following the 1.21 convention tag changes.

### Phase 6: Tests and in-game verification
- Port the 66 test files (JUnit 5 upgrade, mock bootstrap changes).
- `runClient` / `runServer` / `runData` smoke tests: build each tool, the smeltery and foundry, casting, modifiers, armor and slime islands.

### Phase 7: Optional compat
Re-enable JEI (19.x) first, then the others as 1.21.1 versions of those mods allow.

## Rough size
This is a full-mod port of a ~200k-line codebase onto a dependency that is still being ported.
Phases 0–1 are hours of work. Phases 2–4 are the bulk of it and will take many working sessions.
Expect a long tail of runtime bugs after the code compiles.
