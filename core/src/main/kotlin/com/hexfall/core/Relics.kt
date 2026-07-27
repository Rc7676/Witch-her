package com.hexfall.core

import kotlin.random.Random

/**
 * All relics. Their behavior lives in [CombatEngine] and [RunState] hooks,
 * keyed by relic id.
 */
object RelicLibrary {

    val witchwoodCharm = RelicDef(
        "witchwood_charm", "Witchwood Charm",
        "Heal 3 HP after each combat.", 0,
    )
    val bloodVial = RelicDef(
        "blood_vial", "Blood Vial",
        "Heal 2 HP at the start of each combat.", 90,
    )
    val ironTalisman = RelicDef(
        "iron_talisman", "Iron Talisman",
        "Start each combat with 6 Block.", 120,
    )
    val witchHat = RelicDef(
        "witch_hat", "Pointed Hat",
        "Draw 2 additional cards on your first turn of combat.", 110,
    )
    val whetstone = RelicDef(
        "whetstone", "Cursed Whetstone",
        "Start each combat with 2 Strength.", 150,
    )
    val moonCharm = RelicDef(
        "moon_charm", "Moonstone Charm",
        "Start each combat with 2 Dexterity.", 150,
    )
    val thornCrown = RelicDef(
        "thorn_crown", "Crown of Thorns",
        "Start each combat with 3 Thorns.", 130,
    )
    val luckyCoin = RelicDef(
        "lucky_coin", "Lucky Coin",
        "Gain 30% more gold from combat.", 100,
    )
    val heartAmulet = RelicDef(
        "heart_amulet", "Heartwood Amulet",
        "Gain 12 Max HP when picked up.", 140,
    )
    val serpentEye = RelicDef(
        "serpent_eye", "Serpent's Eye",
        "Draw 6 cards each turn instead of 5.", 170,
    )
    val emberStone = RelicDef(
        "ember_stone", "Ember Stone",
        "Gain 1 additional Energy at the start of each turn.", 220,
    )
    val cauldron = RelicDef(
        "cauldron", "Traveling Cauldron",
        "Heal 8 HP after each combat.", 160,
    )
    val ravenFeather = RelicDef(
        "raven_feather", "Raven Feather",
        "Gain 10 Max HP when picked up.", 100,
    )
    val boneDice = RelicDef(
        "bone_dice", "Bone Dice",
        "Gain 15 gold after each combat.", 110,
    )

    val all: List<RelicDef> = listOf(
        bloodVial, ironTalisman, witchHat, whetstone, moonCharm, thornCrown,
        luckyCoin, heartAmulet, serpentEye, emberStone, cauldron, ravenFeather,
        boneDice,
    )

    fun byId(id: String): RelicDef =
        (all + witchwoodCharm).first { it.id == id }

    /** A random relic the run doesn't already own; null if all are owned. */
    fun randomNew(rng: Random, owned: Collection<RelicDef>): RelicDef? {
        val pool = all.filterNot { it in owned }
        return if (pool.isEmpty()) null else pool[rng.nextInt(pool.size)]
    }
}
