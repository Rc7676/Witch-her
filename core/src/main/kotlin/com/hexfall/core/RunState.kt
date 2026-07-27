package com.hexfall.core

import kotlin.random.Random

/**
 * One climb of the spire: the witch's HP, gold, deck, relics, and position
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
        repeat(5) { addCard(CardLibrary.hexBolt) }
        repeat(4) { addCard(CardLibrary.ward) }
        addCard(CardLibrary.eldritchBlast)
        relics += RelicLibrary.witchwoodCharm
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
        when (relic.id) {
            "heart_amulet" -> {
                maxHp += 12
                hp += 12
            }
            "raven_feather" -> {
                maxHp += 10
                hp += 10
            }
        }
    }

    /** Post-combat healing and gold from relics. */
    fun afterCombatRelics(log: MutableList<String>) {
        if (hasRelic("witchwood_charm")) {
            hp = (hp + 3).coerceAtMost(maxHp)
            log += "Witchwood Charm heals 3 HP."
        }
        if (hasRelic("cauldron")) {
            hp = (hp + 8).coerceAtMost(maxHp)
            log += "Traveling Cauldron heals 8 HP."
        }
        if (hasRelic("bone_dice")) {
            gold += 15
            log += "Bone Dice rattle: +15 gold."
        }
    }

    fun goldReward(base: Int): Int =
        if (hasRelic("lucky_coin")) (base * 1.3).toInt() else base

    fun moveTo(node: MapNode) {
        currentNodeId = node.id
        floorsClimbed = node.row + 1
    }
}
