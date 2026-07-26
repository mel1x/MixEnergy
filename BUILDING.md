# Building MixEnergy

MixEnergy is a multi-version project. One source tree in `src/` is compiled once per
supported Minecraft version using [Stonecutter](https://stonecutter.kikugie.dev/), which
rewrites version-specific code before each compilation.

## Supported targets

| Subproject | Minecraft | Loader | Loader version | Java | Declared MC range |
|---|---|---|---|---|---|
| `1.20.1-forge` | 1.20.1 | Forge | 47.3.0 | 17 | `[1.20.1,1.20.2)` |
| `1.20.2-forge` | 1.20.2 | Forge | 48.1.0 | 17 | `[1.20.2,1.20.3)` |
| `1.20.4-forge` | 1.20.4 | Forge | 49.2.8 | 17 | `[1.20.3,1.20.5)` |
| `1.20.6-forge` | 1.20.6 | Forge | 50.2.10 | 21 | `[1.20.6,1.21)` |
| `1.21.1-neoforge` | 1.21.1 | NeoForge | 21.1.243 | 21 | `[1.21,1.21.2)` |
| `1.21.3-neoforge` | 1.21.3 | NeoForge | 21.3.97 | 21 | `[1.21.2,1.21.4)` |
| `1.21.4-neoforge` | 1.21.4 | NeoForge | 21.4.157 | 21 | `[1.21.4,1.21.5)` |
| `1.21.5-neoforge` | 1.21.5 | NeoForge | 21.5.98 | 21 | `[1.21.5,1.21.6)` |
| `1.21.6-neoforge` | 1.21.6 | NeoForge | 21.6.20-beta | 21 | `[1.21.6,1.21.7)` |
| `1.21.8-neoforge` | 1.21.8 | NeoForge | 21.8.54 | 21 | `[1.21.7,1.21.9)` |
| `1.21.10-neoforge` | 1.21.10 | NeoForge | 21.10.64 | 21 | `[1.21.9,1.21.11)` |
| `1.21.11-neoforge` | 1.21.11 | NeoForge | 21.11.44 | 21 | `[1.21.11,1.21.12)` |
| `26.1.1-neoforge` | 26.1.1 | NeoForge | 26.1.1.15-beta | 25 | `[26.1,26.1.2)` |
| `26.1.2-neoforge` | 26.1.2 | NeoForge | 26.1.2.85 | 25 | `[26.1.2,26.2)` |
| `26.2-neoforge` | 26.2 | NeoForge | 26.2.0.32-beta | 25 | `[26.2,26.3)` |

Each row produces `mixenergy-<mod_version>+<mc>-<loader>.jar`.

Targets are grouped where Minecraft did not break mod compatibility, so one jar can cover
several game versions. Combat Roll and Better Combat have no release for the 1.21.2–1.21.3
and 1.21.5 lines, so their integrations are compiled out of those two targets.

`1.21.6-neoforge` is its own target even though 1.21.6-1.21.8 share the same vanilla GUI
rendering rewrite: NeoForge's own network payload-handler split (`PayloadRegistrar
.playBidirectional` taking two handlers, `ClientPacketDistributor`) landed one patch later,
in the 21.7.x line. NeoForge 21.6.20-beta - the only release for Minecraft 1.21.6 - still
uses the older single-handler API, so a jar compiled against 21.7+ throws
`NoSuchMethodError` at runtime on that one Minecraft patch. See `NetworkHandler.java` for
the `<1.21.7` predicates this requires.

The 1.20.x Forge line needs **one target per Forge major**, because Forge bumps its major
version precisely when Minecraft breaks mod compatibility: 47 = 1.20.1, 48 = 1.20.2,
49 = 1.20.3-1.20.4, 50 = 1.20.5-1.20.6. `loaderVersion` in `mods.toml` is the javafml
language provider version, which Forge keeps equal to its own major, so a jar built for one
major is refused outright by the others ("needs language provider javafml:50 … we have
found 49.2.8").

Each of those majors carries real API breaks for this mod:

- **1.20.2 (Forge 48)** rewrote Forge networking - `net.minecraftforge.network.simple
  .SimpleChannel` and `NetworkRegistry.newSimpleChannel` removed for a `SimpleChannel` in
  the base `network` package built through a `ChannelBuilder`, `NetworkEvent`/`DistExecutor`
  removed for `CustomPayloadEvent`, and `PacketDistributor.send` argument order swapped. It
  also dropped `NetworkHooks` (the vanilla spawn packet now carries synched data) and
  renamed `MobEffect#isDurationEffectTick` to `shouldApplyEffectTickThisTick`.
- **1.20.5 (Forge 50)** is where a batch of *vanilla* APIs moved that this project had
  previously attributed to 1.21 - nothing between Forge 1.20.1 and NeoForge 1.21.1 existed
  to reveal the real boundary, since NeoForge starts at 1.20.5. Holder-based effects and
  attributes, the entity data builder, the vertex-builder normal overload and the
  `LayeredDraw` HUD hook all land here; see the table below.

Combat Roll and Better Combat only have a 1.20.2 Forge release in this span, so their
integrations are compiled out of the 1.20.4 and 1.20.6 targets.

Forge never shipped a build for Minecraft 1.20.5 (it went 1.20.4 → 1.20.6), so no target
declares it. That is also why the 1.20.5 boundary above is only ever crossed by the 1.20.6
target: the predicates say `1.20.5` because that is the Minecraft release the APIs actually
moved in, but the lowest Forge build that can hit them is for 1.20.6.

## Requirements

Three JDKs must be installed and discoverable by Gradle's toolchain detection, because the
targets are compiled against the Java version each Minecraft release ships to players:

| JDK | Used for |
|---|---|
| 17 | `1.20.1-forge` toolchain |
| 21 | the Gradle daemon, and the `1.21.x` toolchains |
| 25 | the `26.x` toolchains |

The Gradle daemon runs on **Java 21**, pinned by `gradle/gradle-daemon-jvm.properties` so
that it does not depend on `JAVA_HOME` or on a global `org.gradle.java.home`. The pin
matters: Gradle 8.14 cannot start on Java 25, and the wrapper is held at 8.14.3 because
ForgeGradle 6 (needed for 1.20.1) does not work on Gradle 9, while ModDevGradle cannot
resolve the NeoForge 26.2 dependency on Gradle 8.8.

Nothing else needs configuring — `./gradlew buildAll` works as-is.

## Common commands

Build every version:

```bash
./gradlew buildAll
```

Collect all release jars into `build/libs`:

```bash
./gradlew collectJars
```

Build or run a single version:

```bash
./gradlew :1.21.1-neoforge:build
```

The single-version `build` task also copies its release jar into the root `build/libs`
directory, while retaining the original under `versions/<target>/build/libs`.

```bash
./gradlew :1.21.1-neoforge:runClient
```

## Working on the code

`src/` always holds the state of one *active* version. Switching the active version
rewrites the comment markers in place:

```bash
./gradlew "Set active project to 1.21.1-neoforge"
```

Before committing, restore the state the repository stores (`1.20.1-forge`):

```bash
./gradlew "Reset active project"
```

If a file ends up with markers in an inconsistent state — for example after adding a new
conditional block by hand — normalise it with:

```bash
./gradlew "Refresh active project"
```

## Version-specific code

Code is selected with comment directives. They are ordinary comments, so the IDE and the
compiler only ever see the branch that applies to the active version.

```java
//? if >=1.21 {
return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
//?} else {
/*return new ResourceLocation(MOD_ID, path);
*///?}
```

Available predicates:

- Version comparisons against the target's Minecraft version: `>=1.21`, `<1.21.6`, `1.20.1`.
- `forge` / `neoforge` — the mod loader of the current target.
- `combatroll` / `bettercombat` — whether that integration is available for the target,
  derived from `deps_combatroll` / `deps_bettercombat` in the target's `gradle.properties`.

Chains use `elif` / `else`, e.g. `//?} elif <1.21.5 {`.

## Adding a target

1. Register it in `settings.gradle` under `stonecutter { create(rootProject) { ... } }`.
   The first argument is the subproject directory, the second is the Minecraft version used
   by the predicates above.
2. Create `versions/<subproject>/gradle.properties` with the loader, Minecraft version and
   range, loader version, `java_version` and `pack_format`. Copy the closest existing file.
3. Run `./gradlew :<subproject>:compileJava` and resolve whatever the compiler reports.

On NeoForge, `loaderVersion` in `neoforge.mods.toml` is the version of the **javafml
language provider** — that is, FancyModLoader — and not the NeoForge version. The two are
numbered independently: NeoForge 21.11 ships FancyModLoader 10.0, and putting `[21.11,)`
there makes the game refuse the jar with *"needs language provider javafml:21.11 or above
to load. We have found 10.0"*. The properties are therefore separate:

- `loader_version_range` — javafml, e.g. `[10,)`. Take the FancyModLoader major of the
  **lowest** NeoForge release covered by the target's Minecraft range; it is listed as the
  `net.neoforged.fancymodloader:loader` dependency of the NeoForge Gradle metadata at
  `https://maven.neoforged.net/releases/net/neoforged/neoforge/<version>/neoforge-<version>.module`.
- `neoforge_version_range` — the NeoForge dependency itself, e.g. `[21.11,)`.

For reference, the mapping used here: NeoForge 21.0–21.3 → javafml 4, 21.4–21.5 → 6,
21.6–21.8 → 7, 21.9–21.11 → 10, 26.1–26.2 → 11.

`build.gradle` needs no change: it applies ForgeGradle for `loader=forge` targets and
ModDevGradle for `loader=neoforge` targets, and reads everything else from properties.

## Where the loader differences live

| Concern | Forge 1.20.1 | NeoForge |
|---|---|---|
| Player energy storage | capability (`PlayerEnergyProvider`) | data attachment, same class |
| Networking | `SimpleChannel` | `CustomPacketPayload` + `StreamCodec` |
| Config spec | `ForgeConfigSpec` | `ModConfigSpec` |
| Config screen hook | `ConfigScreenHandler` | `IConfigScreenFactory` |
| HUD hook | `RenderGuiOverlayEvent` | `RenderGuiLayerEvent` |
| Tick / attack events | `TickEvent`, `LivingAttackEvent` | `PlayerTickEvent.Post`, `LivingIncomingDamageEvent` |

Call sites avoid most of this by going through `PlayerEnergyProvider.get(player)`,
`MixEnergyEffects.isFatigued(entity)` / `fatigue(ticks)`, `MixEnergy.id(path)` and
`NetworkHandler.sendToPlayer` / `sendToServer`.

## How the bar tracks the energy value

Three values are involved, and keeping them straight matters when touching the HUD:

- `energyValue` — the last value the server sent. Authoritative.
- `projectedEnergyValue` — that value carried forward locally, one client tick at a time,
  by the trend the server reported (sprint or swim cost, or the regeneration rate). This is
  what lets the bar keep moving between packets instead of stepping once per update.
- `displayedEnergyValue` — what is actually drawn. It eases towards the projection once per
  **frame**, off the wall clock, closing a fixed fraction of the remaining gap per
  millisecond (`VISUAL_RESPONSE_MILLIS`).

Because the ease is computed from elapsed milliseconds rather than ticks, the bar takes the
same time to catch up at any frame rate: roughly 90% of a gap within 170 ms, fully settled
under 600 ms, with a steady-state lag while sprinting of under 1% of the bar. Corrections
work identically in both directions — an earlier version skipped any correction that ran
against the current trend, which let the bar sit at a stale value indefinitely while
sprinting.

The server pushes a value whenever it changes, at most every `CLIENT_SYNC_INTERVAL_TICKS`
(2) ticks, and always immediately for a discrete cost such as a block break or an attack.
Nothing is sent while the value is unchanged.

## Where the Minecraft differences live

Every version predicate in `src/` traces back to one of these breaks. Boundaries were read
off the compiled jars, so they are the version the change actually landed in.

| From | Change | Affected file |
|---|---|---|
| 1.20.2 | `renderBackground` takes the mouse position and partial tick; `renderTransparentBackground` added (Forge 1.20.1 has neither) | `MixEnergyConfigScreen` |
| 1.20.2 | Forge network rewrite: `SimpleChannel`/`NetworkRegistry.newSimpleChannel` removed for a `ChannelBuilder`, `NetworkEvent`/`DistExecutor` removed for `CustomPayloadEvent`, `PacketDistributor.send` argument order swapped | `NetworkHandler`, both packets |
| 1.20.2 | `MobEffect#isDurationEffectTick` renamed to `shouldApplyEffectTickThisTick` - one release before `applyEffectTick` started returning a boolean, so 1.20.2-1.20.4 needs a branch of its own | `MixEnergySlownessEffect` |
| 1.20.2 (Forge only) | `NetworkHooks` removed; the vanilla spawn packet carries synched data, so `getAddEntityPacket` needs no override | `EnergyOrbEntity` |
| 1.20.2 | `Screen#render` began painting the background itself before iterating widgets, so calling `super.render` from an override re-runs it (and the blur) after the screen's own content is drawn. Stopped again in 1.21.6, when the background moved up into `renderWithTooltip` | `MixEnergyConfigScreen` |
| 1.20.5 | `MobEffect`/attribute APIs take `Holder<MobEffect>`/`Holder<Attribute>` instead of the object itself; `MULTIPLY_TOTAL` renamed to `ADD_MULTIPLIED_TOTAL`. NeoForge only exists from this point on, so it always needs the new form; Forge 1.20.1 predates it | `MixEnergyEffects`, `MixEnergySlownessEffect` |
| 1.20.5 (Forge only) | `VertexConsumer#normal(Matrix3f, ...)` replaced by `normal(PoseStack.Pose, ...)`, keeping the rest of the old chained builder | `EnergyOrbRenderer` |
| 1.21 | Attribute modifiers keyed by `ResourceLocation`. Up to 1.20.6 the key stays a `String` that `MobEffect#addAttributeModifier` parses with `UUID.fromString`, so it must be a UUID - passing a readable name compiles and then throws `IllegalArgumentException: Invalid UUID string` at registration | `MixEnergySlownessEffect` |
| 1.20.5 | Synched data defined through a builder; the default spawn packet already carries it, so Forge's `NetworkHooks.getEntitySpawningPacket` is no longer needed | `EnergyOrbEntity` |
| 1.20.5 (Forge only) | The per-frame `RenderGuiOverlayEvent`/`VanillaGuiOverlay` HUD hook was replaced by a `LayeredDraw` the mod registers a layer into once, via a new mod-bus `AddGuiOverlayLayersEvent` | `EnergyOverlayHandler`, `ClientModEvents` |
| 1.21 | `ResourceLocation` constructor replaced by `fromNamespaceAndPath` | `MixEnergy` |
| 1.21 | Vertex builder renamed to `addVertex`/`setColor`/etc.; `endVertex()` dropped | `EnergyOrbRenderer` |
| 1.21.2 | `Entity#hurtServer` is abstract | `EnergyOrbEntity` |
| 1.21.2 | `MobEffect#applyEffectTick` takes a `ServerLevel` | `MixEnergySlownessEffect` |
| 1.21.2 | `EntityType.Builder#build` takes a `ResourceKey` | `MixEnergyEntities` |
| 1.21.2 | `EntityRenderer` gained a render-state type parameter | `EnergyOrbRenderer` |
| 1.21.2 | `blit` takes a render type and a per-call tint | `EnergyOverlayHandler` |
| 1.21.5 | `CompoundTag` getters return `Optional` (`getIntOr` / `getFloatOr`) | `EnergyOrbEntity` |
| 1.21.6 | Entity NBT replaced by `ValueInput` / `ValueOutput` | `EnergyOrbEntity` |
| 1.21.6 | `blit` takes a render pipeline instead of a render type | `EnergyOverlayHandler` |
| 1.21.6 | `renderTooltip` became `setTooltipForNextFrame` | `MixEnergyConfigScreen` |
| 1.21.6 | Attachment `serialize` takes a `MapCodec` | `PlayerEnergyProvider` |
| 1.21.7 (NeoForge 21.7.x) | Sending to the server moved to `ClientPacketDistributor`; NeoForge 21.6.20-beta (Minecraft 1.21.6) does not have this class | `NetworkHandler` |
| 1.21.7 (NeoForge 21.7.x) | `playBidirectional` with one handler registers the serverbound direction only; the clientbound handler is a fourth argument. NeoForge 21.6.20-beta still registers one handler for both directions | `NetworkHandler` |
| 1.21.9 | `EntityRenderer#render` became `submit` with a node collector | `EnergyOrbRenderer` |
| 1.21.9 | `FMLEnvironment.dist` became `getDist()` | `MixEnergyConfigMigration` |
| 1.21.11 | `ResourceLocation` renamed to `Identifier` | `MixEnergy`, both renderers |
| 1.21.11 | `RenderType` factories moved to `rendertype.RenderTypes` | `EnergyOrbRenderer` |
| 1.21.11 | `Util` moved to `net.minecraft.util` | `EnergyOverlayHandler` |
| 1.21.11 | Numeric permission levels replaced by `PermissionSet` | `EnergyCommands` |
| pack format 65 | `pack_format` / `supported_formats` replaced by `min_format` / `max_format`; the old spelling makes the game drop the mod's resources | `pack.mcmeta`, `build.gradle` |
| 26.1 | `GuiGraphics` renamed to `GuiGraphicsExtractor` | `EnergyOverlayHandler`, `MixEnergyConfigScreen` |
| 26.1 | `Screen#render` became `extractRenderState`; text helpers renamed | `MixEnergyConfigScreen` |
| 1.21.6 | `renderWithTooltip`/`renderWithTooltipAndSubtitles` (and, from 26.1, `extractRenderStateWithTooltipAndSubtitles`) started calling `renderBackground`/`extractBackground` themselves before invoking the screen; calling it again crashes with "Can only blur once per frame" | `MixEnergyConfigScreen` |
| 26.1 | `LightTexture` removed | `EnergyOrbRenderer` |
| 26.1.2 (NeoForge) | `BlockEvent.BreakEvent` became `BreakBlockEvent`; this requires separate 26.1–26.1.1 and 26.1.2 jars | `PlayerEnergyManager` |
