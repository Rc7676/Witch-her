package com.hexfall.core

import kotlin.math.max
import kotlin.random.Random

enum class CombatResult { VICTORY, DEFEAT }

/**
 * Runs a single combat: piles, mana, the turning moon, statuses, enemy
 * turns. Mutable by design; the UI takes snapshots after each action.
 *
 * Damage math is flat, not percentage-based: every hit deals
 * base + attacker Spellpower - attacker Chill + defender Hexed.
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

    var moon: MoonPhase = MoonPhase.WAXING
        private set
    var mana: Int = 0
        private set
    var turn: Int = 0
        private set
    var result: CombatResult? = null
        private set

    val log: MutableList<String> = mutableListOf()

    private val baseMana: Int = 3 + if (run.hasRelic("ashen_hourglass")) 1 else 0
    private val drawPerTurn: Int = if (run.hasRelic("owl_quill")) 6 else 5

    init {
        if (run.hasRelic("obsidian_figurine")) {
            player.ward += 8
            log += "The Obsidian Figurine grants 8 Ward."
        }
        if (run.hasRelic("thorn_girdle")) player.applyStatus(StatusType.BRAMBLES, 3)
        if (run.hasRelic("serpent_fang") && enemies.isNotEmpty()) {
            val victim = enemies[rng.nextInt(enemies.size)]
            victim.applyStatus(StatusType.VENOM, 3)
            log += "The Serpent Fang envenoms ${victim.def.name}."
        }
        if (run.hasRelic("doomkeepers_bell")) {
            enemies.forEach { applyStatusChecked(it, StatusType.DOOM, 2) }
        }
        if (run.hasRelic("hexwrought_idol")) {
            enemies.forEach { it.applyStatus(StatusType.HEXED, 3) }
        }
        enemies.forEach { chooseNextMove(it, moon) }
        startPlayerTurn()
    }

    // --- Player actions --------------------------------------------------

    fun canPlay(card: CardInstance): Boolean =
        result == null && card.def.playable && card.def.cost <= mana

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

        mana -= card.def.cost
        hand.remove(card)
        log += "You cast ${card.def.name}."

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
        // Player end-of-turn: Regrowth, then decaying afflictions tick down.
        val regrowth = player.statusAmount(StatusType.REGROWTH)
        if (regrowth > 0) {
            player.heal(regrowth)
            player.applyStatus(StatusType.REGROWTH, -1)
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
        if (turn > 1) {
            moon = moon.next()
            log += "The moon turns: ${moon.displayName}."
        }
        // Ward expires at the start of each turn — except on turn 1, where it
        // would wipe Ward granted before combat began (Obsidian Figurine).
        if (turn > 1) player.ward = 0
        mana = baseMana + player.statusAmount(StatusType.ATTUNED)
        if (moon == MoonPhase.FULL && run.hasRelic("wolfpelt_cloak")) {
            mana += 1
            log += "The Wolfpelt Cloak stirs under the Full Moon: +1 Mana."
        }

        val debt = player.statusAmount(StatusType.BLOOD_DEBT)
        if (debt > 0) {
            player.loseHp(debt)
            log += "Your Blood Debt claims $debt HP."
        }
        tickVenom(player, "you")
        val ember = player.statusAmount(StatusType.EMBERHEART)
        if (ember > 0) {
            log += "Your Emberheart burns every foe for $ember."
            enemies.filter { it.alive }.forEach { it.takeDamage(ember) }
            cleanupDeadEnemies()
        }

        checkCombatEnd()
        if (result != null) return

        var toDraw = drawPerTurn + player.statusAmount(StatusType.OMEN)
        if (turn == 1 && run.hasRelic("scryers_orb")) toDraw += 2
        draw(toDraw)
    }

    private fun takeEnemyTurn(enemy: EnemyCombatant) {
        // An earlier enemy's turn may have ended the fight, or Brambles may
        // have killed this enemy before it could act. Checking the player
        // directly also covers deaths that bypass checkCombatEnd (an erupting
        // Doom applied by a Debuff move).
        if (result != null || !enemy.alive || !player.alive) return
        enemy.ward = 0
        tickVenom(enemy, enemy.def.name)
        if (!enemy.alive) return

        when (val move = enemy.nextMove) {
            is EnemyMove.Attack -> repeat(move.times) {
                if (player.alive) enemyAttack(enemy, move.damage)
            }
            is EnemyMove.Guard -> {
                enemy.ward += move.ward
                log += "${enemy.def.name} guards for ${move.ward} Ward."
            }
            is EnemyMove.AttackGuard -> {
                enemyAttack(enemy, move.damage)
                enemy.ward += move.ward
            }
            is EnemyMove.Buff -> {
                enemy.applyStatus(move.status, move.amount)
                log += "${enemy.def.name} uses ${move.label}."
            }
            is EnemyMove.Debuff -> {
                applyStatusChecked(player, move.status, move.amount)
                log += "${enemy.def.name} afflicts you: ${move.amount} ${move.status.displayName}."
                checkCombatEnd()
            }
            is EnemyMove.HealSelf -> {
                enemy.heal(move.amount)
                log += "${enemy.def.name} mends ${move.amount} HP."
            }
            is EnemyMove.Steal -> {
                enemyAttack(enemy, move.damage)
                val stolen = minOf(run.gold, move.gold)
                if (stolen > 0) {
                    run.gold -= stolen
                    log += "${enemy.def.name} pilfers $stolen gold!"
                }
            }
        }

        val frenzy = enemy.statusAmount(StatusType.FRENZY)
        if (frenzy > 0) enemy.applyStatus(StatusType.SPELLPOWER, frenzy)
        val regrowth = enemy.statusAmount(StatusType.REGROWTH)
        if (regrowth > 0) {
            enemy.heal(regrowth)
            enemy.applyStatus(StatusType.REGROWTH, -1)
        }
        enemy.decayStatusesAtTurnEnd()
        enemy.turnCounter++
        // The move chosen now executes after the next moon turn.
        chooseNextMove(enemy, moon.next())
    }

    private fun chooseNextMove(enemy: EnemyCombatant, phase: MoonPhase) {
        enemy.nextMove = enemy.def.ai(enemy.turnCounter, rng, enemy, phase)
    }

    /** Venom hits at the start of the owner's turn, then halves (rounds down). */
    private fun tickVenom(combatant: Combatant, name: String) {
        val venom = combatant.statusAmount(StatusType.VENOM)
        if (venom > 0) {
            combatant.loseHp(venom)
            combatant.statuses[StatusType.VENOM] = venom / 2
            if (venom / 2 == 0) combatant.statuses.remove(StatusType.VENOM)
            log += "Venom sears $name for $venom."
        }
    }

    private fun enemyAttack(enemy: EnemyCombatant, base: Int) {
        val damage = attackDamage(enemy, player, base)
        val lost = player.takeDamage(damage)
        log += "${enemy.def.name} hits you for $damage."
        if (damage > 0 || lost > 0) {
            val brambles = player.statusAmount(StatusType.BRAMBLES)
            if (brambles > 0) {
                enemy.takeDamage(brambles)
                log += "Brambles tear back for $brambles."
            }
        }
        checkCombatEnd()
    }

    /**
     * Flat damage pipeline: base + attacker Spellpower - attacker Chill
     * + defender Hexed, floored at 0. Silver Crescent adds 2 to the
     * player's hits under a Full Moon.
     */
    fun attackDamage(attacker: Combatant, defender: Combatant, base: Int): Int {
        var value = base + attacker.statusAmount(StatusType.SPELLPOWER)
        value -= attacker.statusAmount(StatusType.CHILL)
        value += defender.statusAmount(StatusType.HEXED)
        if (attacker === player && moon == MoonPhase.FULL && run.hasRelic("silver_crescent")) {
            value += 2
        }
        return max(0, value)
    }

    /** Ward gain: base + Bulwark. */
    fun wardGain(base: Int): Int = max(0, base + player.statusAmount(StatusType.BULWARK))

    /** Applies a status and detonates Doom if it crossed the threshold. */
    fun applyStatusChecked(combatant: Combatant, status: StatusType, amount: Int) {
        combatant.applyStatus(status, amount)
        if (status == StatusType.DOOM) {
            val doom = combatant.statusAmount(StatusType.DOOM)
            if (doom >= DOOM_THRESHOLD) {
                val burst = doom * DOOM_ERUPTION_MULTIPLIER
                combatant.statuses.remove(StatusType.DOOM)
                combatant.loseHp(burst)
                val name = if (combatant === player) "you" else (combatant as EnemyCombatant).def.name
                log += "DOOM erupts on $name for $burst!"
            }
        }
    }

    private fun resolveEffect(effect: CardEffect, target: EnemyCombatant?) {
        when (effect) {
            is CardEffect.Damage -> {
                val enemy = target ?: return
                val bonus = if (moon == MoonPhase.FULL) effect.fullMoonBonus else 0
                repeat(effect.times) {
                    if (!enemy.alive) return@repeat
                    val dmg = attackDamage(player, enemy, effect.amount + bonus)
                    enemy.takeDamage(dmg)
                    log += "You strike ${enemy.def.name} for $dmg."
                }
            }
            is CardEffect.DamageAll -> {
                val bonus = if (moon == MoonPhase.FULL) effect.fullMoonBonus else 0
                enemies.filter { it.alive }.forEach { enemy ->
                    val dmg = attackDamage(player, enemy, effect.amount + bonus)
                    enemy.takeDamage(dmg)
                    log += "You strike ${enemy.def.name} for $dmg."
                }
            }
            is CardEffect.GainWard -> {
                val bonus = if (moon == MoonPhase.FULL) effect.fullMoonBonus else 0
                val gained = wardGain(effect.amount + bonus)
                player.ward += gained
                log += "You gain $gained Ward."
            }
            is CardEffect.ApplyToTarget -> target?.takeIf { it.alive }
                ?.let { applyStatusChecked(it, effect.status, effect.amount) }
            is CardEffect.ApplyToAll -> enemies.filter { it.alive }
                .forEach { applyStatusChecked(it, effect.status, effect.amount) }
            is CardEffect.ApplyToSelf -> applyStatusChecked(player, effect.status, effect.amount)
            is CardEffect.Draw -> {
                val bonus = if (moon == MoonPhase.NEW) effect.newMoonBonus else 0
                draw(effect.count + bonus)
            }
            is CardEffect.GainMana -> mana += effect.amount
            is CardEffect.Heal -> player.heal(effect.amount)
            is CardEffect.LoseHp -> {
                player.loseHp(effect.amount)
                checkCombatEnd()
            }
            is CardEffect.AdvanceMoon -> {
                moon = moon.next()
                log += "The moon turns: ${moon.displayName}."
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
        val slain = enemies.filter { !it.alive }
        slain.forEach { log += "${it.def.name} is slain!" }
        if (slain.isNotEmpty() && run.hasRelic("bloodstone_ring")) {
            player.heal(2 * slain.size)
            log += "The Bloodstone Ring drinks deep: +${2 * slain.size} HP."
        }
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

    /** The number an intent badge should show for an attacking enemy. */
    fun intentDamage(enemy: EnemyCombatant): Int? = when (val move = enemy.nextMove) {
        is EnemyMove.Attack -> attackDamage(enemy, player, move.damage)
        is EnemyMove.AttackGuard -> attackDamage(enemy, player, move.damage)
        is EnemyMove.Steal -> attackDamage(enemy, player, move.damage)
        else -> null
    }
}
