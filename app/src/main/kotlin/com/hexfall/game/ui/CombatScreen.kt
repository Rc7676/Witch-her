package com.hexfall.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hexfall.core.CardInstance
import com.hexfall.core.CombatEngine
import com.hexfall.core.Combatant
import com.hexfall.core.EnemyCombatant
import com.hexfall.core.EnemyMove
import com.hexfall.game.GameViewModel
import kotlin.math.roundToInt

private fun intentText(engine: CombatEngine, enemy: EnemyCombatant): String =
    when (val move = enemy.nextMove) {
        is EnemyMove.Attack -> {
            val dmg = engine.intentDamage(enemy) ?: move.damage
            if (move.times > 1) "⚔ $dmg×${move.times}" else "⚔ $dmg"
        }
        is EnemyMove.Guard -> "🛡 ${move.ward}"
        is EnemyMove.AttackGuard -> "⚔ ${engine.intentDamage(enemy)} 🛡 ${move.ward}"
        is EnemyMove.Buff -> "↑ ${move.label}"
        is EnemyMove.Debuff -> "☠ ${move.label}"
        is EnemyMove.HealSelf -> "✚ ${move.label}"
        is EnemyMove.Steal -> "⚔ ${engine.intentDamage(enemy)} 🪙 ${move.label}"
    }

private fun statusLine(combatant: Combatant): String =
    combatant.statuses.entries.joinToString(" ") { (type, amount) ->
        "${statusIcon(type)}$amount"
    }

