package com.hexfall.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CombatEngineTest {

    private fun newRun(seed: Long = 42L) = RunState(seed)

    private fun dummyEnemy(hp: Int, damage: Int = 5) =
        EnemyDef("dummy", "Dummy", hp, hp) { _, _, _, _ -> EnemyMove.Attack(damage) }

    private fun engineWith(run: RunState = newRun(), vararg enemies: EnemyDef) =
        CombatEngine(run, enemies.toList(), Random(7))

    /** Cycles turns until a card with [id] is in hand; fails after 20 turns. */
    private fun drawUntil(engine: CombatEngine, id: String): CardInstance {
        var card = engine.hand.firstOrNull { it.def.id == id }
        var guard = 0
        while (card == null && guard++ < 20) {
            engine.endTurn()
            card = engine.hand.firstOrNull { it.def.id == id }
        }
        assertNotNull(card, "$id should eventually be drawn")
        return card
    }

    @Test
    fun `starting hand has five cards and three mana at waxing moon`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(20)))
        assertEquals(5, engine.hand.size)
        assertEquals(3, engine.mana)
        assertEquals(MoonPhase.WAXING, engine.moon)
    }

    @Test
    fun `the moon turns each player turn`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(500, damage = 0)))
        assertEquals(MoonPhase.WAXING, engine.moon)
        engine.endTurn()
        assertEquals(MoonPhase.FULL, engine.moon)
        engine.endTurn()
        assertEquals(MoonPhase.WANING, engine.moon)
        engine.endTurn()
        assertEquals(MoonPhase.NEW, engine.moon)
        engine.endTurn()
        assertEquals(MoonPhase.WAXING, engine.moon)
    }

    @Test
    fun `moonbolt gains its full moon bonus`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(500, damage = 0)))
        engine.endTurn() // moon is now FULL
        assertEquals(MoonPhase.FULL, engine.moon)
        val bolt = drawUntil(engine, "moonbolt")
        val hpBefore = engine.enemies[0].hp
        assertTrue(engine.playCard(bolt, 0))
        assertEquals(hpBefore - 8, engine.enemies[0].hp) // 5 base + 3 full moon
    }

    @Test
    fun `playing an attack damages the target and spends mana`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(20)))
        val bolt = drawUntil(engine, "moonbolt")
        val enemyHpBefore = engine.enemies[0].hp
        val expected = if (engine.moon == MoonPhase.FULL) 8 else 5
        val manaBefore = engine.mana
        assertTrue(engine.playCard(bolt, 0))
        assertEquals(enemyHpBefore - expected, engine.enemies[0].hp)
        assertEquals(manaBefore - 1, engine.mana)
        assertTrue(bolt in engine.discardPile)
        assertFalse(bolt in engine.hand)
    }

    @Test
    fun `single-target card without a target is rejected`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(20)))
        val bolt = drawUntil(engine, "moonbolt")
        val manaBefore = engine.mana
        assertFalse(engine.playCard(bolt, null))
        assertEquals(manaBefore, engine.mana)
    }

    @Test
    fun `ward absorbs enemy damage`() {
        val run = newRun()
        val engine = engineWith(run, dummyEnemy(500, damage = 4))
        val veil = drawUntil(engine, "veil")
        engine.playCard(veil, null)
        assertEquals(5, engine.player.ward)
        val hpBefore = engine.player.hp
        engine.endTurn()
        assertEquals(hpBefore, engine.player.hp)
    }

    @Test
    fun `bloodprick costs hp instead of mana`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(500, damage = 0)))
        val prick = drawUntil(engine, "bloodprick")
        val hpBefore = engine.player.hp
        val manaBefore = engine.mana
        val enemyHp = engine.enemies[0].hp
        assertTrue(engine.playCard(prick, 0))
        assertEquals(hpBefore - 2, engine.player.hp)
        assertEquals(manaBefore, engine.mana)
        assertEquals(enemyHp - 7, engine.enemies[0].hp)
    }

    @Test
    fun `killing all enemies wins the combat and syncs hp to the run`() {
        val run = newRun()
        val engine = engineWith(run, dummyEnemy(5))
        val bolt = drawUntil(engine, "moonbolt")
        engine.playCard(bolt, 0)
        assertEquals(CombatResult.VICTORY, engine.result)
        assertEquals(engine.player.hp, run.hp)
    }

    @Test
    fun `player death is a defeat`() {
        val run = newRun()
        run.hp = 3
        val engine = engineWith(run, dummyEnemy(500, damage = 50))
        engine.endTurn()
        assertEquals(CombatResult.DEFEAT, engine.result)
        assertEquals(0, run.hp)
    }

    @Test
    fun `hexed adds flat damage per hit`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(50)))
        val enemy = engine.enemies[0]
        enemy.applyStatus(StatusType.HEXED, 2)
        assertEquals(8, engine.attackDamage(engine.player, enemy, 6))
    }

    @Test
    fun `chill subtracts flat damage per hit`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(50)))
        engine.player.applyStatus(StatusType.CHILL, 2)
        assertEquals(4, engine.attackDamage(engine.player, engine.enemies[0], 6))
    }

    @Test
    fun `spellpower adds to damage`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(50)))
        engine.player.applyStatus(StatusType.SPELLPOWER, 3)
        assertEquals(9, engine.attackDamage(engine.player, engine.enemies[0], 6))
    }

    @Test
    fun `venom hits then halves`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(50, damage = 0)))
        val enemy = engine.enemies[0]
        enemy.applyStatus(StatusType.VENOM, 9)
        val hpBefore = enemy.hp
        engine.endTurn()
        assertEquals(hpBefore - 9, enemy.hp)
        assertEquals(4, enemy.statusAmount(StatusType.VENOM))
        engine.endTurn()
        assertEquals(hpBefore - 13, enemy.hp)
        assertEquals(2, enemy.statusAmount(StatusType.VENOM))
    }

    @Test
    fun `doom erupts at the threshold`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(50, damage = 0)))
        val enemy = engine.enemies[0]
        engine.applyStatusChecked(enemy, StatusType.DOOM, 4)
        assertEquals(4, enemy.statusAmount(StatusType.DOOM))
        val hpBefore = enemy.hp
        engine.applyStatusChecked(enemy, StatusType.DOOM, 3)
        // 7 stacks >= 6: erupts for 21, resets to 0.
        assertEquals(0, enemy.statusAmount(StatusType.DOOM))
        assertEquals(hpBefore - 21, enemy.hp)
    }

    @Test
    fun `hand is discarded at end of turn and redrawn`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(500)))
        engine.endTurn()
        assertEquals(5, engine.hand.size)
        assertEquals(2, engine.turn)
    }

    @Test
    fun `draw reshuffles the discard pile when the draw pile empties`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(500, damage = 0)))
        repeat(3) { engine.endTurn() }
        assertEquals(5, engine.hand.size)
        assertEquals(10, engine.hand.size + engine.drawPile.size + engine.discardPile.size)
    }

    @Test
    fun `exhausted cards leave the deck cycle`() {
        val run = newRun()
        run.addCard(CardLibrary.bloodTithe)
        val engine = CombatEngine(run, listOf(dummyEnemy(500, damage = 0)), Random(3))
        val tithe = drawUntil(engine, "blood_tithe")
        val manaBefore = engine.mana
        engine.playCard(tithe, null)
        assertEquals(manaBefore + 2, engine.mana)
        assertTrue(tithe in engine.exhaustPile)
    }

    @Test
    fun `curses cannot be played`() {
        val run = newRun()
        val engine = engineWith(run, dummyEnemy(50))
        val curse = CardInstance(999, CardLibrary.graveDust)
        engine.hand += curse
        assertFalse(engine.canPlay(curse))
        assertFalse(engine.playCard(curse, null))
    }

    @Test
    fun `brambles retaliate against attackers`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(50, damage = 3)))
        engine.player.applyStatus(StatusType.BRAMBLES, 2)
        val enemy = engine.enemies[0]
        val hpBefore = enemy.hp
        engine.endTurn()
        assertEquals(hpBefore - 2, enemy.hp)
    }

    @Test
    fun `ashen hourglass grants four mana`() {
        val run = newRun()
        run.addRelic(RelicLibrary.ashenHourglass)
        val engine = engineWith(run, dummyEnemy(50))
        assertEquals(4, engine.mana)
    }

    @Test
    fun `grave robber steals gold`() {
        val run = newRun()
        run.gold = 50
        val robber = EnemyDef("thief", "Thief", 500, 500) { _, _, _, _ ->
            EnemyMove.Steal(2, 10)
        }
        val engine = CombatEngine(run, listOf(robber), Random(2))
        engine.endTurn()
        assertEquals(40, run.gold)
    }

    @Test
    fun `moonfang wolf hits harder under the full moon`() {
        val rng = Random(1)
        val self = EnemyCombatant(EnemyLibrary.moonfangWolf, 20)
        val fullMove = EnemyLibrary.moonfangWolf.ai(0, rng, self, MoonPhase.FULL)
        val newMove = EnemyLibrary.moonfangWolf.ai(0, rng, self, MoonPhase.NEW)
        assertEquals(13, (fullMove as EnemyMove.Attack).damage)
        assertEquals(8, (newMove as EnemyMove.Attack).damage)
    }

    @Test
    fun `intent damage accounts for enemy spellpower`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(50, damage = 5)))
        val enemy = engine.enemies[0]
        enemy.applyStatus(StatusType.SPELLPOWER, 2)
        assertEquals(7, engine.intentDamage(enemy))
    }

    @Test
    fun `defeated enemies are removed mid-multihit`() {
        val run = newRun()
        run.addCard(CardLibrary.twinSparks)
        val engine = CombatEngine(run, listOf(dummyEnemy(3, damage = 0)), Random(5))
        val sparks = drawUntil(engine, "twin_sparks")
        engine.playCard(sparks, 0)
        assertEquals(CombatResult.VICTORY, engine.result)
        assertNull(engine.enemies.firstOrNull())
    }

    @Test
    fun `lunar tide advances the moon mid-turn`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(500, damage = 0)))
        val tide = drawUntil(engine, "lunar_tide")
        val phaseBefore = engine.moon
        engine.playCard(tide, null)
        assertEquals(phaseBefore.next(), engine.moon)
    }
}
