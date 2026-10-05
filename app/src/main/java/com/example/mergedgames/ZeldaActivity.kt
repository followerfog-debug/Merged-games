package com.example.mergedgames

import android.content.res.AssetManager
import android.os.Bundle
import android.view.ViewGroup
import org.libsdl.app.SDLActivity
import java.io.File

/**
 * Hosts the Lux Engine (OpenZelda). SDLActivity loads the native libraries, creates the GL surface
 * and calls the engine's main(). Native build: app/src/main/cpp/CMakeLists.txt.
 *
 * The engine reads its game from a real directory, not from inside the APK, so the content package
 * is copied out of assets/openzelda on first launch (and again whenever the app version changes).
 */
class ZeldaActivity : SDLActivity() {

    private lateinit var gameFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        val root = File(filesDir, "openzelda")
        gameFile = File(root, "game.mokoi")
        installContent(root)
        super.onCreate(savedInstanceState)

        // On-screen d-pad and buttons over the game surface.
        addContentView(
            TouchControlsView(this),
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
    }

    override fun getLibraries(): Array<String> = arrayOf("SDL2", "SDL2_mixer", "luxengine")

    /**
     * Becomes argv[1] of the engine's main(). We pass the game's folder with a trailing slash, which is the
     * engine's normal convention (MokoiGame::ReadType appends "game.mokoi" itself).
     */
    override fun getArguments(): Array<String> = arrayOf(gameFile.parentFile!!.absolutePath + "/")

    private fun installContent(root: File) {
        val stamp = File(root, ".installed-version")
        // Changes on every install/update (works on all API levels, unlike longVersionCode).
        val version = packageManager.getPackageInfo(packageName, 0).lastUpdateTime.toString()
        if (gameFile.exists() && stamp.exists() && stamp.readText() == version) return

        root.deleteRecursively()
        root.mkdirs()
        copyAssetDir(assets, "openzelda", root)
        stamp.writeText(version)
    }

    private fun copyAssetDir(am: AssetManager, assetPath: String, target: File) {
        val children = am.list(assetPath) ?: emptyArray()
        if (children.isEmpty()) {
            // A file (or an empty directory, which assets cannot represent).
            target.parentFile?.mkdirs()
            am.open(assetPath).use { input -> target.outputStream().use { input.copyTo(it) } }
            return
        }
        target.mkdirs()
        for (name in children) copyAssetDir(am, "$assetPath/$name", File(target, name))
    }
}
