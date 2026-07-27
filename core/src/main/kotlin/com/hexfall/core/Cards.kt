package com.hexfall.core

import com.hexfall.core.CardEffect.ApplyToAll
import com.hexfall.core.CardEffect.ApplyToSelf
import com.hexfall.core.CardEffect.ApplyToTarget
import com.hexfall.core.CardEffect.Damage
import com.hexfall.core.CardEffect.DamageAll
import com.hexfall.core.CardEffect.Draw
import com.hexfall.core.CardEffect.GainBlock
import com.hexfall.core.CardEffect.GainEnergy
import com.hexfall.core.CardEffect.Heal
import com.hexfall.core.CardEffect.LoseHp
import kotlin.random.Random

/**
 * Every card in the game. Upgraded variants share the base id with a `+` suffix.
 */
object CardLibrary {

    private val defs = mutableMapOf<String, CardDef>()

    private fun card(
        id: String,
        name: String,
        type: CardType,
        rarity: Rarity,
        cost: Int,
        description: String,
        effects: List<CardEffect>,
        exhaust: Boolean = false,
    ): CardDef {
        val def = CardDef(id, name, type, rarity, cost, description, effects, exhaust)
        defs[id] = def
        return def
    }

    private fun upgrade(
        base: CardDef,
        description: String,
        effects: List<CardEffect>,
        cost: Int = base.cost,
        exhaust: Boolean = base.exhaust,
    ): CardDef {
        val def = base.copy(
            id = base.id + "+",
            name = base.name + "+",
            cost = cost,
            description = description,
            effects = effects,
            exhaust = exhaust,
            upgraded = true,
        )
        defs[def.id] = def
        return def
    }

    // --- Starter cards ---------------------------------------------------

    val hexBolt = card(
        "hex_bolt", "Hex Bolt", CardType.ATTACK, Rarity.STARTER, 1,
        "Deal 6 damage.", listOf(Damage(6)),
    ).also { upgrade(it, "Deal 9 damage.", listOf(Damage(9))) }

    val ward = card(
        "ward", "Ward", CardType.SKILL, Rarity.STARTER, 1,
        "Gain 5 Block.", listOf(GainBlock(5)),
    ).also { upgrade(it, "Gain 8 Block.", listOf(GainBlock(8))) }

    val eldritchBlast = card(
        "eldritch_blast", "Eldritch Blast", CardType.ATTACK, Rarity.STARTER, 2,
        "Deal 8 damage. Apply 2 Weak.",
        listOf(Damage(8), ApplyToTarget(StatusType.WEAK, 2)),
    ).also {
        upgrade(
            it, "Deal 11 damage. Apply 3 Weak.",
            listOf(Damage(11), ApplyToTarget(StatusType.WEAK, 3)),
        )
    }

    // --- Common cards ----------------------------------------------------

    val fireWhip = card(
        "fire_whip", "Fire Whip", CardType.ATTACK, Rarity.COMMON, 1,
        "Deal 4 damage twice.", listOf(Damage(4, times = 2)),
    ).also { upgrade(it, "Deal 6 damage twice.", listOf(Damage(6, times = 2))) }

    val soulRend = card(
        "soul_rend", "Soul Rend", CardType.ATTACK, Rarity.COMMON, 2,
        "Deal 12 damage.", listOf(Damage(12)),
    ).also { upgrade(it, "Deal 17 damage.", listOf(Damage(17))) }

    val thornLash = card(
        "thorn_lash", "Thorn Lash", CardType.ATTACK, Rarity.COMMON, 1,
        "Deal 5 damage. Gain 3 Block.",
        listOf(Damage(5), GainBlock(3)),
    ).also { upgrade(it, "Deal 7 damage. Gain 5 Block.", listOf(Damage(7), GainBlock(5))) }

    val curseMark = card(
        "curse_mark", "Curse Mark", CardType.ATTACK, Rarity.COMMON, 1,
        "Deal 4 damage. Apply 2 Vulnerable.",
        listOf(Damage(4), ApplyToTarget(StatusType.VULNERABLE, 2)),
    ).also {
        upgrade(
            it, "Deal 6 damage. Apply 3 Vulnerable.",
            listOf(Damage(6), ApplyToTarget(StatusType.VULNERABLE, 3)),
        )
    }

    val venomSpit = card(
        "venom_spit", "Venom Spit", CardType.SKILL, Rarity.COMMON, 1,
        "Apply 4 Poison.", listOf(ApplyToTarget(StatusType.POISON, 4)),
    ).also { upgrade(it, "Apply 7 Poison.", listOf(ApplyToTarget(StatusType.POISON, 7))) }

