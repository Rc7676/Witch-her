package com.hexfall.core

import kotlin.random.Random

/**
 * One climb of the spire: the witch's HP, gold, deck, charms, and position
 * on the map. All randomness flows from [rng], seeded at run start.
 */
class RunState(val seed: Long) {

    val rng = Random(seed)

    var maxHp: Int = 70
    var hp: Int = 70
    var gold: Int = 99

    val deck: MutableList<CardInstance> = mutableListOf()
    val relics: MutableList<RelicDef> = mutableListOf()

    val map: SpireMap = SpireMap.generate(rng)
    var currentNodeId: Int? = null
    var floorsClimbed: Int = 0

    private var nextCardUid = 0

    init {
        repeat(4) { addCard(CardLibrary.moonbolt) }
        repeat(4) { addCard(CardLibrary.veil) }
        addCard(CardLibrary.bloodprick)
        addCard(CardLibrary.lunarTide)
        relics += RelicLibrary.moonwellVial
    }

    fun addCard(def: CardDef): CardInstance =
        CardInstance(nextCardUid++, def).also { deck += it }

    fun removeCard(card: CardInstance) {
        deck.remove(card)
    }

    fun upgradeCard(card: CardInstance): Boolean {
        val upgraded = CardLibrary.upgradedVersion(card.def) ?: return false
        card.def = upgraded
        return true
    }

    fun hasRelic(id: String): Boolean = relics.any { it.id == id }

    fun addRelic(relic: RelicDef) {
        if (hasRelic(relic.id)) return
        relics += relic
        if (relic.id == "giants_tooth") {
            maxHp += 10
            hp += 10
        }
    }

    /** Post-combat healing and gold from charms. */
    fun afterCombatRelics(log: MutableList<String>) {
        if (hasRelic("moonwell_vial")) {
            hp = (hp + 4).coerceAtMost(maxHp)
            log += "The Moonwell Vial restores 4 HP."
        }
    }

    fun goldReward(base: Int): Int =
        if (hasRelic("magpies_eye")) (base * 1.3).toInt() else base

    /** Shop price after charm discounts. */
    fun shopPrice(base: Int): Int =
        if (hasRelic("merchants_skull")) (base * 4) / 5 else base

    fun moveTo(node: MapNode) {
        currentNodeId = node.id
        floorsClimbed = node.row + 1
    }
}
