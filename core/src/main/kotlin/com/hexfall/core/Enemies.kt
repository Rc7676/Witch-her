package com.hexfall.core

import kotlin.random.Random

/**
 * The spire's bestiary. Several creatures react to the moon — wolves rage
 * under the Full Moon, moths hunt in the dark of the New Moon — and the
 * Grave Robber steals gold. Numbers are tuned against RunSimulationTest's
 * balance guardrails.
 */
object EnemyLibrary {

    val moonfangWolf = EnemyDef("moonfang_wolf", "Moonfang Wolf", 18, 24) { _, _, _, moon ->
        if (moon == MoonPhase.FULL) {
            EnemyMove.Attack(13)
        } else {
            EnemyMove.Attack(8)
        }
    }

    val bogWisp = EnemyDef("bog_wisp", "Bog Wisp", 12, 16) { turn, _, _, _ ->
        if (turn % 2 == 0) EnemyMove.Attack(7) else EnemyMove.AttackGuard(5, 4)
    }

    val plagueRat = EnemyDef("plague_rat", "Plague Rat", 11, 15) { turn, _, _, _ ->
        if (turn % 3 == 0) {
            EnemyMove.Debuff(StatusType.VENOM, 4, "Filthy Bite")
        } else {
            EnemyMove.Attack(5)
        }
    }

    val moonMoth = EnemyDef("moon_moth", "Moon Moth", 13, 17) { _, _, _, moon ->
        if (moon == MoonPhase.NEW) {
            EnemyMove.Debuff(StatusType.CHILL, 3, "Sleep Dust")
        } else {
            EnemyMove.Attack(6)
        }
    }

    val graveRobber = EnemyDef("grave_robber", "Grave Robber", 16, 22) { turn, _, _, _ ->
        if (turn % 4 == 1) {
            EnemyMove.Steal(4, 10)
        } else {
            EnemyMove.Attack(7)
        }
    }

    val thornSprite = EnemyDef("thorn_sprite", "Thorn Sprite", 14, 18) { turn, _, _, _ ->
        if (turn == 0) {
            EnemyMove.Buff(StatusType.BRAMBLES, 3, "Bristle")
        } else {
            EnemyMove.Attack(6)
        }
    }

    val hollowArmor = EnemyDef("hollow_armor", "Hollow Armor", 28, 36) { turn, _, _, _ ->
        when (turn % 3) {
            0 -> EnemyMove.Guard(9)
            1 -> EnemyMove.Attack(11)
            else -> EnemyMove.AttackGuard(6, 6)
        }
    }

    val covenTraitor = EnemyDef("coven_traitor", "Coven Traitor", 22, 28) { turn, _, _, _ ->
        if (turn == 0) {
            EnemyMove.Buff(StatusType.FRENZY, 2, "Forbidden Chant")
        } else {
            EnemyMove.Attack(6)
        }
    }

    val barrowWight = EnemyDef("barrow_wight", "Barrow Wight", 20, 26) { turn, _, _, _ ->
        if (turn % 3 == 2) {
            EnemyMove.Debuff(StatusType.HEXED, 3, "Grave Mark")
        } else {
            EnemyMove.Attack(8)
        }
    }

    // --- Elites ----------------------------------------------------------

    val boneColossus = EnemyDef("bone_colossus", "Bone Colossus", 52, 60) { turn, _, _, _ ->
        when (turn % 3) {
            0 -> EnemyMove.Guard(15)
            1 -> EnemyMove.Attack(20)
            else -> EnemyMove.Attack(8, times = 2)
        }
    }

    val bloodAlchemist = EnemyDef("blood_alchemist", "Blood Alchemist", 46, 54) { turn, _, self, _ ->
        when (turn % 4) {
            0 -> EnemyMove.Debuff(StatusType.VENOM, 5, "Vitriol Flask")
            1 -> EnemyMove.Attack(11)
            2 -> if (self.hp < self.maxHp * 2 / 3) {
                EnemyMove.HealSelf(10, "Red Draught")
            } else {
                EnemyMove.AttackGuard(7, 7)
            }
            else -> EnemyMove.Attack(6, times = 2)
        }
    }

    // --- Boss ------------------------------------------------------------

    // Tuned against BalanceTest: at 155 HP with a 15 HP heal, skilled runs
    // reached her 81% of the time and still lost with her averaging 36 HP
    // left — she undid the last stretch faster than a mid-size deck could
    // close it. Less HP and a smaller heal keep her a wall without making
    // the final fifth of the fight feel unwinnable.
    val hollowQueen = EnemyDef("hollow_queen", "The Hollow Queen", 135, 135) { turn, _, self, moon ->
        if (moon == MoonPhase.FULL) {
            EnemyMove.Attack(11, times = 2)
        } else {
            when (turn % 4) {
                0 -> EnemyMove.Debuff(StatusType.HEXED, 3, "Royal Decree")
                1 -> EnemyMove.Attack(15)
                2 -> if (self.hp < self.maxHp / 2) {
                    EnemyMove.HealSelf(9, "Drink the Court")
                } else {
                    EnemyMove.Buff(StatusType.FRENZY, 3, "Coronation")
                }
                else -> EnemyMove.AttackGuard(9, 8)
            }
        }
    }

    private val easyPacks: List<List<EnemyDef>> = listOf(
        listOf(moonfangWolf),
        listOf(bogWisp),
        listOf(thornSprite),
        listOf(plagueRat, moonMoth),
    )

    private val mediumPacks: List<List<EnemyDef>> = listOf(
        listOf(graveRobber),
        listOf(barrowWight),
        listOf(hollowArmor),
        listOf(covenTraitor),
        listOf(moonfangWolf, moonMoth),
        listOf(bogWisp, thornSprite),
    )

    private val hardPacks: List<List<EnemyDef>> = listOf(
        listOf(moonfangWolf, moonfangWolf),
        listOf(hollowArmor, plagueRat),
        listOf(covenTraitor, barrowWight),
        listOf(graveRobber, bogWisp),
        listOf(hollowArmor, moonMoth),
    )

    private val elitePacks: List<List<EnemyDef>> = listOf(
        listOf(boneColossus),
        listOf(bloodAlchemist),
    )

    /** A normal encounter appropriate for [floor] (0-based map row). */
    fun normalEncounter(floor: Int, rng: Random): List<EnemyDef> {
        val packs = when {
            floor < 3 -> easyPacks
            floor < 9 -> mediumPacks
            else -> hardPacks
        }
        return packs[rng.nextInt(packs.size)]
    }

    fun eliteEncounter(rng: Random): List<EnemyDef> =
        elitePacks[rng.nextInt(elitePacks.size)]

    fun bossEncounter(): List<EnemyDef> = listOf(hollowQueen)
}
