# Merged Games - status and porting plan

One Android app, two games, each on its own engine:

| Game | Engine | State |
|------|--------|-------|
| OpenZelda | Lux Engine (C++, SDL2, Pawn scripts) via NDK | Builds into an APK in the cloud; **never run on a device** |
| Minosoft | Kotlin/JVM, LWJGL + GLFW + JavaFX | **Not ported.** Launcher screen is a placeholder |

## How to build the APK

**Without a PC (GitHub builds it for you):** put this project in a GitHub repository. The workflow
`.github/workflows/build-apk.yml` runs on every push, builds the APK in the cloud and attaches
`MergedGames.apk` to a Release on the repo, which you can download and install from your phone.

**With a PC:** run `scripts/fetch-third-party.sh`, open the project root in Android Studio, run on an arm64 phone.

The repo is small on purpose: SDL, the Zelda engine and the game content are downloaded at pinned versions by
`scripts/fetch-third-party.sh`, and our two fixes to the engine are applied from `patches/luxengine.patch`.

Status: the cloud build compiles, links and packages the APK successfully (arm64 only). Nothing has been run on a
phone yet, so everything under "Likely first problems" is still unknown.

## Touch controls

`TouchControlsView` draws a d-pad, four face buttons (A S D Q), shoulders (W E), START and BACK over the game and
sends the matching keys to the engine. Touches outside the controls go to the game as pointer input.

## What was verified, and how

- GitHub Actions builds the APK from this repo (compile + link with the Android NDK, Gradle packaging). Contents checked: native libs for arm64, 524 game content files.
- Before that, the engine was compiled and linked with clang on a Linux host to find errors faster than cloud round trips.
- NOT verified: that the game starts, renders, plays sound, or responds to the touch buttons on a real phone.

## Changes made to upstream source (both marked in code)

1. `luxengine/src/sprite_sheet.cpp` (3 places): `std::make_pair<uint32_t, LuxSprite*>(...)` -> `std::pair<uint32_t, LuxSprite*>(...)`. The original does not compile on current compilers (explicit template args vs. rvalue-reference `make_pair`).
2. `luxengine/src/platform/sdl2/platform_main.cpp`: the `ANDROID_NDK` entry point started a hardcoded game (`puttytris.game`); it now starts the `game.mokoi` path passed in `argv[1]`.

3. Seven more small fixes (`luxengine.patch` has all of them): C++03-style string literals written as `"text"MACRO` (no space), which clang rejects, in `config.cpp`, `engine.cpp`, `game_config.cpp`, `elix_path.cpp`, `mokoi_game.cpp`, `stdheader.h`.

Files in the engine tree that its own makefile never builds are excluded in CMake (leftover `map_object_data.cpp`, `project_media.cpp`, `main.cpp`, `lux_types.cpp`, `map_masks.cpp`, `portal_item.cpp`, `reusable_graphics_system.cpp`, `tjs.cpp`, the Squirrel backend, the `gles/` display code, the desktop-OpenGL backend).

## Likely first problems (OpenZelda)

- **Which button does what** in OpenZelda is untested; the overlay sends the engine's default keys, so labels may need adjusting once you play.
- **Rendering.** The engine's desktop-OpenGL backend uses fixed-function GL 2.x, which Android doesn't have, so the app is built in the engine's `DISPLAYMODE_NATIVE` (the one its Raspberry Pi build uses), which draws through SDL's 2D renderer. Untested on a phone: it may be slower than the OpenGL path, and visual effects the engine implements only in OpenGL (shaders, some blend effects) will be missing.
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
