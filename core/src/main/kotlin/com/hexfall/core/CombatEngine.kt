package com.hexfall.core

import kotlin.math.max
import kotlin.random.Random

enum class CombatResult { VICTORY, DEFEAT }

/**
 * Runs a single combat: piles, energy, statuses, enemy turns.
 * Mutable by design; the UI takes snapshots after each action.
 */
class CombatEngine(
    private val run: RunState,
    enemyDefs: List<EnemyDef>,
    private val rng: Random,
) {
    val player = PlayerCombatant(run.hp, run.maxHp)
    val enemies: MutableList<EnemyCombatant> = enemyDefs.map { def ->
        EnemyCombatant(def, hp = rng.nextInt(def.minHp, def.maxHp + 1))
    }.toMutableList()

    val drawPile: MutableList<CardInstance> = run.deck.shuffled(rng).toMutableList()
    val hand: MutableList<CardInstance> = mutableListOf()
    val discardPile: MutableList<CardInstance> = mutableListOf()
    val exhaustPile: MutableList<CardInstance> = mutableListOf()

    var energy: Int = 0
        private set
    var turn: Int = 0
        private set
    var result: CombatResult? = null
        private set

    val log: MutableList<String> = mutableListOf()

    private val baseEnergy: Int = 3 + if (run.hasRelic("ember_stone")) 1 else 0
    private val drawPerTurn: Int = if (run.hasRelic("serpent_eye")) 6 else 5

    init {
        if (run.hasRelic("blood_vial")) {
            player.heal(2)
            log += "Blood Vial heals 2 HP."
        }
        if (run.hasRelic("iron_talisman")) {
            player.block += 6
            log += "Iron Talisman grants 6 Block."
        }
        if (run.hasRelic("whetstone")) player.applyStatus(StatusType.STRENGTH, 2)
        if (run.hasRelic("moon_charm")) player.applyStatus(StatusType.DEXTERITY, 2)
        if (run.hasRelic("thorn_crown")) player.applyStatus(StatusType.THORNS, 3)
        enemies.forEach { chooseNextMove(it) }
        startPlayerTurn()
    }

    // --- Player actions --------------------------------------------------

    fun canPlay(card: CardInstance): Boolean =
        result == null && card.def.playable && card.def.cost <= energy

    /**
     * Plays [card] from the hand. [targetIndex] indexes [enemies] and is
     * required for single-target cards. Returns false if the play is illegal.
     */
    fun playCard(card: CardInstance, targetIndex: Int? = null): Boolean {
        if (!canPlay(card) || card !in hand) return false
        val target = if (card.def.needsTarget) {
            val t = targetIndex?.let { enemies.getOrNull(it) } ?: return false
            if (!t.alive) return false
            t
        } else null

        energy -= card.def.cost
        hand.remove(card)
        log += "You play ${card.def.name}."

        for (effect in card.def.effects) {
            resolveEffect(effect, target)
            if (result != null) break
        }

        if (card.def.exhaust) exhaustPile += card else discardPile += card
        cleanupDeadEnemies()
        checkCombatEnd()
        return true
    }

    fun endTurn() {
        if (result != null) return
        // Player end-of-turn: regen, then decaying debuffs tick down.
        val regen = player.statusAmount(StatusType.REGEN)
        if (regen > 0) {
            player.heal(regen)
            player.applyStatus(StatusType.REGEN, -1)
        }
        player.decayStatusesAtTurnEnd()
        discardPile += hand
        hand.clear()

        enemies.filter { it.alive }.forEach { takeEnemyTurn(it) }
        cleanupDeadEnemies()
        checkCombatEnd()
        if (result == null) startPlayerTurn()
    }

    // --- Internals -------------------------------------------------------

    private fun startPlayerTurn() {
        turn++
        player.block = 0
        energy = baseEnergy + player.statusAmount(StatusType.ENERGIZE)

        val decay = player.statusAmount(StatusType.DECAY)
        if (decay > 0) {
            player.loseHp(decay)
            log += "Decay saps $decay HP."
        }
        val poison = player.statusAmount(StatusType.POISON)
        if (poison > 0) {
            player.loseHp(poison)
            player.applyStatus(StatusType.POISON, -1)
            log += "Poison deals $poison damage to you."
        }
        val immolate = player.statusAmount(StatusType.IMMOLATE)
        if (immolate > 0) {
            log += "Flames engulf your foes for $immolate damage."
            enemies.filter { it.alive }.forEach { it.takeDamage(immolate) }
            cleanupDeadEnemies()
        }

        checkCombatEnd()
        if (result != null) return

        var toDraw = drawPerTurn
        if (turn == 1 && run.hasRelic("witch_hat")) toDraw += 2
        draw(toDraw)
    }

    private fun takeEnemyTurn(enemy: EnemyCombatant) {
        // An earlier enemy's turn may have ended the fight, or thorns may
        // have killed this enemy before it could act.
        if (result != null || !enemy.alive) return
        enemy.block = 0
        val poison = enemy.statusAmount(StatusType.POISON)
        if (poison > 0) {
            enemy.loseHp(poison)
            enemy.applyStatus(StatusType.POISON, -1)
            log += "${enemy.def.name} suffers $poison poison damage."
            if (!enemy.alive) return
        }

        when (val move = enemy.nextMove) {
            is EnemyMove.Attack -> repeat(move.times) {
                if (player.alive) enemyAttack(enemy, move.damage)
            }
            is EnemyMove.Defend -> {
                enemy.block += move.block
                log += "${enemy.def.name} braces for ${move.block} Block."
            }
            is EnemyMove.AttackDefend -> {
                enemyAttack(enemy, move.damage)
                enemy.block += move.block
            }
            is EnemyMove.Buff -> {
                enemy.applyStatus(move.status, move.amount)
                log += "${enemy.def.name} uses ${move.label}."
            }
            is EnemyMove.Debuff -> {
                player.applyStatus(move.status, move.amount)
                log += "${enemy.def.name} afflicts you with ${move.amount} ${move.status.displayName}."
            }
            is EnemyMove.HealSelf -> {
                enemy.heal(move.amount)
                log += "${enemy.def.name} restores ${move.amount} HP."
            }
        }

        val ritual = enemy.statusAmount(StatusType.RITUAL)
        if (ritual > 0) enemy.applyStatus(StatusType.STRENGTH, ritual)
        val regen = enemy.statusAmount(StatusType.REGEN)
        if (regen > 0) {
            enemy.heal(regen)
            enemy.applyStatus(StatusType.REGEN, -1)
        }
        enemy.decayStatusesAtTurnEnd()
        enemy.turnCounter++
        chooseNextMove(enemy)
    }

    private fun chooseNextMove(enemy: EnemyCombatant) {
        enemy.nextMove = enemy.def.ai(enemy.turnCounter, rng, enemy)
    }

    private fun enemyAttack(enemy: EnemyCombatant, base: Int) {
        val damage = attackDamage(enemy, player, base)
        val lost = player.takeDamage(damage)
        log += "${enemy.def.name} hits you for $damage."
        if (lost > 0 || damage > 0) {
            val thorns = player.statusAmount(StatusType.THORNS)
            if (thorns > 0) {
                enemy.takeDamage(thorns)
                log += "Thorns strike back for $thorns."
            }
        }
        checkCombatEnd()
    }

    /** Strength, Weak (-25%) and Vulnerable (+50%) modifiers, floored, min 0. */
    fun attackDamage(attacker: Combatant, defender: Combatant, base: Int): Int {
        var value = (base + attacker.statusAmount(StatusType.STRENGTH)).toDouble()
        if (attacker.statusAmount(StatusType.WEAK) > 0) value *= 0.75
        if (defender.statusAmount(StatusType.VULNERABLE) > 0) value *= 1.5
        return max(0, value.toInt())
    }

    /** Dexterity and Frail (-25%) modifiers for block gain. */
    fun blockGain(base: Int): Int {
        var value = (base + player.statusAmount(StatusType.DEXTERITY)).toDouble()
        if (player.statusAmount(StatusType.FRAIL) > 0) value *= 0.75
        return max(0, value.toInt())
    }

    private fun resolveEffect(effect: CardEffect, target: EnemyCombatant?) {
        when (effect) {
            is CardEffect.Damage -> {
                val enemy = target ?: return
                repeat(effect.times) {
                    if (!enemy.alive) return@repeat
                    val dmg = attackDamage(player, enemy, effect.amount)
                    enemy.takeDamage(dmg)
                    log += "You hit ${enemy.def.name} for $dmg."
                }
            }
            is CardEffect.DamageAll -> enemies.filter { it.alive }.forEach { enemy ->
                val dmg = attackDamage(player, enemy, effect.amount)
                enemy.takeDamage(dmg)
                log += "You hit ${enemy.def.name} for $dmg."
            }
            is CardEffect.GainBlock -> {
                val gained = blockGain(effect.amount)
                player.block += gained
                log += "You gain $gained Block."
            }
            is CardEffect.ApplyToTarget -> target?.takeIf { it.alive }
                ?.applyStatus(effect.status, effect.amount)
            is CardEffect.ApplyToAll -> enemies.filter { it.alive }
                .forEach { it.applyStatus(effect.status, effect.amount) }
            is CardEffect.ApplyToSelf -> player.applyStatus(effect.status, effect.amount)
            is CardEffect.Draw -> draw(effect.count)
            is CardEffect.GainEnergy -> energy += effect.amount
            is CardEffect.Heal -> player.heal(effect.amount)
            is CardEffect.LoseHp -> {
                player.loseHp(effect.amount)
                checkCombatEnd()
            }
        }
    }

    private fun draw(count: Int) {
        repeat(count) {
            if (hand.size >= 10) return
            if (drawPile.isEmpty()) {
                if (discardPile.isEmpty()) return
                drawPile += discardPile.shuffled(rng)
                discardPile.clear()
            }
            hand += drawPile.removeAt(0)
        }
    }

    private fun cleanupDeadEnemies() {
        enemies.filter { !it.alive }.forEach { log += "${it.def.name} is slain!" }
        enemies.removeAll { !it.alive }
    }

    private fun checkCombatEnd() {
        if (result != null) return
        if (!player.alive) {
            result = CombatResult.DEFEAT
            run.hp = 0
        } else if (enemies.isEmpty()) {
            result = CombatResult.VICTORY
            run.hp = player.hp
        }
    }

    /** The number an intent icon should show for an attacking enemy. */
    fun intentDamage(enemy: EnemyCombatant): Int? = when (val move = enemy.nextMove) {
        is EnemyMove.Attack -> attackDamage(enemy, player, move.damage)
        is EnemyMove.AttackDefend -> attackDamage(enemy, player, move.damage)
        else -> null
    }
}
