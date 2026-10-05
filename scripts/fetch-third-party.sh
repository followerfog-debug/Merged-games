#!/usr/bin/env bash
# Downloads every upstream dependency at a pinned version and applies our small source patches.
# Run from anywhere; results land in third_party/. Safe to re-run (it skips what is already there).
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"
mkdir -p third_party && cd third_party

# fetch <dir> <repo-url> <ref: tag or commit sha>
fetch() {
  local dir="$1" url="$2" ref="$3"
  [ -d "$dir" ] && { echo "skip $dir (already present)"; return; }
  git init -q "$dir"
  git -C "$dir" remote add origin "$url"
  git -C "$dir" fetch -q --depth 1 origin "$ref"
  git -C "$dir" checkout -q FETCH_HEAD
  rm -rf "$dir/.git"
  echo "fetched $dir @ $ref"
}

# OpenZelda engine ("Lux Engine"), its support library and the game content package.
fetch luxengine        https://github.com/lukesalisbury/luxengine     b4ca9bf2db60d5a781c399fea68baaae2170114c
fetch elix             https://github.com/lukesalisbury/elix          862ae82905047e343f95468b77279b5e606abe13
fetch openzelda-content https://github.com/openzelda/content-package  e7cf22bc45580609931a3233d3b9322180d12dae

# SDL2 and its audio add-on (with the codec libraries SDL2_mixer vendors).
fetch SDL2             https://github.com/libsdl-org/SDL.git          release-2.32.8
fetch SDL2_mixer       https://github.com/libsdl-org/SDL_mixer.git    release-2.8.1
if [ ! -f SDL2_mixer/external/ogg/CMakeLists.txt ]; then
  (cd SDL2_mixer/external && bash download.sh)
  find SDL2_mixer/external -name .git -prune -exec rm -rf {} +
fi

# Our fixes to upstream engine code (see PORTING.md).
if ! grep -q "Patched for the merged app" luxengine/src/platform/sdl2/platform_main.cpp; then
  patch -p1 -d luxengine < "$ROOT/patches/luxengine.patch"
fi

echo "Done."
