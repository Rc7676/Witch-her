package com.hexfall.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RunSnapshotTest {

    /** Plays a few nodes so the snapshot covers a run in progress. */
    private fun advancedRun(seed: Long = 5L): RunState {
        val state = RunState(seed)
        state.hp = 41
        state.gold = 237
        state.maxHp = 82
        state.addCard(CardLibrary.moonfall)
        state.addCard(CardLibrary.venomKiss)
        state.upgradeCard(state.deck.first { it.def.id == "moonbolt" })
        state.addRelic(RelicLibrary.serpentFang)
        state.addRelic(RelicLibrary.owlQuill)
        state.moveTo(state.map.available(null).first())
        return state
    }

    @Test
    fun `round trip preserves the run`() {
        val original = advancedRun()
        val restored = RunSnapshot.decode(RunSnapshot.encode(original))
        assertNotNull(restored)

        assertEquals(original.seed, restored.seed)
        assertEquals(original.hp, restored.hp)
        assertEquals(original.maxHp, restored.maxHp)
        assertEquals(original.gold, restored.gold)
        assertEquals(original.currentNodeId, restored.currentNodeId)
        assertEquals(original.floorsClimbed, restored.floorsClimbed)
        assertEquals(
            original.deck.map { it.def.id }.sorted(),
            restored.deck.map { it.def.id }.sorted(),
        )
        assertEquals(
            original.relics.map { it.id }.sorted(),
            restored.relics.map { it.id }.sorted(),
        )
    }

    @Test
    fun `restored map is identical to the original`() {
        val original = advancedRun(seed = 99L)
        val restored = assertNotNull(RunSnapshot.decode(RunSnapshot.encode(original)))

        assertEquals(original.map.nodes.size, restored.map.nodes.size)
        assertEquals(original.map.bossId, restored.map.bossId)
        original.map.nodes.zip(restored.map.nodes).forEach { (a, b) ->
            assertEquals(a.id, b.id)
            assertEquals(a.row, b.row)
            assertEquals(a.col, b.col)
            assertEquals(a.type, b.type)
            assertEquals(a.next, b.next)
        }
        // The same rooms must still be reachable from where the witch stands.
        assertEquals(
            original.map.available(original.currentNodeId).map { it.id }.sorted(),
            restored.map.available(restored.currentNodeId).map { it.id }.sorted(),
        )
    }

    @Test
    fun `restoring does not re-apply relic pickup bonuses`() {
        val state = RunState(3L)
        state.addRelic(RelicLibrary.giantsTooth) // +10 max HP on pickup
        assertEquals(80, state.maxHp)

        val restored = assertNotNull(RunSnapshot.decode(RunSnapshot.encode(state)))
        assertEquals(80, restored.maxHp)
        assertTrue(restored.hasRelic("giants_tooth"))
    }

    @Test
    fun `card upgrades survive the round trip`() {
        val state = RunState(11L)
        state.deck.forEach { state.upgradeCard(it) }
        val restored = assertNotNull(RunSnapshot.decode(RunSnapshot.encode(state)))
        assertTrue(restored.deck.all { it.def.upgraded })
    }

    @Test
    fun `restored deck instances have unique ids`() {
        val restored = assertNotNull(RunSnapshot.decode(RunSnapshot.encode(advancedRun())))
        assertEquals(restored.deck.size, restored.deck.map { it.uid }.distinct().size)
    }

    @Test
    fun `a restored run keeps playing`() {
        val restored = assertNotNull(RunSnapshot.decode(RunSnapshot.encode(advancedRun())))
        val engine = CombatEngine(restored, EnemyLibrary.bossEncounter(), Random(1))
        // The restored Owl Feather Quill is in effect: 6 cards, not 5.
        assertTrue(restored.hasRelic("owl_quill"))
        assertEquals(6, engine.hand.size)
        val card = engine.hand.first { engine.canPlay(it) }
        assertTrue(engine.playCard(card, if (card.def.needsTarget) 0 else null))
    }

    @Test
    fun `garbage and truncated saves are rejected`() {
        assertNull(RunSnapshot.decode(""))
        assertNull(RunSnapshot.decode("not a save file"))
        assertNull(RunSnapshot.decode("hexfall-run-1\nseed=notanumber"))
        assertNull(RunSnapshot.decode("hexfall-run-1\nmaxHp=70\nhp=70"))
        // Header present but body cut off mid-write.
        val truncated = RunSnapshot.encode(advancedRun()).take(40)
        assertNull(RunSnapshot.decode(truncated))
    }

    @Test
    fun `saves referencing unknown content are rejected`() {
        val text = RunSnapshot.encode(advancedRun())
            .replace("deck=", "deck=spell_from_a_future_version,")
        assertNull(RunSnapshot.decode(text))

        val relicText = RunSnapshot.encode(advancedRun())
            .replace("relics=", "relics=charm_that_no_longer_exists,")
        assertNull(RunSnapshot.decode(relicText))
    }

    @Test
    fun `impossible states are rejected`() {
        val dead = RunSnapshot.encode(advancedRun()).replace("hp=41", "hp=0")
        assertNull(RunSnapshot.decode(dead))

        val overheal = RunSnapshot.encode(advancedRun()).replace("hp=41", "hp=999")
        assertNull(RunSnapshot.decode(overheal))

        val broke = RunSnapshot.encode(advancedRun()).replace("gold=237", "gold=-5")
        assertNull(RunSnapshot.decode(broke))

        val emptyDeck = RunSnapshot.encode(advancedRun()).replace(
            Regex("deck=.*"), "deck=",
        )
        assertNull(RunSnapshot.decode(emptyDeck))
    }

    @Test
    fun `a fresh run at the start of the climb round trips`() {
        val fresh = RunState(7L)
        val restored = assertNotNull(RunSnapshot.decode(RunSnapshot.encode(fresh)))
        assertNull(restored.currentNodeId)
        assertEquals(0, restored.floorsClimbed)
        assertEquals(10, restored.deck.size)
        assertEquals(fresh.map.available(null).size, restored.map.available(null).size)
    }
}
