package com.hexfall.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Balance harness. Two bots play the same 200 seeds:
 *
 *  - GREEDY dumps the most expensive playable card into enemy 0, ignoring
 *    the moon, incoming damage, and whether anything is about to die.
 *  - SKILLED reads intents, finishes off enemies it can kill, blocks when a
 *    hit would be lethal, and saves lunar spells for the Full Moon.
 *
 * The gap between them is the game's skill ceiling. If it collapses, the
 * numbers have stopped rewarding play and need retuning — that is what the
 * assertions here defend.
 */
class BalanceTest {

    private data class Outcome(
        val victory: Boolean,
        val floors: Int,
        val reachedBoss: Boolean,
        /** Boss HP left when the witch died, for gauging how close it was. */
        val bossHpLeft: Int = 0,
    )

    // --- Evaluation helpers ----------------------------------------------

    /** Damage this card would deal to [enemy] if cast right now. */
    private fun damageOf(engine: CombatEngine, card: CardInstance, enemy: EnemyCombatant): Int {
        var total = 0
        for (effect in card.def.effects) {
            when (effect) {
                is CardEffect.Damage -> {
                    val bonus = if (engine.moon == MoonPhase.FULL) effect.fullMoonBonus else 0
                    total += engine.attackDamage(
                        engine.player, enemy, effect.amount + bonus,
                    ) * effect.times
                }
                is CardEffect.DamageAll -> {
                    val bonus = if (engine.moon == MoonPhase.FULL) effect.fullMoonBonus else 0
                    total += engine.attackDamage(engine.player, enemy, effect.amount + bonus)
                }
                else -> Unit
            }
        }
        return total
    }

    private fun wardOf(engine: CombatEngine, card: CardInstance): Int {
        var total = 0
        for (effect in card.def.effects) {
            if (effect is CardEffect.GainWard) {
                val bonus = if (engine.moon == MoonPhase.FULL) effect.fullMoonBonus else 0
                total += engine.wardGain(effect.amount + bonus)
            }
        }
        return total
    }

    /** Damage the enemies have telegraphed for this turn. */
    private fun incoming(engine: CombatEngine): Int =
        engine.enemies.sumOf { enemy ->
            when (val move = enemy.nextMove) {
                is EnemyMove.Attack -> (engine.intentDamage(enemy) ?: 0) * move.times
                is EnemyMove.AttackGuard -> engine.intentDamage(enemy) ?: 0
                is EnemyMove.Steal -> engine.intentDamage(enemy) ?: 0
                else -> 0
            }
        }

    // --- Policies ---------------------------------------------------------

    private fun greedyTurn(engine: CombatEngine) {
        while (engine.result == null) {
            val card = engine.hand.filter { engine.canPlay(it) }
                .maxByOrNull { it.def.cost * 10 + it.def.effects.size } ?: break
            if (!engine.playCard(card, if (card.def.needsTarget) 0 else null)) break
        }
    }

    private fun skilledTurn(engine: CombatEngine) {
        while (engine.result == null) {
            val playable = engine.hand.filter { engine.canPlay(it) }
            if (playable.isEmpty()) break

            // 1. Free card draw and mana are pure profit — take them first.
            val cantrip = playable.firstOrNull { card ->
                card.def.cost == 0 && card.def.effects.any {
                    it is CardEffect.Draw || it is CardEffect.GainMana
                }
            }
            if (cantrip != null) {
                if (!engine.playCard(cantrip, null)) break
                continue
            }

            // 2. Kill anything that can die now: a dead enemy deals no damage.
            var finished = false
            for (card in playable.filter { it.def.needsTarget }) {
                val index = engine.enemies.indexOfFirst { enemy ->
                    damageOf(engine, card, enemy) >= enemy.hp + enemy.ward
                }
                if (index >= 0) {
                    if (!engine.playCard(card, index)) return
                    finished = true
                    break
                }
            }
            if (finished) continue

            // 3. Survive the telegraphed hit before doing anything greedy.
            val threat = (incoming(engine) - engine.player.ward).coerceAtLeast(0)
            if (threat >= engine.player.hp) {
                val shield = playable.filter { wardOf(engine, it) > 0 }
                    .maxByOrNull { wardOf(engine, it) }
                if (shield != null) {
                    if (!engine.playCard(shield, null)) break
                    continue
                }
            }

            // 4. Hit the enemy closest to dying, with the best spell for the
            //    current phase (damageOf already folds in the Full Moon bonus).
            val target = engine.enemies.indices.minByOrNull {
                engine.enemies[it].hp + engine.enemies[it].ward
            }
            if (target == null) break
            val best = playable.maxByOrNull { damageOf(engine, it, engine.enemies[target]) }
            if (best != null && damageOf(engine, best, engine.enemies[target]) > 0) {
                if (!engine.playCard(best, if (best.def.needsTarget) target else null)) break
                continue
            }

            // 5. Nothing damaging left: spend the rest on buffs and ward.
            val filler = playable.firstOrNull()
            if (filler == null) break
            if (!engine.playCard(filler, if (filler.def.needsTarget) target else null)) break
        }
    }

