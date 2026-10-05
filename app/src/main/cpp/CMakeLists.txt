cmake_minimum_required(VERSION 3.22.1)
project(luxengine_android C CXX)

# NOTE: this file mirrors ozengine's makefile (makefiles/linux-sdl2.make + makefile) for Android.
# It has NOT been compiled yet - the authoring environment had no Android NDK. Expect to fix
# compile errors on first build; PORTING.md lists the known ones.

set(TP ${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party)
set(LUX ${TP}/luxengine)

# ---------------------------------------------------------------- SDL2 + SDL2_mixer
add_subdirectory(${TP}/SDL2 SDL2-build)

# SDL2_mixer codecs are vendored; run third_party/SDL2_mixer/external/download.sh once first.
set(SDL2MIXER_VENDORED ON CACHE BOOL "" FORCE)
set(SDL2MIXER_SAMPLES  OFF CACHE BOOL "" FORCE)
set(SDL2MIXER_INSTALL  OFF CACHE BOOL "" FORCE)
add_subdirectory(${TP}/SDL2_mixer SDL2_mixer-build)

# ---------------------------------------------------------------- Lux Engine sources
file(GLOB LUX_TOP        ${LUX}/src/*.cpp)
list(FILTER LUX_TOP EXCLUDE REGEX "downloader.*\\.cpp$|map_network\\.cpp$")

file(GLOB LUX_DIRS
    ${LUX}/src/display/*.cpp
    ${LUX}/src/gui/*.cpp
    ${LUX}/src/portal/*.cpp
    ${LUX}/src/test/*.cpp
    ${LUX}/src/entity_systems/*.cpp
    ${LUX}/src/gles/*.cpp)

# Files in the tree that the engine's own makefile never builds (leftovers, an unused Squirrel
# backend, and *.inc.cpp fragments that other files #include). Verified with a host syntax check.
list(FILTER LUX_TOP  EXCLUDE REGEX "map_object_data\\.cpp$")
list(FILTER LUX_DIRS EXCLUDE REGEX "portal_item\\.cpp$|squirrel_lang\\.cpp$|reusable_graphics_system\\.cpp$|shaders_[a-z_]+\\.inc\\.cpp$")

# Platform layer: SDL2 (same one the desktop build uses). network_client/platform_launch are desktop-only.
file(GLOB LUX_PLATFORM ${LUX}/src/platform/sdl2/*.cpp)
list(FILTER LUX_PLATFORM EXCLUDE REGEX "network_client\\.cpp$|platform_launch\\.cpp$")

# Pawn scripting VM (game logic scripts run on this)
set(LUX_AMX
    ${LUX}/src/scripting/amx4/amx.c
    ${LUX}/src/scripting/amx4/amxcons.c
    ${LUX}/src/scripting/amx4/amxcore.c
    ${LUX}/src/scripting/amx4/amxexec_gcc.c)

# Bundled libraries
file(GLOB_RECURSE LUX_BOX2D ${LUX}/src/libs/Box2D/*.cpp)
file(GLOB LUX_TINYXML ${LUX}/src/libs/tinyxml/*.cpp)
file(GLOB LUX_ELIX
    ${LUX}/src/libs/elix/elix_directory.cpp
    ${LUX}/src/libs/elix/elix_endian.cpp
    ${LUX}/src/libs/elix/elix_file.cpp
    ${LUX}/src/libs/elix/elix_string.cpp
    ${LUX}/src/libs/elix/elix_path.cpp
    ${LUX}/src/libs/elix/elix_png.cpp
    ${LUX}/src/libs/elix/elix_program.cpp)

# downloader: "none" variant (no libcurl on Android)
set(LUX_DOWNLOADER ${LUX}/src/downloader-none.cpp)

add_library(luxengine SHARED
    ${LUX_TOP} ${LUX_DIRS} ${LUX_PLATFORM} ${LUX_AMX}
    ${LUX_BOX2D} ${LUX_TINYXML} ${LUX_ELIX} ${LUX_DOWNLOADER})

target_include_directories(luxengine PRIVATE
    ${LUX}/src ${LUX}/src/libs ${LUX}/src/platform/sdl2 ${LUX}/include ${TP}/elix)

target_compile_definitions(luxengine PRIVATE
    TIXML_USE_STL
    USE_SDL2 DISPLAYMODE_OPENGL OPENGLENABLED
    HAS_SOCKLEN_T HAVE_UNISTD_H HAVE_INTTYPES_H HAVE_STDINT_H
    FLOATPOINT FIXEDPOINT NO_ZLIB
    PLATFORMBITS=64
    # Pawn VM, same flags as the desktop makefile (64-bit cells)
    PAWN_VERSION=4 AMXTIME_NOIDLE AMX_NOPROPLIST AMXCONSOLE_NOIDLE AMX_NODYNALOAD
    AMX_ALTCORE PAWN_CELL_SIZE=64 HAVE_I64
    LINUX          # Unix code paths
    ANDROID_NDK)   # selects elix's Android storage code, tinyxml/elix_path variants and the Android main()
                   # in platform_main.cpp (patched to take the game path from argv[1]).

target_compile_options(luxengine PRIVATE
    -Wno-write-strings -fno-access-control -fno-exceptions -fno-rtti
    -Wno-unused-local-typedefs)

target_link_libraries(luxengine PRIVATE SDL2 SDL2_mixer GLESv2 EGL android log)
