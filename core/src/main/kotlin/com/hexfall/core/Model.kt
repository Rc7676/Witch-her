package com.hexfall.core

/** Card categories, mirroring classic deckbuilder roles. */
enum class CardType { ATTACK, SKILL, POWER, CURSE }

enum class Rarity { STARTER, COMMON, UNCOMMON, RARE, CURSE }

/**
 * Atomic things a card can do. Cards are just an ordered list of these;
 * the [CombatEngine] interprets them.
 */
sealed interface CardEffect {
    /** Hit the chosen enemy [times] times for [amount] base damage each. */
    data class Damage(val amount: Int, val times: Int = 1) : CardEffect

    /** Hit every enemy once for [amount] base damage. */
    data class DamageAll(val amount: Int) : CardEffect

    data class GainBlock(val amount: Int) : CardEffect
    data class ApplyToTarget(val status: StatusType, val amount: Int) : CardEffect
    data class ApplyToAll(val status: StatusType, val amount: Int) : CardEffect
    data class ApplyToSelf(val status: StatusType, val amount: Int) : CardEffect
    data class Draw(val count: Int) : CardEffect
    data class GainEnergy(val amount: Int) : CardEffect
    data class Heal(val amount: Int) : CardEffect
    data class LoseHp(val amount: Int) : CardEffect
}

/**
 * Buffs and debuffs carried by any combatant.
 *
 * @param decaysAtTurnEnd stacks tick down by one at the end of the owner's turn
 */
enum class StatusType(
    val displayName: String,
    val isDebuff: Boolean,
    val decaysAtTurnEnd: Boolean = false,
) {
    STRENGTH("Strength", false),
    DEXTERITY("Dexterity", false),
    WEAK("Weak", true, decaysAtTurnEnd = true),
    VULNERABLE("Vulnerable", true, decaysAtTurnEnd = true),
    FRAIL("Frail", true, decaysAtTurnEnd = true),
    POISON("Poison", true),
    REGEN("Regen", false),
    THORNS("Thorns", false),

    /** Enemy ritual: gains this much Strength at the end of each of its turns. */
    RITUAL("Ritual", false),

    /** Extra energy at the start of each of the player's turns. */
    ENERGIZE("Energize", false),

    /** Lose this much HP at the start of each of the player's turns. */
    DECAY("Decay", true),

    /** Deal this much damage to all enemies at the start of each player turn. */
    IMMOLATE("Immolation", false),
}

data class CardDef(
    val id: String,
    val name: String,
    val type: CardType,
    val rarity: Rarity,
    /** Energy cost; a negative cost means the card is unplayable (curses). */
    val cost: Int,
    val description: String,
    val effects: List<CardEffect>,
    val exhaust: Boolean = false,
    val upgraded: Boolean = false,
) {
    val playable: Boolean get() = cost >= 0
    val needsTarget: Boolean
        get() = effects.any { it is CardEffect.Damage || it is CardEffect.ApplyToTarget }
}

/** A concrete copy of a card inside a run's deck. */
data class CardInstance(val uid: Int, var def: CardDef)

/** Anything that fights: the witch or an enemy. */
sealed class Combatant(var hp: Int, var maxHp: Int) {
    var block: Int = 0
    val statuses: MutableMap<StatusType, Int> = mutableMapOf()

    val alive: Boolean get() = hp > 0

    fun statusAmount(type: StatusType): Int = statuses[type] ?: 0

    fun applyStatus(type: StatusType, amount: Int) {
        val next = statusAmount(type) + amount
        if (next <= 0) statuses.remove(type) else statuses[type] = next
    }

    fun heal(amount: Int) {
        hp = (hp + amount).coerceAtMost(maxHp)
    }

    /** Applies block-mitigated damage; returns HP actually lost. */
    fun takeDamage(amount: Int): Int {
        if (amount <= 0) return 0
        val absorbed = minOf(block, amount)
        block -= absorbed
        val hpLoss = amount - absorbed
        hp = (hp - hpLoss).coerceAtLeast(0)
        return hpLoss
    }

    /** Damage that ignores block (poison, decay). */
    fun loseHp(amount: Int) {
        if (amount > 0) hp = (hp - amount).coerceAtLeast(0)
    }

    fun decayStatusesAtTurnEnd() {
        StatusType.entries.filter { it.decaysAtTurnEnd }.forEach { type ->
            val current = statusAmount(type)
            if (current > 0) applyStatus(type, -1)
        }
    }
}

class PlayerCombatant(hp: Int, maxHp: Int) : Combatant(hp, maxHp)

class EnemyCombatant(val def: EnemyDef, hp: Int) : Combatant(hp, hp) {
    var turnCounter: Int = 0
    var nextMove: EnemyMove = EnemyMove.Attack(0)
}

/** What an enemy will do on its next turn; drives the intent icon in the UI. */
sealed class EnemyMove(val label: String) {
    class Attack(val damage: Int, val times: Int = 1) : EnemyMove("Attack")
    class Defend(val block: Int) : EnemyMove("Defend")
    class AttackDefend(val damage: Int, val block: Int) : EnemyMove("Attack & Defend")
    class Buff(val status: StatusType, val amount: Int, name: String) : EnemyMove(name)
    class Debuff(val status: StatusType, val amount: Int, name: String) : EnemyMove(name)
    class HealSelf(val amount: Int, name: String = "Heal") : EnemyMove(name)
}

/** Enemy blueprint. The [ai] picks a move given the enemy's own turn counter. */
class EnemyDef(
    val id: String,
    val name: String,
    val minHp: Int,
    val maxHp: Int,
    val ai: (turn: Int, rng: kotlin.random.Random, self: EnemyCombatant) -> EnemyMove,
)

data class RelicDef(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
)
