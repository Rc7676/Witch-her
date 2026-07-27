package com.hexfall.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.hexfall.core.CardDef
import com.hexfall.core.CardInstance
import com.hexfall.core.CombatEngine
import com.hexfall.core.CombatResult
import com.hexfall.core.CombatReward
import com.hexfall.core.EnemyLibrary
import com.hexfall.core.EventDef
import com.hexfall.core.EventLibrary
import com.hexfall.core.MapNode
import com.hexfall.core.NodeType
import com.hexfall.core.RelicDef
import com.hexfall.core.RelicLibrary
import com.hexfall.core.RunState
import com.hexfall.core.ShopInventory
import kotlin.random.Random

sealed interface GameScreen {
    data object Title : GameScreen
    data object Map : GameScreen
    data object Combat : GameScreen
    data class Reward(val reward: CombatReward) : GameScreen
    data class Event(val def: EventDef) : GameScreen
    data class Shop(val inventory: ShopInventory) : GameScreen
    data object Rest : GameScreen
    data class Treasure(val relic: RelicDef?, val gold: Int) : GameScreen
    data class GameOver(val victory: Boolean, val floors: Int) : GameScreen
}

/**
 * Holds the current run and routes between screens. The core engine is
 * mutable, so every action bumps [version]; the UI keys recomposition on it.
 */
class GameViewModel : ViewModel() {

    var screen by mutableStateOf<GameScreen>(GameScreen.Title)
        private set
    var version by mutableIntStateOf(0)
        private set

    var run: RunState? = null
        private set
    var combat: CombatEngine? = null
        private set

    var eventOutcome by mutableStateOf<String?>(null)
        private set
    var rewardCardTaken by mutableStateOf(false)
        private set
    var rewardRelicTaken by mutableStateOf(false)
        private set

    private var combatElite = false
    private var combatBoss = false

    private fun bump() {
        version++
    }

    // --- Run lifecycle ---------------------------------------------------

    fun newRun() {
        run = RunState(Random.nextLong())
        combat = null
        screen = GameScreen.Map
        bump()
    }

    fun abandonRun() {
        run = null
        combat = null
        screen = GameScreen.Title
        bump()
    }

    fun backToMap() {
        eventOutcome = null
        screen = GameScreen.Map
        bump()
    }

    // --- Map -------------------------------------------------------------

    fun availableNodeIds(): Set<Int> {
        val r = run ?: return emptySet()
        return r.map.available(r.currentNodeId).map { it.id }.toSet()
    }

    fun chooseNode(node: MapNode) {
        val r = run ?: return
        if (node.id !in availableNodeIds()) return
        r.moveTo(node)
        when (node.type) {
            NodeType.MONSTER, NodeType.ELITE, NodeType.BOSS -> startCombat(node)
            NodeType.EVENT -> {
                eventOutcome = null
                screen = GameScreen.Event(EventLibrary.random(r.rng))
            }
            NodeType.REST -> screen = GameScreen.Rest
            NodeType.SHOP -> screen = GameScreen.Shop(ShopInventory(r, r.rng))
            NodeType.TREASURE -> screen = GameScreen.Treasure(
                relic = RelicLibrary.randomNew(r.rng, r.relics),
                gold = 20 + r.rng.nextInt(16),
            )
        }
        bump()
    }

    // --- Combat ----------------------------------------------------------

    private fun startCombat(node: MapNode) {
        val r = run ?: return
        combatElite = node.type == NodeType.ELITE
        combatBoss = node.type == NodeType.BOSS
        val enemies = when (node.type) {
            NodeType.ELITE -> EnemyLibrary.eliteEncounter(r.rng)
            NodeType.BOSS -> EnemyLibrary.bossEncounter()
            else -> EnemyLibrary.normalEncounter(node.row, r.rng)
        }
        combat = CombatEngine(r, enemies, r.rng)
        screen = GameScreen.Combat
    }

    fun playCard(card: CardInstance, targetIndex: Int?) {
        val engine = combat ?: return
        engine.playCard(card, targetIndex)
        afterCombatAction(engine)
    }

    fun endTurn() {
        val engine = combat ?: return
        engine.endTurn()
        afterCombatAction(engine)
    }

    private fun afterCombatAction(engine: CombatEngine) {
        val r = run ?: return
        when (engine.result) {
            CombatResult.DEFEAT -> {
                combat = null
                screen = GameScreen.GameOver(victory = false, floors = r.floorsClimbed)
            }
            CombatResult.VICTORY -> {
                r.afterCombatRelics(engine.log)
                combat = null
                if (combatBoss) {
                    screen = GameScreen.GameOver(victory = true, floors = r.floorsClimbed)
                } else {
                    val reward = CombatReward.forCombat(r, r.rng, combatElite)
                    r.gold += reward.gold
                    rewardCardTaken = false
                    rewardRelicTaken = false
                    screen = GameScreen.Reward(reward)
                }
            }
            null -> Unit
        }
        bump()
    }

    // --- Rewards ---------------------------------------------------------

    fun takeRewardCard(def: CardDef) {
        val r = run ?: return
        if (rewardCardTaken) return
        r.addCard(def)
        rewardCardTaken = true
        bump()
    }

    fun takeRewardRelic(relic: RelicDef) {
        val r = run ?: return
        if (rewardRelicTaken) return
        r.addRelic(relic)
        rewardRelicTaken = true
        bump()
    }

    // --- Events ----------------------------------------------------------

    fun chooseEventOption(event: EventDef, index: Int) {
        val r = run ?: return
        if (eventOutcome != null) return
        val choice = event.choices.getOrNull(index) ?: return
        if (!choice.available(r)) return
        eventOutcome = choice.resolve(r, r.rng)
        bump()
    }

    // --- Shop ------------------------------------------------------------

    fun shopBuyCard(inventory: ShopInventory, index: Int) {
        val r = run ?: return
        inventory.buyCard(r, inventory.cards[index])
        bump()
    }

    fun shopBuyRelic(inventory: ShopInventory, index: Int) {
        val r = run ?: return
        inventory.buyRelic(r, inventory.relics[index])
        bump()
    }

    fun shopRemoveCard(inventory: ShopInventory, card: CardInstance) {
        val r = run ?: return
        inventory.removeCard(r, card)
        bump()
    }

    // --- Rest ------------------------------------------------------------

    fun restHeal() {
        val r = run ?: return
        r.hp = (r.hp + (r.maxHp * 3) / 10).coerceAtMost(r.maxHp)
        backToMap()
    }

    fun restUpgrade(card: CardInstance) {
        val r = run ?: return
        r.upgradeCard(card)
        backToMap()
    }

    // --- Treasure --------------------------------------------------------

    fun openTreasure(treasure: GameScreen.Treasure) {
        val r = run ?: return
        treasure.relic?.let { r.addRelic(it) }
        r.gold += treasure.gold
        backToMap()
    }
}
