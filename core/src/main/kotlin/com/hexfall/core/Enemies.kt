package com.hexfall.core

import kotlin.random.Random

/**
 * Act 1 bestiary: normal packs, elites, and the boss. Each AI is a small
 * pattern over the enemy's own turn counter with a little randomness.
 * Numbers are tuned so the greedy bot in RunSimulationTest wins a minority
 * of runs — see that test's balance guardrails.
 */
object EnemyLibrary {

    val gloomRat = EnemyDef("gloom_rat", "Gloom Rat", 12, 16) { turn, rng, _ ->
        if (turn % 3 == 2 && rng.nextBoolean()) {
            EnemyMove.Debuff(StatusType.WEAK, 1, "Gnaw")
        } else {
            EnemyMove.Attack(6)
        }
    }

    val boneSprite = EnemyDef("bone_sprite", "Bone Sprite", 14, 18) { turn, _, _ ->
        if (turn % 2 == 0) EnemyMove.Attack(8) else EnemyMove.Defend(6)
    }

    val marshSlime = EnemyDef("marsh_slime", "Marsh Slime", 18, 24) { _, rng, _ ->
        when (rng.nextInt(3)) {
            0 -> EnemyMove.Debuff(StatusType.FRAIL, 2, "Corrosive Spit")
            1 -> EnemyMove.Attack(9)
            else -> EnemyMove.Attack(7)
        }
    }

    val cultist = EnemyDef("cultist", "Moon Cultist", 24, 30) { turn, _, _ ->
        if (turn == 0) {
            EnemyMove.Buff(StatusType.RITUAL, 3, "Dark Ritual")
        } else {
            EnemyMove.Attack(6)
        }
    }

    val direWolf = EnemyDef("dire_wolf", "Dire Wolf", 20, 26) { turn, rng, _ ->
        if (turn == 0 && rng.nextBoolean()) {
            EnemyMove.Buff(StatusType.STRENGTH, 3, "Howl")
        } else {
            EnemyMove.Attack(10)
        }
    }

    val cryptSpider = EnemyDef("crypt_spider", "Crypt Spider", 17, 21) { turn, _, _ ->
        if (turn % 3 == 0) {
            EnemyMove.Debuff(StatusType.POISON, 4, "Venom Bite")
        } else {
            EnemyMove.Attack(6)
        }
    }

    val hollowKnight = EnemyDef("hollow_knight", "Hollow Knight", 30, 38) { turn, _, _ ->
        when (turn % 3) {
            0 -> EnemyMove.AttackDefend(6, 6)
            1 -> EnemyMove.Attack(11)
            else -> EnemyMove.Defend(9)
        }
    }

    val hedgeWitch = EnemyDef("hedge_witch", "Rival Hedge Witch", 22, 28) { turn, rng, _ ->
        when {
            turn % 4 == 1 -> EnemyMove.Debuff(StatusType.VULNERABLE, 2, "Evil Eye")
            turn % 4 == 3 -> EnemyMove.HealSelf(6, "Sip Potion")
            else -> EnemyMove.Attack(6 + rng.nextInt(3))
        }
    }

    // --- Elites ----------------------------------------------------------

    val stoneGolem = EnemyDef("stone_golem", "Stone Golem", 52, 60) { turn, _, _ ->
        when (turn % 3) {
            0 -> EnemyMove.Defend(12)
            1 -> EnemyMove.Attack(14)
            else -> EnemyMove.Attack(8, times = 2)
        }
    }

    val boneKnight = EnemyDef("bone_knight", "Bone Knight", 46, 54) { turn, _, _ ->
        when (turn % 4) {
            0 -> EnemyMove.Buff(StatusType.STRENGTH, 3, "Grim Resolve")
            1 -> EnemyMove.Attack(12)
            2 -> EnemyMove.AttackDefend(7, 7)
            else -> EnemyMove.Attack(7, times = 2)
        }
    }

    // --- Boss ------------------------------------------------------------

    val paleLich = EnemyDef("pale_lich", "The Pale Lich", 160, 160) { turn, _, self ->
        when (turn % 4) {
            0 -> EnemyMove.Debuff(StatusType.WEAK, 2, "Chill of the Grave")
            1 -> EnemyMove.Attack(18)
            2 -> if (self.hp < self.maxHp / 2) {
                EnemyMove.HealSelf(15, "Soul Feast")
            } else {
                EnemyMove.Buff(StatusType.STRENGTH, 4, "Necrotic Surge")
            }
            else -> EnemyMove.Attack(10, times = 2)
        }
    }

    private val easyPacks: List<List<EnemyDef>> = listOf(
        listOf(gloomRat, gloomRat),
        listOf(boneSprite),
        listOf(marshSlime),
        listOf(cryptSpider),
    )

    private val mediumPacks: List<List<EnemyDef>> = listOf(
        listOf(cultist),
        listOf(direWolf, gloomRat),
        listOf(boneSprite, boneSprite),
        listOf(hollowKnight),
        listOf(hedgeWitch),
        listOf(cryptSpider, marshSlime),
    )

    private val hardPacks: List<List<EnemyDef>> = listOf(
        listOf(direWolf, direWolf),
        listOf(hollowKnight, boneSprite),
        listOf(hedgeWitch, cryptSpider),
        listOf(cultist, marshSlime),
        listOf(hollowKnight, hedgeWitch),
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

    private val elitePacks: List<List<EnemyDef>> = listOf(
        listOf(stoneGolem),
        listOf(boneKnight),
    )

    fun bossEncounter(): List<EnemyDef> = listOf(paleLich)
}
