package com.hexfall.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Headless playtests: a simple greedy bot plays complete runs end-to-end.
 * Guards against crashes, non-terminating combats, and broken invariants,
 * and keeps a loose grip on game balance.
 */
class RunSimulationTest {

    private data class RunOutcome(val victory: Boolean, val floors: Int)

    /** Greedy policy: block when threatened, dump attacks into enemy 0. */
    private fun autoCombat(run: RunState, enemies: List<EnemyDef>): CombatResult {
        val engine = CombatEngine(run, enemies, run.rng)
        var safety = 0
        while (engine.result == null && safety++ < 300) {
            var played = true
            while (played && engine.result == null) {
                played = false
                val playable = engine.hand.filter { engine.canPlay(it) }
                val card = playable.maxByOrNull { it.def.cost * 10 + it.def.effects.size }
                    ?: break
                val target = if (card.def.needsTarget) 0 else null
                if (engine.playCard(card, target)) played = true
            }
            if (engine.result == null) engine.endTurn()

            assertTrue(engine.player.hp <= engine.player.maxHp, "hp above max")
            assertTrue(engine.energy >= 0, "negative energy")
            engine.enemies.forEach { assertTrue(it.alive, "dead enemy lingering") }
        }
        assertNotNull(engine.result, "combat did not terminate in 300 turns")
        return engine.result!!
    }

    private fun simulateRun(seed: Long): RunOutcome {
        val run = RunState(seed)
        val rng = Random(seed + 1_000_000)

        var guard = 0
        while (guard++ < 100) {
            val choices = run.map.available(run.currentNodeId)
            assertTrue(choices.isNotEmpty(), "seed $seed: no reachable nodes")
            val node = choices[rng.nextInt(choices.size)]
            run.moveTo(node)

            when (node.type) {
                NodeType.MONSTER, NodeType.ELITE, NodeType.BOSS -> {
                    val elite = node.type == NodeType.ELITE
                    val enemies = when (node.type) {
                        NodeType.ELITE -> EnemyLibrary.eliteEncounter(run.rng)
                        NodeType.BOSS -> EnemyLibrary.bossEncounter()
                        else -> EnemyLibrary.normalEncounter(node.row, run.rng)
                    }
                    val result = autoCombat(run, enemies)
                    if (result == CombatResult.DEFEAT) {
                        return RunOutcome(victory = false, floors = run.floorsClimbed)
                    }
                    run.afterCombatRelics(mutableListOf())
                    if (node.type == NodeType.BOSS) {
                        return RunOutcome(victory = true, floors = run.floorsClimbed)
                    }
                    val reward = CombatReward.forCombat(run, run.rng, elite)
                    run.gold += reward.gold
                    reward.cardChoices.firstOrNull { it.type == CardType.ATTACK }
                        ?.let { run.addCard(it) }
                    reward.relic?.let { run.addRelic(it) }
                }
                NodeType.EVENT -> {
                    val event = EventLibrary.random(run.rng)
                    val open = event.choices.filter { it.available(run) }
                    assertTrue(open.isNotEmpty(), "event ${event.id} has no available choice")
                    open[rng.nextInt(open.size)].resolve(run, run.rng)
                }
                NodeType.REST -> run.hp = (run.hp + (run.maxHp * 3) / 10).coerceAtMost(run.maxHp)
                NodeType.SHOP -> {
                    val shop = ShopInventory(run, run.rng)
                    shop.relics.firstOrNull { run.gold >= it.price }
                        ?.let { shop.buyRelic(run, it) }
                    shop.cards.firstOrNull { run.gold >= it.price }
                        ?.let { shop.buyCard(run, it) }
                }
                NodeType.TREASURE -> {
                    RelicLibrary.randomNew(run.rng, run.relics)?.let { run.addRelic(it) }
                    run.gold += 20
                }
            }

            assertTrue(run.hp in 0..run.maxHp, "seed $seed: hp ${run.hp} out of range")
            assertTrue(run.gold >= 0, "seed $seed: negative gold")
            assertTrue(run.deck.isNotEmpty(), "seed $seed: empty deck")
        }
        error("seed $seed: run did not terminate")
    }

    @Test
    fun `two hundred simulated runs complete cleanly`() {
        var wins = 0
        var totalFloors = 0
        var deaths = 0
        val runs = 200

        for (seed in 0 until runs) {
            val outcome = simulateRun(seed.toLong())
            if (outcome.victory) wins++ else deaths++
            totalFloors += outcome.floors
        }

        val avgFloors = totalFloors.toDouble() / runs
        println("Simulation: $wins/$runs wins, avg floors ${"%.1f".format(avgFloors)}")

        // Balance guardrails for a dumb greedy bot (deterministic seeds):
        // it should usually survive deep into the act, win sometimes, but
        // not most of the time. Retune enemies if these trip.
        assertTrue(avgFloors > 8.0, "game too hard: bot averages $avgFloors floors")
        assertTrue(wins > 0, "game too hard: bot never wins")
        assertTrue(wins < runs / 2, "game too easy: bot wins $wins/$runs")
    }

    @Test
    fun `all card effects resolve against every enemy type`() {
        // Every playable card is force-played at least once against each
        // enemy, exercising each effect path.
        for (enemyDef in listOf(
            EnemyLibrary.gloomRat, EnemyLibrary.cultist, EnemyLibrary.stoneGolem,
            EnemyLibrary.paleLich,
        )) {
            for (cardDef in CardLibrary.rewardPool) {
                val run = RunState(99L)
                run.deck.clear()
                repeat(10) { run.addCard(cardDef) }
                val engine = CombatEngine(run, listOf(enemyDef), Random(1))
                val card = engine.hand.first()
                val ok = engine.playCard(card, if (cardDef.needsTarget) 0 else null)
                assertTrue(ok, "${cardDef.id} failed to play against ${enemyDef.id}")
            }
        }
    }

    @Test
    fun `every enemy ai produces valid moves for twenty turns`() {
        val allEnemies = listOf(
            EnemyLibrary.gloomRat, EnemyLibrary.boneSprite, EnemyLibrary.marshSlime,
            EnemyLibrary.cultist, EnemyLibrary.direWolf, EnemyLibrary.cryptSpider,
            EnemyLibrary.hollowKnight, EnemyLibrary.hedgeWitch, EnemyLibrary.stoneGolem,
            EnemyLibrary.boneKnight, EnemyLibrary.paleLich,
        )
        val rng = Random(5)
        for (def in allEnemies) {
            val enemy = EnemyCombatant(def, def.maxHp)
            for (turn in 0 until 20) {
                val move = def.ai(turn, rng, enemy)
                when (move) {
                    is EnemyMove.Attack -> assertTrue(move.damage > 0 && move.times > 0)
                    is EnemyMove.Defend -> assertTrue(move.block > 0)
                    is EnemyMove.AttackDefend -> assertTrue(move.damage > 0 && move.block > 0)
                    is EnemyMove.Buff -> assertTrue(move.amount > 0)
                    is EnemyMove.Debuff -> assertTrue(move.amount > 0)
                    is EnemyMove.HealSelf -> assertTrue(move.amount > 0)
                }
            }
        }
    }
}
