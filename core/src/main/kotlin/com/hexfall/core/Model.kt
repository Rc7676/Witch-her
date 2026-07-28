package com.hexfall.core

/**
 * The moon turns as combat unfolds: one phase per player turn, cycling
 * NEW -> WAXING -> FULL -> WANING. Cards and creatures care about it.
 */
enum class MoonPhase(val displayName: String, val rulesText: String) {
    NEW("New Moon", "The dark of the moon. Scrying spells draw deeper, and moths hunt."),
    WAXING("Waxing Moon", "The moon grows. A quiet phase — prepare."),
    FULL("Full Moon", "Lunar spells surge with bonus power. Wolves and the Queen rage."),
    WANING("Waning Moon", "The moon fades toward dark.");

    fun next(): MoonPhase = entries[(ordinal + 1) % entries.size]
}

/** Card categories. */
enum class CardType { SPELL_ATTACK, SPELL_WARD, RITE, CURSE }

enum class Rarity { STARTER, COMMON, UNCOMMON, RARE, CURSE }

/**
 * Atomic things a card can do. Cards are an ordered list of these; the
 * [CombatEngine] interprets them. Several effects carry moon bonuses.
 */
sealed interface CardEffect {
    /** Hit the chosen enemy [times] times; each hit gains [fullMoonBonus] under a Full Moon. */
    data class Damage(val amount: Int, val times: Int = 1, val fullMoonBonus: Int = 0) : CardEffect

    data class DamageAll(val amount: Int, val fullMoonBonus: Int = 0) : CardEffect
    data class GainWard(val amount: Int, val fullMoonBonus: Int = 0) : CardEffect
    data class ApplyToTarget(val status: StatusType, val amount: Int) : CardEffect
    data class ApplyToAll(val status: StatusType, val amount: Int) : CardEffect
    data class ApplyToSelf(val status: StatusType, val amount: Int) : CardEffect

    /** Draw [count] cards, plus [newMoonBonus] under a New Moon. */
    data class Draw(val count: Int, val newMoonBonus: Int = 0) : CardEffect

    data class GainMana(val amount: Int) : CardEffect
    data class Heal(val amount: Int) : CardEffect
    data class LoseHp(val amount: Int) : CardEffect

    /** Turn the moon one phase forward. */
    data object AdvanceMoon : CardEffect
}

/**
 * Buffs and afflictions. Hexfall's afflictions use flat arithmetic, not
 * percentages: Hexed adds flat damage taken per hit, Chill subtracts flat
 * damage dealt per hit.
 *
 * @param decaysAtTurnEnd stacks tick down by one at the end of the owner's turn
 */
enum class StatusType(
    val displayName: String,
    val isDebuff: Boolean,
    val decaysAtTurnEnd: Boolean = false,
    val rulesText: String,
) {
    SPELLPOWER(
        "Spellpower", false,
        rulesText = "Every hit this creature deals does that much extra damage.",
    ),
    BULWARK(
        "Bulwark", false,
        rulesText = "Ward gained is increased by that much.",
    ),
    HEXED(
        "Hexed", true, decaysAtTurnEnd = true,
        rulesText = "Takes that much extra damage from every hit. Fades by 1 each turn.",
    ),
    CHILL(
        "Chill", true, decaysAtTurnEnd = true,
        rulesText = "Every hit it deals does that much less damage. Fades by 1 each turn.",
    ),
    VENOM(
        "Venom", true,
        rulesText = "At the start of its turn: takes that much damage (ignores Ward), " +
            "then the Venom halves.",
    ),
    DOOM(
        "Doom", true,
        rulesText = "At $DOOM_THRESHOLD or more stacks, Doom erupts: " +
            "${DOOM_ERUPTION_MULTIPLIER}× the stacks as damage, ignoring Ward, " +
            "then Doom resets to 0.",
    ),
    REGROWTH(
        "Regrowth", false,
        rulesText = "Heals that much at the end of its turn, then fades by 1.",
    ),
    BRAMBLES(
        "Brambles", false,
        rulesText = "Anything that strikes this creature takes that much damage back.",
    ),
    FRENZY(
        "Frenzy", false,
        rulesText = "Gains that much Spellpower at the end of each of its turns.",
    ),
    ATTUNED(
        "Attuned", false,
        rulesText = "Gain that much extra Mana at the start of each of your turns.",
    ),
    OMEN(
        "Omen", false,
        rulesText = "Draw that many extra cards at the start of each of your turns.",
    ),
    BLOOD_DEBT(
        "Blood Debt", true,
        rulesText = "Lose that much HP at the start of each of your turns.",
    ),
    EMBERHEART(
        "Emberheart", false,
        rulesText = "At the start of each of your turns, deal that much damage to ALL enemies.",
    ),
}

/** Doom erupts when a creature's stacks reach this threshold. */
const val DOOM_THRESHOLD = 6

/** Damage per Doom stack when it erupts. */
const val DOOM_ERUPTION_MULTIPLIER = 3

data class CardDef(
    val id: String,
    val name: String,
    val type: CardType,
    val rarity: Rarity,
    /** Mana cost; a negative cost means the card is unplayable (curses). */
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

/** Anything that fights: the witch or a creature of the spire. */
sealed class Combatant(var hp: Int, var maxHp: Int) {
    var ward: Int = 0
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

    /** Applies ward-mitigated damage; returns HP actually lost. */
    fun takeDamage(amount: Int): Int {
        if (amount <= 0) return 0
        val absorbed = minOf(ward, amount)
        ward -= absorbed
        val hpLoss = amount - absorbed
        hp = (hp - hpLoss).coerceAtLeast(0)
        return hpLoss
    }

    /** Damage that ignores ward (Venom, Doom, Blood Debt). */
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

/** What an enemy will do on its next turn; drives the intent badge in the UI. */
sealed class EnemyMove(val label: String) {
    class Attack(val damage: Int, val times: Int = 1) : EnemyMove("Attack")
    class Guard(val ward: Int) : EnemyMove("Guard")
    class AttackGuard(val damage: Int, val ward: Int) : EnemyMove("Attack & Guard")
    class Buff(val status: StatusType, val amount: Int, name: String) : EnemyMove(name)
    class Debuff(val status: StatusType, val amount: Int, name: String) : EnemyMove(name)
    class HealSelf(val amount: Int, name: String = "Mend") : EnemyMove(name)

    /** Hits for [damage] and pockets [gold] from the witch's purse. */
    class Steal(val damage: Int, val gold: Int, name: String = "Pilfer") : EnemyMove(name)
}

/**
 * Enemy blueprint. The [ai] picks a move from the enemy's own turn counter
 * and the moon phase that will hold when the move executes.
 */
class EnemyDef(
    val id: String,
    val name: String,
    val minHp: Int,
    val maxHp: Int,
    val ai: (turn: Int, rng: kotlin.random.Random, self: EnemyCombatant, moon: MoonPhase) -> EnemyMove,
)

data class RelicDef(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
)
