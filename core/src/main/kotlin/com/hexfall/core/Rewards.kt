package com.hexfall.core

import kotlin.random.Random

/**
 * What a combat pays out: gold, a pick-one-of-three card choice, and a relic
 * after elites.
 */
class CombatReward(
    val gold: Int,
    val cardChoices: List<CardDef>,
    val relic: RelicDef?,
) {
    companion object {
        fun forCombat(run: RunState, rng: Random, elite: Boolean): CombatReward {
            val baseGold = if (elite) 30 + rng.nextInt(21) else 12 + rng.nextInt(14)
            val relic = if (elite) RelicLibrary.randomNew(rng, run.relics) else null
            return CombatReward(
                gold = run.goldReward(baseGold),
                cardChoices = CardLibrary.randomRewardCards(rng, 3, eliteBonus = elite),
                relic = relic,
            )
        }
    }
}
