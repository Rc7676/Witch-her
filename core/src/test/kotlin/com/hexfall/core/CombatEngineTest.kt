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
        EnemyDef("dummy", "Dummy", hp, hp) { _, _, _ -> EnemyMove.Attack(damage) }

    private fun engineWith(run: RunState = newRun(), vararg enemies: EnemyDef) =
        CombatEngine(run, enemies.toList(), Random(7))

    @Test
    fun `starting hand has five cards and three energy`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(20)))
        assertEquals(5, engine.hand.size)
        assertEquals(3, engine.energy)
        assertEquals(5, engine.drawPile.size)
    }

    @Test
    fun `playing an attack damages the target and spends energy`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(20)))
        val bolt = engine.hand.first { it.def.id.startsWith("hex_bolt") }
        val enemyHpBefore = engine.enemies[0].hp
        assertTrue(engine.playCard(bolt, 0))
        assertEquals(enemyHpBefore - 6, engine.enemies[0].hp)
        assertEquals(2, engine.energy)
        assertTrue(bolt in engine.discardPile)
        assertFalse(bolt in engine.hand)
    }

    @Test
    fun `single-target card without a target is rejected`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(20)))
        val bolt = engine.hand.first { it.def.id.startsWith("hex_bolt") }
        assertFalse(engine.playCard(bolt, null))
        assertEquals(3, engine.energy)
    }

    @Test
    fun `block absorbs enemy damage`() {
        val run = newRun()
        val engine = engineWith(run, dummyEnemy(50, damage = 4))
        val ward = engine.hand.firstOrNull { it.def.id.startsWith("ward") }
        if (ward != null) {
            engine.playCard(ward, null)
            assertEquals(5, engine.player.block)
            val hpBefore = engine.player.hp
            engine.endTurn()
            // 4 damage fully absorbed by 5 block
            assertEquals(hpBefore, engine.player.hp)
        }
    }

    @Test
    fun `killing all enemies wins the combat and syncs hp to the run`() {
        val run = newRun()
        val engine = engineWith(run, dummyEnemy(6))
        val bolt = engine.hand.first { it.def.id.startsWith("hex_bolt") }
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
    fun `vulnerable increases damage by fifty percent`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(50)))
        val enemy = engine.enemies[0]
        enemy.applyStatus(StatusType.VULNERABLE, 2)
        assertEquals(9, engine.attackDamage(engine.player, enemy, 6))
    }

    @Test
    fun `weak reduces damage by twenty-five percent`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(50)))
        engine.player.applyStatus(StatusType.WEAK, 1)
        assertEquals(4, engine.attackDamage(engine.player, engine.enemies[0], 6))
    }

    @Test
    fun `strength adds to damage`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(50)))
        engine.player.applyStatus(StatusType.STRENGTH, 3)
        assertEquals(9, engine.attackDamage(engine.player, engine.enemies[0], 6))
    }

    @Test
    fun `poison ticks on the enemy turn and decrements`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(50)))
        val enemy = engine.enemies[0]
        enemy.applyStatus(StatusType.POISON, 3)
        val hpBefore = enemy.hp
        engine.endTurn()
        assertEquals(hpBefore - 3, enemy.hp)
        assertEquals(2, enemy.statusAmount(StatusType.POISON))
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
        run.addCard(CardLibrary.witchsBrew)
        val engine = CombatEngine(run, listOf(dummyEnemy(500)), Random(3))
        var brew = engine.hand.firstOrNull { it.def.id == "witchs_brew" }
        var guard = 0
        while (brew == null && guard++ < 20) {
            engine.endTurn()
            brew = engine.hand.firstOrNull { it.def.id == "witchs_brew" }
        }
        assertNotNull(brew, "Witch's Brew should eventually be drawn")
        val energyBefore = engine.energy
        engine.playCard(brew, null)
        assertEquals(energyBefore + 2, engine.energy)
        assertTrue(brew in engine.exhaustPile)
    }

    @Test
    fun `curses cannot be played`() {
        val run = newRun()
        val engine = engineWith(run, dummyEnemy(50))
        val curse = CardInstance(999, CardLibrary.burden)
        engine.hand += curse
        assertFalse(engine.canPlay(curse))
        assertFalse(engine.playCard(curse, null))
    }

    @Test
    fun `thorns retaliate against attackers`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(50, damage = 3)))
        engine.player.applyStatus(StatusType.THORNS, 2)
        val enemy = engine.enemies[0]
        val hpBefore = enemy.hp
        engine.endTurn()
        assertEquals(hpBefore - 2, enemy.hp)
    }

    @Test
    fun `relic ember stone grants four energy`() {
        val run = newRun()
        run.addRelic(RelicLibrary.emberStone)
        val engine = engineWith(run, dummyEnemy(50))
        assertEquals(4, engine.energy)
    }

    @Test
    fun `intent damage accounts for enemy strength`() {
        val engine = engineWith(enemies = arrayOf(dummyEnemy(50, damage = 5)))
        val enemy = engine.enemies[0]
        enemy.applyStatus(StatusType.STRENGTH, 2)
        assertEquals(7, engine.intentDamage(enemy))
    }

    @Test
    fun `defeated enemies are removed mid-multihit`() {
        val run = newRun()
        run.addCard(CardLibrary.fireWhip)
        val engine = CombatEngine(run, listOf(dummyEnemy(3)), Random(5))
        var whip = engine.hand.firstOrNull { it.def.id == "fire_whip" }
        var guard = 0
        while (whip == null && guard++ < 20) {
            engine.endTurn()
            whip = engine.hand.firstOrNull { it.def.id == "fire_whip" }
        }
        assertNotNull(whip)
        engine.playCard(whip, 0)
        assertEquals(CombatResult.VICTORY, engine.result)
        assertNull(engine.enemies.firstOrNull())
    }
}
