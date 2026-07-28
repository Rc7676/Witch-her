package com.hexfall.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.io.File

/**
 * Where a run in progress is kept between app launches. Every operation is
 * best-effort: losing a save is survivable, crashing on one is not.
 */
class RunStore(private val file: File) {

    fun save(text: String) {
        runCatching {
            // Write to a temp file first so a kill mid-write cannot leave a
            // half-written save behind.
            val temp = File(file.parentFile, file.name + ".tmp")
            temp.writeText(text)
            if (!temp.renameTo(file)) {
                file.writeText(text)
                temp.delete()
            }
        }
    }

    fun load(): String? =
        runCatching { if (file.exists()) file.readText() else null }.getOrNull()

    fun clear() {
        runCatching { if (file.exists()) file.delete() }
    }

    fun exists(): Boolean = runCatching { file.exists() }.getOrDefault(false)
}

/** Supplies the [RunStore] to the ViewModel, which outlives configuration changes. */
class GameViewModelFactory(private val store: RunStore?) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return GameViewModel(store) as T
    }
}
