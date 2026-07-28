package com.hexfall.core

/**
 * Serializes a run to plain text so a climb survives the app being killed.
 *
 * The act map is *derived* from the seed (RunState generates it from a fresh
 * Random(seed) before anything else consumes the stream), so only the seed is
 * stored and the identical map is rebuilt on load. Future randomness diverges
 * from the original session, which is harmless.
 *
 * Snapshots are taken between map nodes, never mid-combat: resuming puts the
 * witch back on the map with the state she had before entering the room.
 */
object RunSnapshot {

    private const val HEADER = "hexfall-run-1"

    fun encode(run: RunState): String = buildString {
        appendLine(HEADER)
        appendLine("seed=${run.seed}")
        appendLine("maxHp=${run.maxHp}")
        appendLine("hp=${run.hp}")
        appendLine("gold=${run.gold}")
        appendLine("node=${run.currentNodeId ?: -1}")
        appendLine("floors=${run.floorsClimbed}")
        appendLine("deck=${run.deck.joinToString(",") { it.def.id }}")
        appendLine("relics=${run.relics.joinToString(",") { it.id }}")
    }

    /** Returns null for anything unreadable — the caller then starts fresh. */
    fun decode(text: String): RunState? {
        return try {
            val lines = text.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toList()
            if (lines.firstOrNull() != HEADER) return null

            val fields = lines.drop(1).mapNotNull { line ->
                val split = line.indexOf('=')
                if (split <= 0) null else line.substring(0, split) to line.substring(split + 1)
            }.toMap()

            val seed = fields["seed"]?.toLongOrNull() ?: return null
            val state = RunState(seed)
            state.maxHp = fields["maxHp"]?.toIntOrNull() ?: return null
            state.hp = fields["hp"]?.toIntOrNull() ?: return null
            state.gold = fields["gold"]?.toIntOrNull() ?: return null
            state.floorsClimbed = fields["floors"]?.toIntOrNull() ?: 0

            val nodeId = fields["node"]?.toIntOrNull() ?: -1
            state.currentNodeId =
                if (state.map.nodes.any { it.id == nodeId }) nodeId else null

            // Unknown ids throw and are caught below, so a save written by a
            // different content version is rejected rather than half-loaded.
            state.deck.clear()
            fields["deck"].orEmpty().split(",").filter { it.isNotBlank() }.forEach { id ->
                state.addCard(CardLibrary.byId(id))
            }
            state.relics.clear()
            fields["relics"].orEmpty().split(",").filter { it.isNotBlank() }.forEach { id ->
                // Added directly: pickup bonuses are already baked into maxHp.
                state.relics += RelicLibrary.byId(id)
            }

            val sane = state.deck.isNotEmpty() &&
                state.maxHp > 0 &&
                state.hp in 1..state.maxHp &&
                state.gold >= 0
            if (sane) state else null
        } catch (e: Exception) {
            null
        }
    }
}
