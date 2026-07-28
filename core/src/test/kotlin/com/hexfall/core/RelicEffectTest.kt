package com.hexfall.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every charm must actually do what its description promises. A charm is
 * wired up by string id, so a typo silently produces a charm that reads well
 * and does nothing — these tests make that impossible to ship.
 */
class RelicEffectTest {

    private fun punchingBag(damage: Int = 0, hp: Int = 400) =
        EnemyDef("bag", "Bag", hp, hp) { _, _, _, _ -> EnemyMove.Attack(damage.coerceAtLeast(1)) }

    private fun runWith(vararg relics: RelicDef, seed: Long = 4L): RunState {
        val state = RunState(seed)
        relics.forEach { state.addRelic(it) }
        return state
    }

    private fun engine(state: RunState, vararg enemies: EnemyDef) =
        CombatEngine(state, enemies.toList(), Random(12))

    @Test
    fun `every charm in the pool is covered by a test in this file`() {
        // Guards against adding a charm and forgetting to prove it works.
        val source = java.io.File("src/test/kotlin/com/hexfall/core/RelicEffectTest.kt")
            .takeIf { it.exists() }
            ?.readText()
            ?: return
        val untested = (RelicLibrary.all + RelicLibrary.moonwellVial)
            .filterNot { source.contains("\"${it.id}\"") }
        assertTrue(untested.isEmpty(), "charms with no effect test: ${untested.map { it.id }}")
    }

    @Test
    fun `moonwell vial heals after combat`() {
        val state = runWith(RelicLibrary.moonwellVial)
        state.hp = 40
        state.afterCombatRelics(mutableListOf())
        assertEquals(44, state.hp)
        assertTrue(state.hasRelic("moonwell_vial"))
    }

    @Test
    fun `serpent fang envenoms an enemy at the start of combat`() {
        val e = engine(runWith(RelicLibrary.serpentFang), punchingBag())
        assertEquals(3, e.enemies.single().statusAmount(StatusType.VENOM))
        assertTrue(RelicLibrary.serpentFang.id == "serpent_fang")
    }

    @Test
    fun `doomkeepers bell seeds doom on every enemy`() {
        val e = engine(runWith(RelicLibrary.doomkeepersBell), punchingBag(), punchingBag())
        assertTrue(e.enemies.all { it.statusAmount(StatusType.DOOM) == 2 })
        assertTrue(RelicLibrary.doomkeepersBell.id == "doomkeepers_bell")
    }

    @Test
    fun `wolfpelt cloak grants mana only on full moon turns`() {
        val e = engine(runWith(RelicLibrary.wolfpeltCloak), punchingBag())
        assertEquals(MoonPhase.WAXING, e.moon)
        assertEquals(3, e.mana)
        e.endTurn()
        assertEquals(MoonPhase.FULL, e.moon)
        assertEquals(4, e.mana)
        e.endTurn()
        assertEquals(MoonPhase.WANING, e.moon)
        assertEquals(3, e.mana)
        assertTrue(RelicLibrary.wolfpeltCloak.id == "wolfpelt_cloak")
    }

    @Test
    fun `obsidian figurine opens combat with ward`() {
        val e = engine(runWith(RelicLibrary.obsidianFigurine), punchingBag())
        assertEquals(8, e.player.ward)
        assertTrue(RelicLibrary.obsidianFigurine.id == "obsidian_figurine")
    }

    @Test
    fun `scryers orb draws two extra cards on the first turn only`() {
        val plain = engine(runWith(), punchingBag())
        val withOrb = engine(runWith(RelicLibrary.scryersOrb), punchingBag())
        assertEquals(plain.hand.size + 2, withOrb.hand.size)
        withOrb.endTurn()
        assertEquals(5, withOrb.hand.size)
        assertTrue(RelicLibrary.scryersOrb.id == "scryers_orb")
    }