    val ironBark = card(
        "iron_bark", "Iron Bark", CardType.SKILL, Rarity.COMMON, 1,
        "Gain 8 Block.", listOf(GainBlock(8)),
    ).also { upgrade(it, "Gain 11 Block.", listOf(GainBlock(11))) }

    val foresight = card(
        "foresight", "Foresight", CardType.SKILL, Rarity.COMMON, 1,
        "Draw 2 cards.", listOf(Draw(2)),
    ).also { upgrade(it, "Draw 3 cards.", listOf(Draw(3))) }

    val deflect = card(
        "deflect", "Deflect", CardType.SKILL, Rarity.COMMON, 0,
        "Gain 3 Block.", listOf(GainBlock(3)),
    ).also { upgrade(it, "Gain 5 Block.", listOf(GainBlock(5))) }

    // --- Uncommon cards --------------------------------------------------

    val hexStorm = card(
        "hex_storm", "Hex Storm", CardType.ATTACK, Rarity.UNCOMMON, 2,
        "Deal 8 damage to ALL enemies.", listOf(DamageAll(8)),
    ).also { upgrade(it, "Deal 12 damage to ALL enemies.", listOf(DamageAll(12))) }

    val bloodPact = card(
        "blood_pact", "Blood Pact", CardType.SKILL, Rarity.UNCOMMON, 0,
        "Lose 3 HP. Draw 3 cards. Exhaust.",
        listOf(LoseHp(3), Draw(3)), exhaust = true,
    ).also {
        upgrade(
            it, "Lose 2 HP. Draw 3 cards. Exhaust.",
            listOf(LoseHp(2), Draw(3)), exhaust = true,
        )
    }

    val moonShield = card(
        "moon_shield", "Moon Shield", CardType.SKILL, Rarity.UNCOMMON, 1,
        "Gain 6 Block. Draw 1 card.",
        listOf(GainBlock(6), Draw(1)),
    ).also { upgrade(it, "Gain 9 Block. Draw 1 card.", listOf(GainBlock(9), Draw(1))) }

    val toxicCloud = card(
        "toxic_cloud", "Toxic Cloud", CardType.SKILL, Rarity.UNCOMMON, 2,
        "Apply 3 Poison to ALL enemies.",
        listOf(ApplyToAll(StatusType.POISON, 3)),
    ).also { upgrade(it, "Apply 5 Poison to ALL enemies.", listOf(ApplyToAll(StatusType.POISON, 5))) }

    val empower = card(
        "empower", "Empower", CardType.POWER, Rarity.UNCOMMON, 1,
        "Gain 2 Strength.", listOf(ApplyToSelf(StatusType.STRENGTH, 2)),
    ).also { upgrade(it, "Gain 3 Strength.", listOf(ApplyToSelf(StatusType.STRENGTH, 3))) }

    val catFamiliar = card(
        "cat_familiar", "Cat Familiar", CardType.POWER, Rarity.UNCOMMON, 1,
        "Gain 2 Dexterity.", listOf(ApplyToSelf(StatusType.DEXTERITY, 2)),
    ).also { upgrade(it, "Gain 3 Dexterity.", listOf(ApplyToSelf(StatusType.DEXTERITY, 3))) }

    val regrowth = card(
        "regrowth", "Regrowth", CardType.POWER, Rarity.UNCOMMON, 1,
        "Gain 3 Regen.", listOf(ApplyToSelf(StatusType.REGEN, 3)),
    ).also { upgrade(it, "Gain 5 Regen.", listOf(ApplyToSelf(StatusType.REGEN, 5))) }

    val brambleSkin = card(
        "bramble_skin", "Bramble Skin", CardType.POWER, Rarity.UNCOMMON, 1,
        "Gain 3 Thorns.", listOf(ApplyToSelf(StatusType.THORNS, 3)),
    ).also { upgrade(it, "Gain 5 Thorns.", listOf(ApplyToSelf(StatusType.THORNS, 5))) }

    val witchsBrew = card(
        "witchs_brew", "Witch's Brew", CardType.SKILL, Rarity.UNCOMMON, 0,
        "Gain 2 Energy. Exhaust.", listOf(GainEnergy(2)), exhaust = true,
    ).also { upgrade(it, "Gain 3 Energy. Exhaust.", listOf(GainEnergy(3)), exhaust = true) }

    val siphonSoul = card(
        "siphon_soul", "Siphon Soul", CardType.ATTACK, Rarity.UNCOMMON, 2,
        "Deal 9 damage. Heal 4 HP.",
        listOf(Damage(9), Heal(4)),
    ).also { upgrade(it, "Deal 13 damage. Heal 6 HP.", listOf(Damage(13), Heal(6))) }

