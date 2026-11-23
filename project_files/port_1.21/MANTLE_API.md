# Porting notes: Mantle 1.20 → 1.21 and NeoForge

A reference for the remaining phases of `MIGRATION_1.21.md`. It is built up as the port goes; when in doubt, read Mantle's
`1.21` branch at the pinned commit (`../Mantle`). Mantle's commit messages since the 1.20 merge base (`1032b476`) usually
name the change: `git -C ../Mantle log 1032b476..origin/1.21 -- <path>`.

## Tooling (`project_files/port_1.21/`)

| Script | Use |
|---|---|
| `build_index.sh`, `mapimports.py`, `rewrite.py` | Phase 1 import and rename pass (already applied) |
| `registryobject.py` | `RegistryObject<T>` → `DeferredHolder<R, T>` (already applied) |
| `packets.py` | SimpleChannel packets → `CustomPacketPayload` (already applied) |
| `errfix.py <log> stats` / `list <regex>` | Summarize or filter javac errors (deduplicated) |
| `errfix.py <log> loadable-overrides` | Fix overrides of Mantle loadable methods (see below) |
| `errfix.py <log> upgrade-buffers` | `FriendlyByteBuf` → `RegistryFriendlyByteBuf` at the declaration feeding a Mantle call. Re-run after each compile until it reports 0 |
| `errfix.py <log> drop-get <types>` | Drop `.get()` where a holder is now expected (`Attribute`, `MobEffect`, …) |
| `errfix.py <log> stub-imports` | Comment out unresolvable imports with a `// TODO 1.21 import:` marker |

Compile with `./gradlew compileJava > log 2>&1`, then run the fixers on the log.
`-XDshould-stop.ifError=FLOW` in `build.gradle` keeps Lombok and later stages running; without it, one bad declaration
(for example "wrong number of type arguments" or "cannot inherit from final") hides every Lombok getter in the build.

## Loadables

| 1.20 | 1.21 |
|---|---|
| `JsonElement serialize(T)` | `JsonElement serialize(T, TypedMap context)` (a 1-arg default still exists for callers) |
| `void serialize(T, JsonObject)` on `RecordLoadable` / `RecordField` | `void serializeInto(T, JsonObject, TypedMap context)` |
| `decode(FriendlyByteBuf, TypedMap)` | `decode(RegistryFriendlyByteBuf, TypedMap)` |
| `encode(FriendlyByteBuf, T)` | `encode(RegistryFriendlyByteBuf, T, TypedMap context)` |
| `StringLoadable#getString(T)` | `getString(T, TypedMap context)` |
| `xmap(from, to)` (BiFunctions) | `mapChecked(from, to)` |
| `flatXmap(from, to)` | `map(from, to)` |
| `comapFlatMap(from, to)` / `flatComap(from, to)` | `mapChecked(from, Loadable.noErrors(to))` / `mapChecked(Loadable.noErrors(from), to)` |
| `list()`, `set()` return `Loadable` | return `ArrayLoadable` |
| `EitherLoadable` | `DispatchLoadable` |
| `common.RegistryLoadable` | `registry.RegistryLoadable` |
| GSON support (`GSON_INSTANCE`, `JsonDeserializer`) | removed from loadables |

`TypedMap` lives in `slimeknights.mantle.util.typed`.

## Registration

- `IdAwareObject#getId()` → `id()`.
- `ItemObject<T>` → `ItemObject<R, T>`, where `R` is `Item` or `Block`. Mantle now uses `ItemObject` only for multi-form objects.
  `ItemDeferredRegister#register` returns `DeferredItem<I>` and `BlockDeferredRegister#register` returns `DeferredBlock<B>`; declare fields with those types.
- `RegistryObject<T>` → `DeferredHolder<R, T>`. It is a supertype of Mantle's typed holders (`DeferredRecipeSerializer`, `DeferredMenu`, …), so it accepts either.
  `RegistryObject.create(id, ForgeRegistries.X)` → `DeferredHolder.create(Registries.X, id)` / `DeferredItem.createItem(id)`.
- `SynchronizedDeferredRegister` → NeoForge `DeferredRegister`, or Mantle's `RecipeSerializerDeferredRegister`,
  `LootConditionDeferredRegister`, `LootFunctionDeferredRegister` and `LootEntryDeferredRegister` (register with a `MapCodec`).
- `FMLJavaModLoadingContext.get().getModEventBus()` → the `IEventBus` injected into the mod constructor (`TConstruct(IEventBus, ModContainer)`).
- `ModLoadingContext.get().registerConfig` → `ModContainer#registerConfig`.
- `MissingMappingsEvent` → `Registry#addAlias(from, to)` (see `TConstruct#registerAliases`).
- Attributes and effects are passed as `Holder`s in 1.21. Our `DeferredHolder`s are holders, so pass them directly instead of calling `get()`.
- `ForgeRegistries.X.getValue(id)` → `BuiltInRegistries.X.get(id)`; `getValues()` → `stream()`; `getEntries()` → `entrySet()`.
- Forge registry keys → `NeoForgeRegistries.Keys.*` (`BIOME_MODIFIERS`, `GLOBAL_LOOT_MODIFIER_SERIALIZERS`, `ENTITY_DATA_SERIALIZERS`).
- Custom `ItemDisplayContext`s are enum extensions (`META-INF/enumextensions.json`), looked up with `valueOf`.

## Networking

- `TinkerNetwork` keeps its send helpers (`sendTo`, `sendToServer`, `sendToClientsAround`, `sendToTracking…`) on top of `PacketDistributor`,
  and registers packets in `RegisterPayloadHandlersEvent`. Handlers run on the main thread.
- A packet implements Mantle's `ISimplePacket` (a `CustomPacketPayload`), with
  `public static final Type<P> TYPE`, a `StreamCodec<RegistryFriendlyByteBuf, P> CODEC` and `type()` returning `TYPE`.
  `IThreadsafePacket#handleThreadsafe(Context)` → `handle(IPayloadContext)`; `context.getSender()` → `(ServerPlayer) context.player()`.
- Mantle's `BlockEntityPacket`: `type()` → `blockEntityType()`; `handleBlockEntity(IPayloadContext, T)`.
- `buf.readFluidStack()` / `writeFluidStack` → `FluidStack.OPTIONAL_STREAM_CODEC.decode/encode`; same for items with `ItemStack.OPTIONAL_STREAM_CODEC`.
- `NetworkHooks.openScreen(player, provider, …)` → `player.openMenu(provider, …)`.
  `NetworkHooks.getEntitySpawningPacket` overrides are deleted, because NeoForge sends extra spawn data for `IEntityWithComplexSpawn` automatically.
- Mantle `PacketHelper` has `syncPackets(OnDatapackSyncEvent, …)` for datapack registries.

## Vanilla

- `ResourceLocation` is final. We remove `final` with our AT so `ResourceId` subclasses still work; `ResourceId.Dummy` replaces `ResourceLocation.Dummy`.
- `new ResourceLocation(ns, path)` → `ResourceLocation.fromNamespaceAndPath`; `new ResourceLocation(s)` → `ResourceLocation.parse`.
- `AttributeModifier.Operation`: `ADDITION`/`MULTIPLY_BASE`/`MULTIPLY_TOTAL` → `ADD_VALUE`/`ADD_MULTIPLIED_BASE`/`ADD_MULTIPLIED_TOTAL`.
- `FrameType` → `AdvancementType`.