    /** Runs a combat and reports the result plus the enemies' surviving HP. */
    private fun fight(
        run: RunState,
        enemies: List<EnemyDef>,
        skilled: Boolean,
    ): Pair<CombatResult, Int> {
        val engine = CombatEngine(run, enemies, run.rng)
        var safety = 0
        while (engine.result == null && safety++ < 300) {
            if (skilled) skilledTurn(engine) else greedyTurn(engine)
            if (engine.result == null) engine.endTurn()
        }
        assertNotNull(engine.result, "combat did not terminate")
        return engine.result!! to engine.enemies.sumOf { it.hp }
    }

    private fun simulate(seed: Long, skilled: Boolean): Outcome {
        val run = RunState(seed)
        val nav = Random(seed + 1_000_000)

        var guard = 0
        while (guard++ < 100) {
            val choices = run.map.available(run.currentNodeId)
            val node = choices[nav.nextInt(choices.size)]
            run.moveTo(node)

            when (node.type) {
                NodeType.MONSTER, NodeType.ELITE, NodeType.BOSS -> {
                    val isBoss = node.type == NodeType.BOSS
                    val enemies = when (node.type) {
                        NodeType.ELITE -> EnemyLibrary.eliteEncounter(run.rng)
                        NodeType.BOSS -> EnemyLibrary.bossEncounter()
                        else -> EnemyLibrary.normalEncounter(node.row, run.rng)
                    }
                    val (result, hpLeft) = fight(run, enemies, skilled)
                    if (result == CombatResult.DEFEAT) {
                        return Outcome(false, run.floorsClimbed, isBoss, if (isBoss) hpLeft else 0)
                    }
                    run.afterCombatRelics(mutableListOf())
                    if (isBoss) return Outcome(true, run.floorsClimbed, true)

                    val reward = CombatReward.forCombat(run, run.rng, node.type == NodeType.ELITE)
                    run.gold += reward.gold
                    reward.relic?.let { run.addRelic(it) }
                    // Both bots draft identically so this measures combat play
                    // alone, not deckbuilding.
                    reward.cardChoices.firstOrNull { it.type == CardType.SPELL_ATTACK }
                        ?.let { run.addCard(it) }
                }
                NodeType.EVENT -> {
                    val event = EventLibrary.random(run.rng)
                    val open = event.choices.filter { it.available(run) }
                    open[nav.nextInt(open.size)].resolve(run, run.rng)
                }
                NodeType.REST ->
                    run.hp = (run.hp + (run.maxHp * 3) / 10).coerceAtMost(run.maxHp)
                NodeType.SHOP -> {
                    val shop = ShopInventory(run, run.rng)
                    shop.relics.firstOrNull { run.gold >= it.price }?.let { shop.buyRelic(run, it) }
                    shop.cards.firstOrNull { run.gold >= it.price }?.let { shop.buyCard(run, it) }
                }
                NodeType.TREASURE -> {
                    RelicLibrary.randomNew(run.rng, run.relics)?.let { run.addRelic(it) }
                    run.gold += 20
                }
            }
        }
        error("seed $seed did not terminate")
    }

    @Test
    fun `skilled play beats brute force and the spire stays winnable`() {
        val runs = 200
        var greedyWins = 0
        var skilledWins = 0
        var greedyFloors = 0
        var skilledFloors = 0
        var skilledReachedBoss = 0
        var bossHpLeftTotal = 0
        var bossLosses = 0

        for (seed in 0 until runs) {
            val greedy = simulate(seed.toLong(), skilled = false)
            val skilled = simulate(seed.toLong(), skilled = true)
            if (greedy.victory) greedyWins++
            if (skilled.victory) skilledWins++
            greedyFloors += greedy.floors
            skilledFloors += skilled.floors
            if (skilled.reachedBoss) skilledReachedBoss++
            if (skilled.reachedBoss && !skilled.victory) {
                bossLosses++
                bossHpLeftTotal += skilled.bossHpLeft
            }
        }

        println(
            "Balance over $runs seeds:\n" +
                "  greedy  ${greedyWins * 100 / runs}% wins, " +
                "avg floor ${"%.1f".format(greedyFloors.toDouble() / runs)}\n" +
                "  skilled ${skilledWins * 100 / runs}% wins, " +
                "avg floor ${"%.1f".format(skilledFloors.toDouble() / runs)}\n" +
                "  skilled reached the boss ${skilledReachedBoss * 100 / runs}% of runs; " +
                "of $bossLosses boss losses the Queen averaged " +
                "${if (bossLosses > 0) bossHpLeftTotal / bossLosses else 0} HP remaining",
        )

        // Playing well must matter, and matter a lot. Stated as a ratio with
        // slack so ordinary retuning does not trip it on a knife edge.
        assertTrue(
            skilledWins * 2 >= greedyWins * 3,
            "skill barely helps: greedy $greedyWins vs skilled $skilledWins of $runs",
        )
        // A competent climber should reach the top a fair share of the time...
        assertTrue(
            skilledWins * 100 / runs >= 25,
            "spire is too punishing: skilled bot wins only $skilledWins/$runs",
        )
        // ...but never so often that the climb feels safe.
        assertTrue(
            skilledWins * 100 / runs <= 75,
            "spire is too soft: skilled bot wins $skilledWins/$runs",
        )
    }
}
