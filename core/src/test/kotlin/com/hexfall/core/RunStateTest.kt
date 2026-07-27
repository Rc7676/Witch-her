package com.hexfall.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RunStateTest {

    @Test
    fun `starter deck is ten cards with the moonwell vial`() {
        val run = RunState(1L)
        assertEquals(10, run.deck.size)
        assertEquals(4, run.deck.count { it.def.id == "moonbolt" })
        assertEquals(4, run.deck.count { it.def.id == "veil" })
        assertEquals(1, run.deck.count { it.def.id == "bloodprick" })
        assertEquals(1, run.deck.count { it.def.id == "lunar_tide" })
        assertTrue(run.hasRelic("moonwell_vial"))
    }

    @Test
    fun `upgrading a card swaps in the upgraded def once`() {
        val run = RunState(1L)
        val bolt = run.deck.first { it.def.id == "moonbolt" }
        assertTrue(run.upgradeCard(bolt))
        assertEquals("moonbolt+", bolt.def.id)
        assertFalse(run.upgradeCard(bolt))
    }

    @Test
    fun `giants tooth grants max hp on pickup`() {
        val run = RunState(1L)
        run.addRelic(RelicLibrary.giantsTooth)
        assertEquals(80, run.maxHp)
        assertEquals(80, run.hp)
    }

    @Test
    fun `duplicate relics are not added`() {
        val run = RunState(1L)
        run.addRelic(RelicLibrary.magpiesEye)
        run.addRelic(RelicLibrary.magpiesEye)
        assertEquals(1, run.relics.count { it.id == "magpies_eye" })
    }

    @Test
    fun `magpies eye boosts gold rewards`() {
        val run = RunState(1L)
        assertEquals(20, run.goldReward(20))
        run.addRelic(RelicLibrary.magpiesEye)
        assertEquals(26, run.goldReward(20))
    }

    @Test
    fun `merchants skull discounts shop prices`() {
        val run = RunState(2L)
        assertEquals(100, run.shopPrice(100))
        run.addRelic(RelicLibrary.merchantsSkull)
        assertEquals(80, run.shopPrice(100))
    }

    @Test
    fun `shop purchases spend gold and add cards`() {
        val run = RunState(2L)
        run.gold = 500
        val shop = ShopInventory(run, Random(9))
        val item = shop.cards.first()
        val deckBefore = run.deck.size
        assertTrue(shop.buyCard(run, item))
        assertEquals(500 - item.price, run.gold)
        assertEquals(deckBefore + 1, run.deck.size)
        assertFalse(shop.buyCard(run, item))
    }

    @Test
    fun `card removal service works once`() {
        val run = RunState(2L)
        run.gold = 200
        val shop = ShopInventory(run, Random(9))
        val card = run.deck.first()
        assertTrue(shop.removeCard(run, card))
        assertEquals(9, run.deck.size)
        assertFalse(shop.removeCard(run, run.deck.first()))
    }

    @Test
    fun `reward generation respects elite relic drops`() {
        val run = RunState(3L)
        val normal = CombatReward.forCombat(run, Random(1), elite = false)
        val elite = CombatReward.forCombat(run, Random(1), elite = true)
        assertEquals(3, normal.cardChoices.size)
        assertEquals(null, normal.relic)
        assertTrue(elite.relic != null)
        assertEquals(3, elite.cardChoices.distinctBy { it.id }.size)
    }

    @Test
    fun `events resolve without crashing and respect availability`() {
        for (event in EventLibrary.all) {
            for (choice in event.choices) {
                val run = RunState(7L)
                run.gold = 500
                if (choice.available(run)) {
                    val text = choice.resolve(run, Random(11))
                    assertTrue(text.isNotBlank())
                    assertTrue(run.gold >= 0)
                }
            }
        }
    }
}