@Composable
fun CombatScreen(vm: GameViewModel, handScroll: LazyListState = rememberLazyListState()) {
    val engine = vm.combat ?: return
    val density = LocalDensity.current

    var inspectCard by remember { mutableStateOf<CardInstance?>(null) }
    var inspectEnemy by remember { mutableStateOf<EnemyCombatant?>(null) }
    var showDeck by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    // Drag-to-cast state, all in root coordinates.
    var dragCard by remember { mutableStateOf<CardInstance?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    val enemyBounds = remember { mutableStateMapOf<Int, Rect>() }
    var battlefieldBounds by remember { mutableStateOf(Rect.Zero) }
    var contentOrigin by remember { mutableStateOf(Offset.Zero) }

    val draggingTargeted = dragCard?.def?.needsTarget == true
    val draggingSelf = dragCard != null && dragCard?.def?.needsTarget == false
    // One slack value for both the highlight and the drop test, so a card
    // always lands on the enemy the glow says it will. Generous, because a
    // fingertip is far bigger than the pixel it reports.
    val targetSlack = with(density) { 28.dp.toPx() }

    /**
     * The enemy a spell dropped at [point] would hit: the one under the
     * finger, or else the nearest one if the finger is anywhere over the
     * battlefield. Fingers are imprecise, so landing near a foe is enough.
     *
     * Deliberately a function rather than a composition value. The gesture
     * callbacks below run inside a pointerInput coroutine that is NOT
     * restarted on recomposition, so any composition value they capture is
     * frozen at drag start — reading snapshot state through a function keeps
     * them live. Capturing the target by value here is exactly what stopped
     * drag-to-cast from ever landing.
     */
    fun dropTarget(point: Offset): Int? {
        val under = enemyBounds.entries
            .firstOrNull { it.value.inflate(targetSlack).contains(point) }
            ?.key
        if (under != null) return under
        if (!battlefieldBounds.contains(point)) return null
        return enemyBounds.entries
            .minByOrNull { (it.value.center - point).getDistance() }
            ?.key
    }

    // The glow uses the same function as the drop, so it can never point at
    // an enemy the card will not actually hit.
    val hoveredEnemy: Int? = if (draggingTargeted) dropTarget(dragPos) else null
    val hoveringSelf = draggingSelf && battlefieldBounds.contains(dragPos)

    fun finishDrag() {
        val card = dragCard
        dragCard = null
        if (card == null || !engine.canPlay(card)) return
        val dropPoint = dragPos // read live, never captured
        if (card.def.needsTarget) {
            dropTarget(dropPoint)?.let { vm.playCard(card, it) }
        } else if (battlefieldBounds.contains(dropPoint)) {
            vm.playCard(card, null)
        }
    }

    NightScene(moon = engine.moon, starSeed = 11) {
        Box(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { contentOrigin = it.boundsInRoot().topLeft },
        ) {
            Column(Modifier.fillMaxSize()) {
                RunHeader(vm, onDeck = { showDeck = true }, onHelp = { showHelp = true })

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MoonIcon(engine.moon, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        engine.moon.displayName + "  ·  Turn ${engine.turn}",
                        color = Art.moonGlow.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // --- Battlefield: enemies, log, the witch. Dropping a
                // self-cast spell anywhere in here casts it. ------------
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .onGloballyPositioned { battlefieldBounds = it.boundsInRoot() }
                        .then(
                            if (draggingSelf) {
                                Modifier
                                    .border(
                                        width = if (hoveringSelf) 3.dp else 1.5.dp,
                                        color = HexfallColors.purple.copy(
                                            alpha = if (hoveringSelf) 0.95f else 0.5f,
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                    )
                                    .background(
                                        HexfallColors.purple.copy(
                                            alpha = if (hoveringSelf) 0.12f else 0.05f,
                                        ),
                                        RoundedCornerShape(14.dp),
                                    )
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(
                                10.dp, Alignment.CenterHorizontally,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            engine.enemies.forEachIndexed { index, enemy ->
                                EnemyColumn(
                                    engine = engine,
                                    enemy = enemy,
                                    highlight = draggingTargeted,
                                    hovered = hoveredEnemy == index,
                                    onPositioned = { enemyBounds[index] = it },
                                    onTap = { if (dragCard == null) inspectEnemy = enemy },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        if (engine.log.isNotEmpty()) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .background(Color(0x990D0918), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                engine.log.takeLast(3).forEach { line ->
                                    Text(
                                        line,
                                        color = HexfallColors.parchment.copy(alpha = 0.65f),
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Canvas(Modifier.size(44.dp)) { drawWitch(tint = Color(0xFF1A1130)) }
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier
                                    .size(46.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color(0xFFFFE29A),
                                                HexfallColors.energyAmber,
                                            ),
                                        ),
                                        CircleShape,
                                    )
                                    .border(1.5.dp, Color(0xFF7A5A1E), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${engine.mana}",
                                    color = Color(0xFF3A2A08),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                StatBar(
                                    engine.player.hp,
                                    engine.player.maxHp,
                                    HexfallColors.hpRed,
                                    Modifier.fillMaxWidth(),
                                )
                                Row {
                                    if (engine.player.ward > 0) {
                                        Text(
                                            "🛡 ${engine.player.ward}  ",
                                            color = HexfallColors.blockBlue,
                                            fontSize = 11.sp,
                                        )
                                    }
                                    Text(
                                        statusLine(engine.player),
                                        fontSize = 11.sp,
                                        color = HexfallColors.parchment,
                                        maxLines = 1,
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            ArcaneButton(
                                text = "End Turn",
                                onClick = { vm.endTurn() },
                            )
                        }
                    }
                }

                // --- Hand ------------------------------------------------
                Text(
                    when {
                        draggingTargeted -> "Drop on an enemy to strike"
                        draggingSelf -> "Drop on the battlefield to cast"
                        else ->
                            "Tap a card to inspect · drag to cast   |   " +
                                "Draw ${engine.drawPile.size} · " +
                                "Discard ${engine.discardPile.size} · " +
                                "Exhaust ${engine.exhaustPile.size}"
                    },
                    color = if (dragCard != null) {
                        HexfallColors.gold
                    } else {
                        HexfallColors.parchment.copy(alpha = 0.6f)
                    },
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
                LazyRow(
                    Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .padding(vertical = 6.dp),
                    state = handScroll,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    items(engine.hand, key = { it.uid }) { card ->
                        var origin by remember(card.uid) { mutableStateOf(Offset.Zero) }
                        CardView(
                            def = card.def,
                            enabled = engine.canPlay(card),
                            modifier = Modifier
                                .onGloballyPositioned { origin = it.boundsInRoot().topLeft }
                                .graphicsLayer {
                                    alpha = if (dragCard?.uid == card.uid) 0.25f else 1f
                                }
                                .pointerInput(card.uid) {
                                    detectTapGestures(onTap = { inspectCard = card })
                                }
                                .pointerInput(card.uid) {
                                    detectDragGestures(
                                        onDragStart = { startPos ->
                                            if (engine.canPlay(card)) {
                                                dragCard = card
                                                dragPos = origin + startPos
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            dragPos = origin + change.position
                                        },
                                        onDragEnd = { finishDrag() },
                                        onDragCancel = { dragCard = null },
                                    )
                                },
                        )
                    }
                }
            }

            // Ghost card following the finger while dragging.
            dragCard?.let { card ->
                val cardW = with(density) { 108.dp.toPx() }
                val cardH = with(density) { 160.dp.toPx() }
                CardView(
                    def = card.def,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (dragPos.x - contentOrigin.x - cardW / 2f).roundToInt(),
                                (dragPos.y - contentOrigin.y - cardH * 0.95f).roundToInt(),
                            )
                        }
                        .graphicsLayer {
                            alpha = 0.9f
                            scaleX = 1.08f
                            scaleY = 1.08f
                        },
                )
            }
        }
    }

    // --- Dialogs ---------------------------------------------------------
    inspectCard?.let { card ->
        CardInspectDialog(
            def = card.def,
            canCast = engine.canPlay(card),
            enemies = engine.enemies,
            onCast = { target ->
                inspectCard = null
                vm.playCard(card, target)
            },
            onDismiss = { inspectCard = null },
        )
    }
    inspectEnemy?.let { enemy ->
        EnemyInspectDialog(engine, enemy, onDismiss = { inspectEnemy = null })
    }
    if (showHelp) HelpDialog(onDismiss = { showHelp = false })
    if (showDeck) {
        val run = vm.run
        if (run != null) {
            DeckDialog(
                title = "Your Grimoire (${run.deck.size})",
                cards = run.deck,
                onDismiss = { showDeck = false },
            )
        }
    }
}

/** One enemy: intent badge, figure with target glow, HP, statuses. */
@Composable
private fun EnemyColumn(
    engine: CombatEngine,
    enemy: EnemyCombatant,
    highlight: Boolean,
    hovered: Boolean,
    onPositioned: (Rect) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .onGloballyPositioned { onPositioned(it.boundsInRoot()) }
            .clickable { onTap() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .background(Color(0xB30D0918), RoundedCornerShape(10.dp))
                .border(
                    1.dp,
                    HexfallColors.energyAmber.copy(alpha = 0.5f),
                    RoundedCornerShape(10.dp),
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                intentText(engine, enemy),
                color = HexfallColors.energyAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(contentAlignment = Alignment.Center) {
            if (highlight) {
                Canvas(Modifier.size(100.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                HexfallColors.gold.copy(alpha = if (hovered) 0.75f else 0.35f),
                                Color.Transparent,
                            ),
                        ),
                        radius = size.minDimension / 2f,
                    )
                    drawCircle(
                        color = HexfallColors.gold.copy(alpha = if (hovered) 1f else 0.5f),
                        radius = size.minDimension / 2f - 4f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = if (hovered) 6f else 3f,
                        ),
                    )
                }
            }
            EnemyFigure(
                enemy.def.id,
                Modifier
                    .size(88.dp)
                    .graphicsLayer {
                        if (hovered) {
                            scaleX = 1.12f
                            scaleY = 1.12f
                        }
                    },
            )
        }
        Text(
            enemy.def.name,
            color = HexfallColors.parchment,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        StatBar(enemy.hp, enemy.maxHp, HexfallColors.hpRed, Modifier.fillMaxWidth())
        Row {
            if (enemy.ward > 0) {
                Text("🛡${enemy.ward} ", color = HexfallColors.blockBlue, fontSize = 10.sp)
            }
            val statuses = statusLine(enemy)
            if (statuses.isNotEmpty()) {
                Text(
                    statuses,
                    fontSize = 10.sp,
                    color = HexfallColors.poisonGreen,
                    maxLines = 1,
                )
            }
        }
    }
}
