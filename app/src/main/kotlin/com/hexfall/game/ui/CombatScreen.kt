package com.hexfall.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hexfall.core.CardInstance
import com.hexfall.core.CombatEngine
import com.hexfall.core.Combatant
import com.hexfall.core.EnemyCombatant
import com.hexfall.core.EnemyMove
import com.hexfall.game.GameViewModel

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
fun CombatScreen(vm: GameViewModel) {
    val engine = vm.combat ?: return
    var selected by remember { mutableStateOf<CardInstance?>(null) }
    var showDeck by remember { mutableStateOf(false) }

    NightScene(moon = engine.moon, starSeed = 11) {
        Column(Modifier.fillMaxSize()) {
            RunHeader(vm, onDeck = { showDeck = true })

            // Moon phase banner.
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

            // --- Enemies -------------------------------------------------
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                engine.enemies.forEachIndexed { index, enemy ->
                    val targeting = selected?.def?.needsTarget == true
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable(enabled = targeting) {
                                selected?.let { card -> vm.playCard(card, index) }
                                selected = null
                            },
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
                            if (targeting) {
                                Canvas(Modifier.size(96.dp)) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            listOf(
                                                HexfallColors.gold.copy(alpha = 0.45f),
                                                Color.Transparent,
                                            ),
                                        ),
                                        radius = size.minDimension / 2f,
                                    )
                                }
                            }
                            EnemyFigure(enemy.def.id, Modifier.size(88.dp))
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
                                Text(
                                    "🛡${enemy.ward} ",
                                    color = HexfallColors.blockBlue,
                                    fontSize = 10.sp,
                                )
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
            }

            // --- Combat log ----------------------------------------------
            if (engine.log.isNotEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
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

            // --- Player row ----------------------------------------------
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(44.dp)) { drawWitch(tint = Color(0xFF1A1130)) }
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .size(46.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFFFE29A), HexfallColors.energyAmber),
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
                Button(
                    onClick = {
                        selected = null
                        vm.endTurn()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HexfallColors.purple),
                ) {
                    Text("End Turn", fontSize = 13.sp)
                }
            }

            // --- Hand ----------------------------------------------------
            Text(
                if (selected != null) {
                    "Tap an enemy to strike"
                } else {
                    "Draw ${engine.drawPile.size} · Discard ${engine.discardPile.size} · " +
                        "Exhaust ${engine.exhaustPile.size}"
                },
                color = HexfallColors.parchment.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
            LazyRow(
                Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                items(engine.hand, key = { it.uid }) { card ->
                    CardView(
                        def = card.def,
                        enabled = engine.canPlay(card),
                        selected = card == selected,
                        onClick = {
                            when {
                                !engine.canPlay(card) -> Unit
                                card == selected -> selected = null
                                card.def.needsTarget -> selected = card
                                else -> {
                                    selected = null
                                    vm.playCard(card, null)
                                }
                            }
                        },
                    )
                }
            }
        }
    }

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
