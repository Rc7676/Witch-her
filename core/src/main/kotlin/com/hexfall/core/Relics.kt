package com.hexfall.core

import kotlin.random.Random

/**
 * All charms (relics). Their behavior lives in [CombatEngine] and
 * [RunState] hooks, keyed by charm id.
 */
object RelicLibrary {

    val moonwellVial = RelicDef(
        "moonwell_vial", "Moonwell Vial",
        "Heal 4 HP after each combat.", 0,
    )
    val serpentFang = RelicDef(
        "serpent_fang", "Serpent Fang",
        "Combats begin with 3 Venom on a random enemy.", 110,
    )
    val doomkeepersBell = RelicDef(
        "doomkeepers_bell", "Doomkeeper's Bell",
        "Enemies enter combat with 2 Doom.", 130,
    )
    val wolfpeltCloak = RelicDef(
        "wolfpelt_cloak", "Wolfpelt Cloak",
        "Gain 1 extra Mana on Full Moon turns.", 150,
    )
    val obsidianFigurine = RelicDef(
        "obsidian_figurine", "Obsidian Figurine",
        "Start each combat with 8 Ward.", 120,
    )
    val scryersOrb = RelicDef(
        "scryers_orb", "Scryer's Orb",
        "Draw 2 additional cards on your first turn of combat.", 110,
    )
    val bloodstoneRing = RelicDef(
        "bloodstone_ring", "Bloodstone Ring",
        "Heal 2 HP whenever an enemy is slain.", 140,
    )
    val hexwroughtIdol = RelicDef(
        "hexwrought_idol", "Hexwrought Idol",
        "Enemies enter combat with 3 Hexed.", 130,
    )
    val silverCrescent = RelicDef(
        "silver_crescent", "Silver Crescent",
        "Your hits deal 2 more damage on Full Moon turns.", 150,
    )
    val merchantsSkull = RelicDef(
        "merchants_skull", "Merchant's Skull",
        "Everything in shops costs 20% less.", 100,
    )
    val giantsTooth = RelicDef(
        "giants_tooth", "Giant's Tooth",
        "Gain 10 Max HP when picked up.", 100,
    )
    val ashenHourglass = RelicDef(
        "ashen_hourglass", "Ashen Hourglass",
        "Gain 1 additional Mana at the start of each turn.", 220,
    )
    val thornGirdle = RelicDef(
        "thorn_girdle", "Thorn Girdle",
        "Start each combat with 3 Brambles.", 130,
    )
    val magpiesEye = RelicDef(
        "magpies_eye", "Magpie's Eye",
        "Gain 30% more gold from combat.", 100,
    )
    val owlQuill = RelicDef(
        "owl_quill", "Owl Feather Quill",
        "Draw 6 cards each turn instead of 5.", 170,
    )

    val all: List<RelicDef> = listOf(
        serpentFang, doomkeepersBell, wolfpeltCloak, obsidianFigurine, scryersOrb,
        bloodstoneRing, hexwroughtIdol, silverCrescent, merchantsSkull, giantsTooth,
        ashenHourglass, thornGirdle, magpiesEye, owlQuill,
    )

    fun byId(id: String): RelicDef =
        (all + moonwellVial).first { it.id == id }

    /** A random charm the run doesn't already own; null if all are owned. */
    fun randomNew(rng: Random, owned: Collection<RelicDef>): RelicDef? {
        val pool = all.filterNot { it in owned }
        return if (pool.isEmpty()) null else pool[rng.nextInt(pool.size)]
    }
}
