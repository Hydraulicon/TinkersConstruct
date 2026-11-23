# Port plan: Minecraft 1.20.1 (Forge) → 1.21.1 (NeoForge)

Status: **draft for review**. Nothing is ported yet.

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
fixer that moves `tag.*` tool keys into the new component. This is optional but should be decided before release.

## Phases

Each phase ends with a commit. From phase 2 on, it also ends with a compiling (or compiling-so-far) state, where practical.

### Phase 0: Toolchain and dependencies
- Build Mantle `1.21` locally and record the pinned commit.
- Replace `build.gradle` with a ModDevGradle setup modelled on Mantle's (Java 21, Parchment 1.21.1, NeoForge 21.1.265).
- Rename `META-INF/mods.toml` → `neoforge.mods.toml` and update dependency entries (`neoforge`, `mantle`, JEI 19.x).
- Migrate `accesstransformer.cfg` (198 lines). This means re-mapping names for 1.21.1 and dropping obsolete entries.
- Temporarily exclude optional compat (`plugin/`: JEI, IE, JSON Things, Diet, CraftingTweaks) from compilation.
- **Exit:** Gradle configures and decompiles. Compilation fails with a long list of errors, which is expected.

### Phase 1: Mechanical renames (scripted)
- `net.minecraftforge` → `net.neoforged` package moves (with the per-class mapping table, since not everything moved 1:1).
- `new ResourceLocation(a, b)` / `(s)` → `ResourceLocation.fromNamespaceAndPath` / `parse`.
- `RegistryObject` → `DeferredHolder`, `ForgeRegistries` → `BuiltInRegistries`, `ForgeConfigSpec` → `ModConfigSpec`.
- Event renames (`TickEvent.*` → split `*TickEvent.Pre/Post`, `LivingHurtEvent` → `LivingIncomingDamageEvent`, and others).
- **Exit:** The error count is dominated by real API changes, not renames.

### Phase 2: `library` core
Order: `library/utils`, `json`, `data` → `materials` → `modifiers` → `tools` (incl. tool data component) → `recipe` → `module`.
- Codecs: Mantle loadables gain `StreamCodec` support, so we follow Mantle's patterns.
- `FriendlyByteBuf` → `RegistryFriendlyByteBuf` in network-synced data (material and modifier sync, recipes).
- Recipes: `Container` → `RecipeInput` implementations for the tinker station, part builder, casting, melting, alloying and modifier recipes.
- Attribute modifiers: UUIDs → `ResourceLocation` ids.
- Enchantment-related modifiers and hooks: adapt to data-driven `Holder<Enchantment>`.

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
