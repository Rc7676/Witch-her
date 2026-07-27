package com.hexfall.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hexfall.game.GameViewModel
import com.hexfall.core.MapNode
import com.hexfall.core.NodeType
import com.hexfall.core.SpireMap
import kotlin.math.roundToInt

fun nodeIcon(type: NodeType): String = when (type) {
    NodeType.MONSTER -> "⚔"
    NodeType.ELITE -> "👹"
    NodeType.EVENT -> "❓"
    NodeType.REST -> "🔥"
    NodeType.SHOP -> "💰"
    NodeType.TREASURE -> "🎁"
    NodeType.BOSS -> "💀"
}

private const val ROW_HEIGHT_DP = 74f
private const val NODE_SIZE_DP = 46f

@Composable
fun MapScreen(vm: GameViewModel) {
    val run = vm.run ?: return
    val map = run.map
    val available = vm.availableNodeIds()
    var showDeck by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        RunHeader(vm, onDeck = { showDeck = true })

        val scroll = rememberScrollState()
        val totalRows = SpireMap.ROWS + 1
        val mapHeight = (totalRows * ROW_HEIGHT_DP).dp

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scroll),
        ) {
            val widthDp = maxWidth
            val density = LocalDensity.current
            val widthPx = with(density) { widthDp.toPx() }
            val rowHeightPx = with(density) { ROW_HEIGHT_DP.dp.toPx() }
            val mapHeightPx = totalRows * rowHeightPx
            val nodeSizePx = with(density) { NODE_SIZE_DP.dp.toPx() }

            fun centerOf(node: MapNode): Offset {
                val x = (node.col + 0.5f) / SpireMap.COLS * widthPx
                val y = mapHeightPx - (node.row + 0.5f) * rowHeightPx
                return Offset(x, y)
            }

            // Keep the witch's current position (or the start) in view.
            // viewport = content height - max scroll, so this needs maxValue.
            LaunchedEffect(scroll.maxValue) {
                if (scroll.maxValue > 0) {
                    val viewport = mapHeightPx - scroll.maxValue
                    val row = run.currentNodeId?.let { map.node(it).row } ?: -1
                    val nodeY = mapHeightPx - (row + 0.5f) * rowHeightPx
                    val target = (nodeY - viewport + 1.5f * rowHeightPx).toInt()
                    scroll.scrollTo(target.coerceIn(0, scroll.maxValue))
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(mapHeight),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    map.nodes.forEach { node ->
                        val from = centerOf(node)
                        node.next.forEach { nextId ->
                            drawLine(
                                color = Color(0x55EADFC8),
                                start = from,
                                end = centerOf(map.node(nextId)),
                                strokeWidth = 3f,
                            )
                        }
                    }
                }

                map.nodes.forEach { node ->
                    val center = centerOf(node)
                    val reachable = node.id in available
                    val isCurrent = node.id == run.currentNodeId
                    Box(
                        Modifier
                            .offset {
                                IntOffset(
                                    (center.x - nodeSizePx / 2).roundToInt(),
                                    (center.y - nodeSizePx / 2).roundToInt(),
                                )
                            }
                            .size(NODE_SIZE_DP.dp)
                            .background(
                                when {
                                    isCurrent -> HexfallColors.gold
                                    reachable -> HexfallColors.surfaceLight
                                    else -> HexfallColors.surface
                                },
                                CircleShape,
                            )
                            .border(
                                width = if (reachable) 2.5.dp else 1.dp,
                                color = when {
                                    reachable -> HexfallColors.gold
                                    isCurrent -> HexfallColors.gold
                                    else -> Color(0x33EADFC8)
                                },
                                shape = CircleShape,
                            )
                            .clickable(enabled = reachable) { vm.chooseNode(node) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(nodeIcon(node.type), fontSize = 20.sp)
                    }
                }
            }
        }

        Text(
            "⚔ Fight   👹 Elite   ❓ Event   💰 Shop   🔥 Rest   🎁 Treasure   💀 Boss",
            color = HexfallColors.parchment.copy(alpha = 0.55f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(HexfallColors.surface)
                .padding(vertical = 6.dp),
        )
    }

    if (showDeck) {
        DeckDialog(
            title = "Your Deck (${run.deck.size})",
            cards = run.deck,
            onDismiss = { showDeck = false },
        )
    }
}

/** Shared header: HP, gold, floor, relic icons, deck button. */
@Composable
fun RunHeader(vm: GameViewModel, onDeck: (() -> Unit)? = null) {
    val run = vm.run ?: return
    Column(
        Modifier
            .fillMaxWidth()
            .background(HexfallColors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "❤ ${run.hp}/${run.maxHp}",
                color = HexfallColors.hpRed,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text(
                "   🪙 ${run.gold}",
                color = HexfallColors.gold,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text(
                "   ⛰ ${run.floorsClimbed}/${SpireMap.ROWS + 1}",
                color = HexfallColors.parchment,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            if (onDeck != null) {
                TextButton(onClick = onDeck) {
                    Text("Deck (${run.deck.size})", color = HexfallColors.purple, fontSize = 13.sp)
                }
            }
        }
        if (run.relics.isNotEmpty()) {
            Text(
                run.relics.joinToString("  ") { "🔮 ${it.name}" },
                color = HexfallColors.parchment.copy(alpha = 0.7f),
                fontSize = 10.sp,
                maxLines = 2,
            )
        }
    }
}