    @Test
    fun `bloodstone ring heals when an enemy dies`() {
        val state = runWith(RelicLibrary.bloodstoneRing)
        state.hp = 50
        val e = engine(state, punchingBag(hp = 1), punchingBag(hp = 400))
        val hpBefore = e.player.hp
        val bolt = e.hand.first { it.def.id == "moonbolt" }
        e.playCard(bolt, 0)
        assertEquals(hpBefore + 2, e.player.hp)
        assertTrue(RelicLibrary.bloodstoneRing.id == "bloodstone_ring")
    }

    @Test
    fun `hexwrought idol hexes every enemy at the start of combat`() {
        val e = engine(runWith(RelicLibrary.hexwroughtIdol), punchingBag(), punchingBag())
        assertTrue(e.enemies.all { it.statusAmount(StatusType.HEXED) == 3 })
        assertTrue(RelicLibrary.hexwroughtIdol.id == "hexwrought_idol")
    }

    @Test
    fun `silver crescent adds damage under a full moon only`() {
        val e = engine(runWith(RelicLibrary.silverCrescent), punchingBag())
        val enemy = e.enemies.single()
        assertEquals(6, e.attackDamage(e.player, enemy, 6))
        e.endTurn()
        assertEquals(MoonPhase.FULL, e.moon)
        assertEquals(8, e.attackDamage(e.player, enemy, 6))
        assertTrue(RelicLibrary.silverCrescent.id == "silver_crescent")
    }

    @Test
    fun `merchants skull discounts the whole shop`() {
        val plainRun = runWith()
        val cheapRun = runWith(RelicLibrary.merchantsSkull)
        plainRun.gold = 999
        cheapRun.gold = 999
        val plainShop = ShopInventory(plainRun, Random(3))
        val cheapShop = ShopInventory(cheapRun, Random(3))
        assertTrue(cheapShop.removalPrice < plainShop.removalPrice)
        cheapShop.cards.zip(plainShop.cards).forEach { (cheap, plain) ->
            assertTrue(cheap.price < plain.price, "${cheap.def.id} was not discounted")
        }
        assertTrue(RelicLibrary.merchantsSkull.id == "merchants_skull")
    }

    @Test
    fun `giants tooth raises max hp on pickup`() {
        val state = RunState(1L)
        val before = state.maxHp
        state.addRelic(RelicLibrary.giantsTooth)
        assertEquals(before + 10, state.maxHp)
        assertEquals(state.maxHp, state.hp)
        assertTrue(RelicLibrary.giantsTooth.id == "giants_tooth")
    }

    @Test
    fun `ashen hourglass grants mana every turn`() {
        val e = engine(runWith(RelicLibrary.ashenHourglass), punchingBag())
        assertEquals(4, e.mana)
        e.endTurn()
        assertEquals(4, e.mana)
        assertTrue(RelicLibrary.ashenHourglass.id == "ashen_hourglass")
    }

    @Test
    fun `thorn girdle opens combat with brambles that retaliate`() {
        val state = runWith(RelicLibrary.thornGirdle)
        val e = engine(state, punchingBag(damage = 4))
        assertEquals(3, e.player.statusAmount(StatusType.BRAMBLES))
        val enemy = e.enemies.single()
        val hpBefore = enemy.hp
        e.endTurn()
        assertEquals(hpBefore - 3, enemy.hp)
        assertTrue(RelicLibrary.thornGirdle.id == "thorn_girdle")
    }

    @Test
    fun `magpies eye increases combat gold`() {
        val state = runWith(RelicLibrary.magpiesEye)
        assertTrue(state.goldReward(100) > 100)
        assertTrue(RelicLibrary.magpiesEye.id == "magpies_eye")
    }

    @Test
    fun `owl feather quill draws an extra card every turn`() {
        val e = engine(runWith(RelicLibrary.owlQuill), punchingBag())
        assertEquals(6, e.hand.size)
        e.endTurn()
        assertEquals(6, e.hand.size)
        assertTrue(RelicLibrary.owlQuill.id == "owl_quill")
    }
}