    // --- Rare cards ------------------------------------------------------

    val meteor = card(
        "meteor", "Meteor", CardType.ATTACK, Rarity.RARE, 3,
        "Deal 24 damage.", listOf(Damage(24)),
    ).also { upgrade(it, "Deal 32 damage.", listOf(Damage(32))) }

    val wither = card(
        "wither", "Wither", CardType.SKILL, Rarity.RARE, 2,
        "Apply 2 Weak and 2 Vulnerable.",
        listOf(
            ApplyToTarget(StatusType.WEAK, 2),
            ApplyToTarget(StatusType.VULNERABLE, 2),
        ),
    ).also {
        upgrade(
            it, "Apply 3 Weak and 3 Vulnerable.",
            listOf(
                ApplyToTarget(StatusType.WEAK, 3),
                ApplyToTarget(StatusType.VULNERABLE, 3),
            ),
        )
    }

    val sandsOfTime = card(
        "sands_of_time", "Sands of Time", CardType.SKILL, Rarity.RARE, 1,
        "Draw 3 cards. Gain 1 Energy. Exhaust.",
        listOf(Draw(3), GainEnergy(1)), exhaust = true,
    ).also {
        upgrade(
            it, "Draw 4 cards. Gain 2 Energy. Exhaust.",
            listOf(Draw(4), GainEnergy(2)), exhaust = true,
        )
    }

    val phoenixForm = card(
        "phoenix_form", "Phoenix Form", CardType.POWER, Rarity.RARE, 3,
        "At the start of your turn, deal 4 damage to ALL enemies.",
        listOf(ApplyToSelf(StatusType.IMMOLATE, 4)),
    ).also {
        upgrade(
            it, "At the start of your turn, deal 6 damage to ALL enemies.",
            listOf(ApplyToSelf(StatusType.IMMOLATE, 6)),
        )
    }

    val demonPact = card(
        "demon_pact", "Demon Pact", CardType.POWER, Rarity.RARE, 1,
        "Gain 1 Energy at the start of each turn. Lose 2 HP at the start of each turn.",
        listOf(ApplyToSelf(StatusType.ENERGIZE, 1), ApplyToSelf(StatusType.DECAY, 2)),
    ).also {
        upgrade(
            it, "Gain 1 Energy at the start of each turn. Lose 1 HP at the start of each turn.",
            listOf(ApplyToSelf(StatusType.ENERGIZE, 1), ApplyToSelf(StatusType.DECAY, 1)),
        )
    }

    val annihilate = card(
        "annihilate", "Annihilate", CardType.ATTACK, Rarity.RARE, 2,
        "Deal 10 damage to ALL enemies. Apply 2 Weak to ALL enemies.",
        listOf(DamageAll(10), ApplyToAll(StatusType.WEAK, 2)),
    ).also {
        upgrade(
            it, "Deal 14 damage to ALL enemies. Apply 2 Weak to ALL enemies.",
            listOf(DamageAll(14), ApplyToAll(StatusType.WEAK, 2)),
        )
    }

    // --- Curses ----------------------------------------------------------

    val burden = card(
        "burden", "Burden", CardType.CURSE, Rarity.CURSE, -1,
        "Unplayable. A dead weight in your hand.", emptyList(),
    )

    // ---------------------------------------------------------------------

    fun byId(id: String): CardDef =
        defs[id] ?: error("Unknown card id: $id")

    fun upgradedVersion(def: CardDef): CardDef? =
        if (def.upgraded || def.type == CardType.CURSE) null else defs[def.id + "+"]

    /** All base (non-upgraded, non-starter, non-curse) cards offered as rewards. */
    val rewardPool: List<CardDef> by lazy {
        defs.values.filter {
            !it.upgraded && it.rarity != Rarity.STARTER && it.rarity != Rarity.CURSE
        }.sortedBy { it.id }
    }

    fun randomRewardCards(rng: Random, count: Int, eliteBonus: Boolean = false): List<CardDef> {
        val picks = mutableListOf<CardDef>()
        val pool = rewardPool.toMutableList()
        repeat(count) {
            if (pool.isEmpty()) return@repeat
            val roll = rng.nextInt(100)
            val rarity = when {
                eliteBonus -> if (roll < 40) Rarity.UNCOMMON else if (roll < 70) Rarity.RARE else Rarity.COMMON
                roll < 60 -> Rarity.COMMON
                roll < 92 -> Rarity.UNCOMMON
                else -> Rarity.RARE
            }
            val candidates = pool.filter { it.rarity == rarity }.ifEmpty { pool }
            val pick = candidates[rng.nextInt(candidates.size)]
            picks += pick
            pool.remove(pick)
        }
        return picks
    }
}
