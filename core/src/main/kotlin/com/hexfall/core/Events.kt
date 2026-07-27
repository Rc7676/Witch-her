package com.hexfall.core

import kotlin.random.Random

/**
 * A choice inside an event. [available] can gate a choice (e.g. enough gold);
 * [resolve] mutates the run and returns the outcome text shown to the player.
 */
class EventChoice(
    val label: String,
    val available: (RunState) -> Boolean = { true },
    val resolve: (RunState, Random) -> String,
)

class EventDef(
    val id: String,
    val title: String,
    val text: String,
    val choices: List<EventChoice>,
)

object EventLibrary {

    private val ancientShrine = EventDef(
        "ancient_shrine", "Ancient Shrine",
        "A moss-covered shrine hums with forgotten power. Offerings of bone " +
            "and silver litter its base.",
        listOf(
            EventChoice("Pray (heal 15 HP)") { run, _ ->
                run.hp = (run.hp + 15).coerceAtMost(run.maxHp)
                "Warmth spreads through you. You heal 15 HP."
            },
            EventChoice("Desecrate (gain 50 gold, gain a Burden curse)") { run, _ ->
                run.gold += 50
                run.addCard(CardLibrary.burden)
                "You pry the silver loose. Something cold settles into your deck..."
            },
            EventChoice("Leave") { _, _ -> "You move on, uneasy." },
        ),
    )

    private val moonlitPool = EventDef(
        "moonlit_pool", "Moonlit Pool",
        "Still water reflects a moon that is not in the sky. Your cards " +
            "shimmer at its edge.",
        listOf(
            EventChoice(
                "Bathe a card (upgrade a random card)",
                available = { run -> run.deck.any { CardLibrary.upgradedVersion(it.def) != null } },
            ) { run, rng ->
                val upgradable = run.deck.filter { CardLibrary.upgradedVersion(it.def) != null }
                val card = upgradable[rng.nextInt(upgradable.size)]
                val oldName = card.def.name
                run.upgradeCard(card)
                "$oldName glows silver and becomes ${card.def.name}."
            },
            EventChoice("Drink (heal 10 HP)") { run, _ ->
                run.hp = (run.hp + 10).coerceAtMost(run.maxHp)
                "The water tastes of starlight. You heal 10 HP."
            },
        ),
    )

    private val wanderingPeddler = EventDef(
        "wandering_peddler", "Wandering Peddler",
        "A hunched figure opens a coat lined with glittering trinkets. " +
            "\"For you, witch? A bargain.\"",
        listOf(
            EventChoice(
                "Buy a trinket (60 gold, random relic)",
                available = { it.gold >= 60 },
            ) { run, rng ->
                val relic = RelicLibrary.randomNew(rng, run.relics)
                if (relic == null) {
                    "The peddler has nothing you don't already own."
                } else {
                    run.gold -= 60
                    run.addRelic(relic)
                    "You receive the ${relic.name}. ${relic.description}"
                }
            },
            EventChoice("Decline") { _, _ -> "The peddler shrugs and shuffles into the mist." },
        ),
    )

    private val cursedTome = EventDef(
        "cursed_tome", "Cursed Tome",
        "A grimoire bound in pale leather lies open on a lectern, its pages " +
            "turning by themselves.",
        listOf(
            EventChoice("Read it (gain a random Rare card and a Burden curse)") { run, rng ->
                val rares = CardLibrary.rewardPool.filter { it.rarity == Rarity.RARE }
                val card = rares[rng.nextInt(rares.size)]
                run.addCard(card)
                run.addCard(CardLibrary.burden)
                "Forbidden knowledge floods your mind. You learn ${card.name} — and carry its price."
            },
            EventChoice("Burn it (gain 25 gold)") { run, _ ->
                run.gold += 25
                "The tome shrieks as it burns. Silver coins remain in the ashes."
            },
            EventChoice("Leave it") { _, _ -> "Some doors are better left closed." },
        ),
    )

    private val lostAdventurer = EventDef(
        "lost_adventurer", "Fallen Adventurer",
        "A skeleton in rusted mail slumps against the wall, purse still full.",
        listOf(
            EventChoice("Take the purse (gain 35 gold)") { run, _ ->
                run.gold += 35
                "The dead have no need of coin. +35 gold."
            },
            EventChoice("Bury them (heal 8 HP)") { run, _ ->
                run.hp = (run.hp + 8).coerceAtMost(run.maxHp)
                "You lay them to rest. A quiet peace strengthens you."
            },
        ),
    )

    val all = listOf(ancientShrine, moonlitPool, wanderingPeddler, cursedTome, lostAdventurer)

    fun random(rng: Random): EventDef = all[rng.nextInt(all.size)]
}
