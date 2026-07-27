package com.hexfall.core

import kotlin.random.Random

class ShopItemCard(val def: CardDef, val price: Int, var sold: Boolean = false)
class ShopItemRelic(val def: RelicDef, val price: Int, var sold: Boolean = false)

/**
 * One shop's inventory: five cards, two charms, and a one-time card-removal
 * service. The Merchant's Skull charm discounts everything.
 */
class ShopInventory(run: RunState, rng: Random) {

    val cards: List<ShopItemCard> = CardLibrary.randomRewardCards(rng, 5).map { def ->
        val base = when (def.rarity) {
            Rarity.RARE -> 140
            Rarity.UNCOMMON -> 75
            else -> 50
        }
        ShopItemCard(def, run.shopPrice(base + rng.nextInt(-5, 16)))
    }

    val relics: List<ShopItemRelic> = buildList {
        val pool = RelicLibrary.all.filterNot { run.hasRelic(it.id) }.shuffled(rng)
        pool.take(2).forEach { add(ShopItemRelic(it, run.shopPrice(it.price))) }
    }

    var removalUsed: Boolean = false
    val removalPrice: Int = run.shopPrice(75)

    fun buyCard(run: RunState, item: ShopItemCard): Boolean {
        if (item.sold || run.gold < item.price) return false
        run.gold -= item.price
        run.addCard(item.def)
        item.sold = true
        return true
    }

    fun buyRelic(run: RunState, item: ShopItemRelic): Boolean {
        if (item.sold || run.gold < item.price) return false
        run.gold -= item.price
        run.addRelic(item.def)
        item.sold = true
        return true
    }

    fun removeCard(run: RunState, card: CardInstance): Boolean {
        if (removalUsed || run.gold < removalPrice || card !in run.deck) return false
        run.gold -= removalPrice
        run.removeCard(card)
        removalUsed = true
        return true
    }
}
