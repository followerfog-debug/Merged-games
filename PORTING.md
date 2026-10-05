# Merged Games - status and porting plan

One Android app, two games, each on its own engine:

| Game | Engine | State |
|------|--------|-------|
| OpenZelda | Lux Engine (C++, SDL2, Pawn scripts) via NDK | Wired up, **never compiled with the NDK, never run** |
| Minosoft | Kotlin/JVM, LWJGL + GLFW + JavaFX | **Not ported.** Launcher screen is a placeholder |

## How to build the APK

**Without a PC (GitHub builds it for you):** put this project in a GitHub repository. The workflow
`.github/workflows/build-apk.yml` runs on every push, builds the APK in the cloud and attaches
`MergedGames.apk` to a Release on the repo, which you can download and install from your phone.

**With a PC:** run `scripts/fetch-third-party.sh`, open the project root in Android Studio, run on an arm64 phone.

The repo is small on purpose: SDL, the Zelda engine and the game content are downloaded at pinned versions by
`scripts/fetch-third-party.sh`, and our two fixes to the engine are applied from `patches/luxengine.patch`.

The build setup was validated only up to this point: the fetch script runs from a clean directory, and CMake
configures the whole native project on a Linux host. The Android compile itself has not been run, so expect to
fix first-build errors; see "Likely first problems".

## Touch controls

`TouchControlsView` draws a d-pad, four face buttons (A S D Q), shoulders (W E), START and BACK over the game and
sends the matching keys to the engine. Touches outside the controls go to the game as pointer input.

## What was verified, and how

No Android SDK/NDK was reachable from the environment this was written in, so nothing was linked or run.
What *was* done on a Linux host:

- All 154 C++ and 4 C engine sources in the CMake source list pass `-fsyntax-only` with the engine's real defines against SDL 2.33 headers.
- The files that only compile with `ANDROID_NDK` (elix directory/path, tinyxml2, platform_main, engine) pass the same check against stand-in Android headers.
- SDLActivity method signatures used by `ZeldaActivity` were checked against the cloned SDL source.

## Changes made to upstream source (both marked in code)

1. `luxengine/src/sprite_sheet.cpp` (3 places): `std::make_pair<uint32_t, LuxSprite*>(...)` -> `std::pair<uint32_t, LuxSprite*>(...)`. The original does not compile on current compilers (explicit template args vs. rvalue-reference `make_pair`).
2. `luxengine/src/platform/sdl2/platform_main.cpp`: the `ANDROID_NDK` entry point started a hardcoded game (`puttytris.game`); it now starts the `game.mokoi` path passed in `argv[1]`.

Files in the engine tree that its own makefile never builds are excluded in CMake (leftover `map_object_data.cpp`, `portal_item.cpp`, `reusable_graphics_system.cpp`, the Squirrel backend, `shaders_*.inc.cpp` fragments).

## Likely first problems (OpenZelda)

- **Which button does what** in OpenZelda is untested; the overlay sends the engine's default keys, so labels may need adjusting once you play.
- **GL path.** The SDL2 platform is built with `DISPLAYMODE_OPENGL`; on Android it must create a GLES context. If the display code asks for desktop GL, set SDL's GL attributes to ES in `platform/sdl2/graphics_opengl.cpp`.
- **Link errors** from files the makefile lists that CMake's globs miss, or the reverse.
- **Content loading.** Game content is copied from `assets/openzelda` to app storage at first launch (`ZeldaActivity`). If the engine expects the directory layout of a `.package` file, point `argv[1]` at the right entry.
- 32-bit phones: only `arm64-v8a` is enabled (`app/build.gradle.kts`); the engine's 64-bit Pawn cell size flags are set for it.

## Minosoft port plan

Minosoft (3,500 Kotlin files) assumes a desktop: LWJGL/GLFW window and OpenGL 3.3, JavaFX UI (34 `.fxml` files), Java 11+ APIs. Phases, in order:

- **A. Core as an Android library.** Network protocol, world, entities, physics. Replace desktop-only pieces (`java.net.http`, JavaFX, LWJGL natives); verify dependencies run on ART. Test headless first (Minosoft already has `--headless`).
- **B. Renderer.** Replace GLFW context/LWJGL bindings with `GLSurfaceView` + `GLES30`. Shaders (23 `.vsh`, 28 `.fsh`) need `#version 300 es` and precision qualifiers.
- **C. Input.** Touch joystick, look, and action buttons instead of GLFW keyboard/mouse.
- **D. UI.** Rebuild the "eros" screens (accounts, server list, settings) natively instead of JavaFX.
- **E. Runtime data.** Minecraft assets (~300 MiB per version) are downloaded from Mojang at runtime into app storage; Microsoft account login needs a browser/device-code flow.

Expect phases B and D to be the bulk of the work. Minosoft wants 4+ cores and 500 MiB-1 GiB RAM; low-end phones will struggle.

## Licenses

- Minosoft: GPLv3. Lux Engine / elix: zlib. A combined app must be distributed under GPLv3 terms.
- OpenZelda game scripts: CC BY-NC-SA 3.0. Game art/sound: Nintendo copyright, not open source. Non-commercial use only.
- Minecraft assets are never bundled; Minosoft downloads them from Mojang.
