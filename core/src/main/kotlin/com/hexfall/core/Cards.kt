package com.hexfall.core

import com.hexfall.core.CardEffect.AdvanceMoon
import com.hexfall.core.CardEffect.ApplyToAll
import com.hexfall.core.CardEffect.ApplyToSelf
import com.hexfall.core.CardEffect.ApplyToTarget
import com.hexfall.core.CardEffect.Damage
import com.hexfall.core.CardEffect.DamageAll
import com.hexfall.core.CardEffect.Draw
import com.hexfall.core.CardEffect.GainMana
import com.hexfall.core.CardEffect.GainWard
import com.hexfall.core.CardEffect.Heal
import com.hexfall.core.CardEffect.LoseHp
import kotlin.random.Random

/**
 * Every card in the game. Upgraded variants share the base id with a `+`
 * suffix. The set is built around three original axes: the turning moon,
 * blood magic (paying HP), and stacking afflictions (Venom, Doom, Hexed).
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

    val moonbolt = card(
        "moonbolt", "Moonbolt", CardType.SPELL_ATTACK, Rarity.STARTER, 1,
        "Deal 5 damage. Full Moon: deal 3 more.",
        listOf(Damage(5, fullMoonBonus = 3)),
    ).also {
        upgrade(
            it, "Deal 8 damage. Full Moon: deal 4 more.",
            listOf(Damage(8, fullMoonBonus = 4)),
        )
    }

    val veil = card(
        "veil", "Veil", CardType.SPELL_WARD, Rarity.STARTER, 1,
        "Gain 5 Ward.", listOf(GainWard(5)),
    ).also { upgrade(it, "Gain 8 Ward.", listOf(GainWard(8))) }

    val bloodprick = card(
        "bloodprick", "Bloodprick", CardType.SPELL_ATTACK, Rarity.STARTER, 0,
        "Lose 2 HP. Deal 7 damage.",
        listOf(LoseHp(2), Damage(7)),
    ).also { upgrade(it, "Lose 1 HP. Deal 9 damage.", listOf(LoseHp(1), Damage(9))) }

    val lunarTide = card(
        "lunar_tide", "Lunar Tide", CardType.RITE, Rarity.STARTER, 1,
        "Turn the moon forward. Draw 1 card.",
        listOf(AdvanceMoon, Draw(1)),
    ).also {
        upgrade(
            it, "Turn the moon forward. Draw 1 card.",
            listOf(AdvanceMoon, Draw(1)), cost = 0,
        )
    }

    // --- Common cards ----------------------------------------------------

    val barbedCurse = card(
        "barbed_curse", "Barbed Curse", CardType.SPELL_ATTACK, Rarity.COMMON, 1,
        "Deal 4 damage. Apply 2 Hexed.",
        listOf(Damage(4), ApplyToTarget(StatusType.HEXED, 2)),
    ).also {
        upgrade(
            it, "Deal 6 damage. Apply 3 Hexed.",
            listOf(Damage(6), ApplyToTarget(StatusType.HEXED, 3)),
        )
    }

    val frostbite = card(
        "frostbite", "Frostbite", CardType.SPELL_ATTACK, Rarity.COMMON, 1,
        "Deal 5 damage. Apply 2 Chill.",
        listOf(Damage(5), ApplyToTarget(StatusType.CHILL, 2)),
    ).also {
        upgrade(
            it, "Deal 7 damage. Apply 3 Chill.",
            listOf(Damage(7), ApplyToTarget(StatusType.CHILL, 3)),
        )
    }

    val venomKiss = card(
        "venom_kiss", "Venom Kiss", CardType.RITE, Rarity.COMMON, 1,
        "Apply 5 Venom.", listOf(ApplyToTarget(StatusType.VENOM, 5)),
    ).also { upgrade(it, "Apply 8 Venom.", listOf(ApplyToTarget(StatusType.VENOM, 8))) }

    val doomsign = card(
        "doomsign", "Doomsign", CardType.SPELL_ATTACK, Rarity.COMMON, 1,
        "Deal 3 damage. Apply 2 Doom.",
        listOf(Damage(3), ApplyToTarget(StatusType.DOOM, 2)),
    ).also {
        upgrade(
            it, "Deal 4 damage. Apply 3 Doom.",
            listOf(Damage(4), ApplyToTarget(StatusType.DOOM, 3)),
        )
    }

    val owlsWarning = card(
        "owls_warning", "Owl's Warning", CardType.SPELL_WARD, Rarity.COMMON, 1,
        "Gain 4 Ward. Draw 1 card.",
        listOf(GainWard(4), Draw(1)),
    ).also { upgrade(it, "Gain 6 Ward. Draw 1 card.", listOf(GainWard(6), Draw(1))) }

    val rootwall = card(
        "rootwall", "Rootwall", CardType.SPELL_WARD, Rarity.COMMON, 2,
        "Gain 11 Ward.", listOf(GainWard(11)),
    ).also { upgrade(it, "Gain 15 Ward.", listOf(GainWard(15))) }

    val mistStep = card(
        "mist_step", "Mist Step", CardType.SPELL_WARD, Rarity.COMMON, 0,
        "Gain 3 Ward. Full Moon: gain 2 more.",
        listOf(GainWard(3, fullMoonBonus = 2)),
    ).also {
        upgrade(
            it, "Gain 4 Ward. Full Moon: gain 3 more.",
            listOf(GainWard(4, fullMoonBonus = 3)),
        )
    }

    val twinSparks = card(
        "twin_sparks", "Twin Sparks", CardType.SPELL_ATTACK, Rarity.COMMON, 1,
        "Deal 3 damage twice. Full Moon: each spark deals 2 more.",
        listOf(Damage(3, times = 2, fullMoonBonus = 2)),
    ).also {
        upgrade(
            it, "Deal 4 damage twice. Full Moon: each spark deals 3 more.",
            listOf(Damage(4, times = 2, fullMoonBonus = 3)),
        )
    }

    val scry = card(
        "scry", "Scry", CardType.RITE, Rarity.COMMON, 1,
        "Draw 2 cards. New Moon: draw 1 more.",
        listOf(Draw(2, newMoonBonus = 1)),
    ).also {
        upgrade(
            it, "Draw 3 cards. New Moon: draw 1 more.",
            listOf(Draw(3, newMoonBonus = 1)),
        )
    }

    // --- Uncommon cards --------------------------------------------------

    val witchfire = card(
        "witchfire", "Witchfire", CardType.SPELL_ATTACK, Rarity.UNCOMMON, 2,
        "Deal 8 damage to ALL enemies. Full Moon: deal 3 more.",
        listOf(DamageAll(8, fullMoonBonus = 3)),
    ).also {
        upgrade(
            it, "Deal 11 damage to ALL enemies. Full Moon: deal 4 more.",
            listOf(DamageAll(11, fullMoonBonus = 4)),
        )
    }

    val bloodTithe = card(
        "blood_tithe", "Blood Tithe", CardType.RITE, Rarity.UNCOMMON, 0,
        "Lose 3 HP. Gain 2 Mana. Exhaust.",
        listOf(LoseHp(3), GainMana(2)), exhaust = true,
    ).also {
        upgrade(
            it, "Lose 2 HP. Gain 2 Mana. Exhaust.",
            listOf(LoseHp(2), GainMana(2)), exhaust = true,
        )
    }

    val mooncall = card(
        "mooncall", "Mooncall", CardType.RITE, Rarity.UNCOMMON, 1,
        "Turn the moon forward. Gain 4 Ward. Draw 1 card.",
        listOf(AdvanceMoon, GainWard(4), Draw(1)),
    ).also {
        upgrade(
            it, "Turn the moon forward. Gain 7 Ward. Draw 1 card.",
            listOf(AdvanceMoon, GainWard(7), Draw(1)),
        )
    }

    val doomHarvest = card(
        "doom_harvest", "Doom Harvest", CardType.SPELL_ATTACK, Rarity.UNCOMMON, 2,
        "Deal 6 damage. Apply 3 Doom.",
        listOf(Damage(6), ApplyToTarget(StatusType.DOOM, 3)),
    ).also {
        upgrade(
            it, "Deal 8 damage. Apply 4 Doom.",
            listOf(Damage(8), ApplyToTarget(StatusType.DOOM, 4)),
        )
    }

    val venomBloom = card(
        "venom_bloom", "Venom Bloom", CardType.RITE, Rarity.UNCOMMON, 2,
        "Apply 4 Venom to ALL enemies.",
        listOf(ApplyToAll(StatusType.VENOM, 4)),
    ).also {
        upgrade(it, "Apply 6 Venom to ALL enemies.", listOf(ApplyToAll(StatusType.VENOM, 6)))
    }

    val whisperedPower = card(
        "whispered_power", "Whispered Power", CardType.RITE, Rarity.UNCOMMON, 1,
        "Gain 2 Spellpower.", listOf(ApplyToSelf(StatusType.SPELLPOWER, 2)),
    ).also { upgrade(it, "Gain 3 Spellpower.", listOf(ApplyToSelf(StatusType.SPELLPOWER, 3))) }

    val barkward = card(
        "barkward", "Barkward", CardType.RITE, Rarity.UNCOMMON, 1,
        "Gain 2 Bulwark.", listOf(ApplyToSelf(StatusType.BULWARK, 2)),
    ).also { upgrade(it, "Gain 3 Bulwark.", listOf(ApplyToSelf(StatusType.BULWARK, 3))) }

    val ravenFamiliar = card(
        "raven_familiar", "Raven Familiar", CardType.RITE, Rarity.UNCOMMON, 1,
        "Gain 1 Omen: draw an extra card each turn.",
        listOf(ApplyToSelf(StatusType.OMEN, 1)),
    ).also {
        upgrade(
            it, "Gain 2 Omen: draw two extra cards each turn.",
            listOf(ApplyToSelf(StatusType.OMEN, 2)),
        )
    }

    val brambleAura = card(
        "bramble_aura", "Bramble Aura", CardType.RITE, Rarity.UNCOMMON, 1,
        "Gain 3 Brambles.", listOf(ApplyToSelf(StatusType.BRAMBLES, 3)),
    ).also { upgrade(it, "Gain 5 Brambles.", listOf(ApplyToSelf(StatusType.BRAMBLES, 5))) }

    val silverChalice = card(
        "silver_chalice", "Silver Chalice", CardType.RITE, Rarity.UNCOMMON, 1,
        "Gain 4 Regrowth.", listOf(ApplyToSelf(StatusType.REGROWTH, 4)),
    ).also { upgrade(it, "Gain 6 Regrowth.", listOf(ApplyToSelf(StatusType.REGROWTH, 6))) }

    val leechLife = card(
        "leech_life", "Leech Life", CardType.SPELL_ATTACK, Rarity.UNCOMMON, 2,
        "Deal 8 damage. Heal 4 HP.",
        listOf(Damage(8), Heal(4)),
    ).also { upgrade(it, "Deal 11 damage. Heal 6 HP.", listOf(Damage(11), Heal(6))) }

    // --- Rare cards ------------------------------------------------------

    val moonfall = card(
        "moonfall", "Moonfall", CardType.SPELL_ATTACK, Rarity.RARE, 3,
        "Deal 18 damage. Full Moon: deal 10 more.",
        listOf(Damage(18, fullMoonBonus = 10)),
    ).also {
        upgrade(
            it, "Deal 24 damage. Full Moon: deal 12 more.",
            listOf(Damage(24, fullMoonBonus = 12)),
        )
    }

    val eclipse = card(
        "eclipse", "Eclipse", CardType.RITE, Rarity.RARE, 1,
        "Turn the moon forward twice. Gain 1 Mana. Exhaust.",
        listOf(AdvanceMoon, AdvanceMoon, GainMana(1)), exhaust = true,
    ).also {
        upgrade(
            it, "Turn the moon forward twice. Gain 1 Mana.",
            listOf(AdvanceMoon, AdvanceMoon, GainMana(1)), exhaust = false,
        )
    }

    val plagueOfAges = card(
        "plague_of_ages", "Plague of Ages", CardType.RITE, Rarity.RARE, 2,
        "Apply 8 Venom and 2 Doom.",
        listOf(ApplyToTarget(StatusType.VENOM, 8), ApplyToTarget(StatusType.DOOM, 2)),
    ).also {
        upgrade(
            it, "Apply 11 Venom and 3 Doom.",
            listOf(ApplyToTarget(StatusType.VENOM, 11), ApplyToTarget(StatusType.DOOM, 3)),
        )
    }

    val emberheart = card(
        "emberheart", "Emberheart", CardType.RITE, Rarity.RARE, 3,
        "Gain 4 Emberheart: burn all enemies each turn.",
        listOf(ApplyToSelf(StatusType.EMBERHEART, 4)),
    ).also {
        upgrade(
            it, "Gain 6 Emberheart: burn all enemies each turn.",
            listOf(ApplyToSelf(StatusType.EMBERHEART, 6)),
        )
    }

    val crimsonPact = card(
        "crimson_pact", "Crimson Pact", CardType.RITE, Rarity.RARE, 1,
        "Gain 1 Attuned: extra Mana each turn. Gain 2 Blood Debt.",
        listOf(ApplyToSelf(StatusType.ATTUNED, 1), ApplyToSelf(StatusType.BLOOD_DEBT, 2)),
    ).also {
        upgrade(
            it, "Gain 1 Attuned: extra Mana each turn. Gain 1 Blood Debt.",
            listOf(ApplyToSelf(StatusType.ATTUNED, 1), ApplyToSelf(StatusType.BLOOD_DEBT, 1)),
        )
    }

    val hundredCurses = card(
        "hundred_curses", "Hundred Curses", CardType.RITE, Rarity.RARE, 2,
        "Apply 2 Hexed, 2 Chill and 2 Doom to ALL enemies.",
        listOf(
            ApplyToAll(StatusType.HEXED, 2),
            ApplyToAll(StatusType.CHILL, 2),
            ApplyToAll(StatusType.DOOM, 2),
        ),
    ).also {
        upgrade(
            it, "Apply 3 Hexed, 3 Chill and 2 Doom to ALL enemies.",
            listOf(
                ApplyToAll(StatusType.HEXED, 3),
                ApplyToAll(StatusType.CHILL, 3),
                ApplyToAll(StatusType.DOOM, 2),
            ),
        )
    }

    // --- Curses ----------------------------------------------------------

    val graveDust = card(
        "grave_dust", "Grave Dust", CardType.CURSE, Rarity.CURSE, -1,
        "Unplayable. It clings to everything.", emptyList(),
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
