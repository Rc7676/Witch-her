package com.hexfall.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
        is EnemyMove.Defend -> "🛡 ${move.block}"
        is EnemyMove.AttackDefend -> "⚔ ${engine.intentDamage(enemy)} 🛡 ${move.block}"
        is EnemyMove.Buff -> "↑ ${move.label}"
        is EnemyMove.Debuff -> "☠ ${move.label}"
        is EnemyMove.HealSelf -> "✚ ${move.label}"
    }

private fun statusLine(combatant: Combatant): String =
    combatant.statuses.entries.joinToString(" ") { (type, amount) ->
        "${statusIcon(type)}$amount"
    }

@Composable
fun CombatScreen(vm: GameViewModel) {
    val engine = vm.combat ?: return
    var selected by remember { mutableStateOf<CardInstance?>(null) }

    Column(Modifier.fillMaxSize()) {
        RunHeader(vm)

        // --- Enemies -----------------------------------------------------
        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            engine.enemies.forEachIndexed { index, enemy ->
                val targeting = selected?.def?.needsTarget == true
                Column(
                    Modifier
                        .weight(1f)
                        .border(
                            width = if (targeting) 2.5.dp else 1.dp,
                            color = if (targeting) HexfallColors.gold else Color(0x33EADFC8),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .background(HexfallColors.surface, RoundedCornerShape(12.dp))
                        .clickable(enabled = targeting) {
                            selected?.let { card ->
                                vm.playCard(card, index)
                            }
                            selected = null
                        }
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        intentText(engine, enemy),
                        color = HexfallColors.energyAmber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("👹", fontSize = 34.sp)
                    Text(
                        enemy.def.name,
                        color = HexfallColors.parchment,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    StatBar(enemy.hp, enemy.maxHp, HexfallColors.hpRed, Modifier.fillMaxWidth())
                    if (enemy.block > 0) {
                        Text(
                            "🛡 ${enemy.block}",
                            color = HexfallColors.blockBlue,
                            fontSize = 11.sp,
                        )
                    }
                    val statuses = statusLine(enemy)
                    if (statuses.isNotEmpty()) {
                        Text(statuses, fontSize = 10.sp, color = HexfallColors.poisonGreen)
                    }
                }
            }
        }

        // --- Player row --------------------------------------------------
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .background(HexfallColors.energyAmber, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${engine.energy}",
                    color = Color.Black,
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
                    if (engine.player.block > 0) {
                        Text(
                            "🛡 ${engine.player.block}  ",
                            color = HexfallColors.blockBlue,
                            fontSize = 11.sp,
                        )
                    }
                    Text(
                        statusLine(engine.player),
                        fontSize = 11.sp,
                        color = HexfallColors.parchment,
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

        // --- Hand --------------------------------------------------------
        Text(
            if (selected != null) "Tap an enemy to strike" else "Turn ${engine.turn}" +
                "   Draw ${engine.drawPile.size} · Discard ${engine.discardPile.size}",
            color = HexfallColors.parchment.copy(alpha = 0.6f),
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        LazyRow(
            Modifier
                .fillMaxWidth()
                .height(166.dp)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
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
