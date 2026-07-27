package com.hexfall.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hexfall.core.CardDef
import com.hexfall.core.CardInstance
import com.hexfall.core.CardType
import com.hexfall.core.StatusType

fun cardColor(type: CardType): Color = when (type) {
    CardType.ATTACK -> HexfallColors.attackRed
    CardType.SKILL -> HexfallColors.skillBlue
    CardType.POWER -> HexfallColors.powerViolet
    CardType.CURSE -> HexfallColors.curseGrey
}

fun statusIcon(type: StatusType): String = when (type) {
    StatusType.STRENGTH -> "💪"
    StatusType.DEXTERITY -> "🐾"
    StatusType.WEAK -> "💧"
    StatusType.VULNERABLE -> "🎯"
    StatusType.FRAIL -> "🥀"
    StatusType.POISON -> "☠"
    StatusType.REGEN -> "✚"
    StatusType.THORNS -> "🌵"
    StatusType.RITUAL -> "🕯"
    StatusType.ENERGIZE -> "⚡"
    StatusType.DECAY -> "🩸"
    StatusType.IMMOLATE -> "🔥"
}

/** A hand/deck card. [enabled] dims unplayable cards; [selected] adds a glow. */
@Composable
fun CardView(
    def: CardDef,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val borderColor = when {
        selected -> HexfallColors.gold
        enabled -> cardColor(def.type)
        else -> HexfallColors.curseGrey
    }
    var m = modifier
        .width(104.dp)
        .height(150.dp)
        .border(if (selected) 3.dp else 1.5.dp, borderColor, RoundedCornerShape(10.dp))
        .background(
            if (enabled) HexfallColors.surfaceLight else HexfallColors.surface,
            RoundedCornerShape(10.dp),
        )
    if (onClick != null) m = m.clickable { onClick() }

    Column(m.padding(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (def.playable) {
                Box(
                    Modifier
                        .size(22.dp)
                        .background(HexfallColors.energyAmber, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${def.cost}",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
            Text(
                def.name,
                color = if (def.upgraded) HexfallColors.gold else HexfallColors.parchment,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 12.sp,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(cardColor(def.type).copy(alpha = 0.35f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(def.type.name, fontSize = 8.sp, color = HexfallColors.parchment)
        }
        Spacer(Modifier.height(5.dp))
        Text(
            def.description,
            color = HexfallColors.parchment.copy(alpha = 0.9f),
            fontSize = 10.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Simple labeled bar (HP etc.). */
@Composable
fun StatBar(current: Int, max: Int, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(14.dp)
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(7.dp)),
    ) {
        val fraction = if (max <= 0) 0f else (current.toFloat() / max).coerceIn(0f, 1f)
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(color, RoundedCornerShape(7.dp)),
        )
        Text(
            "$current/$max",
            color = Color.White,
            fontSize = 9.sp,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/**
 * Full-deck list dialog. When [onPick] is set, tapping a card selects it
 * (used for upgrades and removals); otherwise it is a read-only viewer.
 */
@Composable
fun DeckDialog(
    title: String,
    cards: List<CardInstance>,
    onDismiss: () -> Unit,
    onPick: ((CardInstance) -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = HexfallColors.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    title,
                    color = HexfallColors.gold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(cards) { card ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(HexfallColors.surfaceLight, RoundedCornerShape(8.dp))
                                .let { m ->
                                    if (onPick != null) m.clickable { onPick(card) } else m
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (card.def.playable) {
                                Text(
                                    "${card.def.cost}",
                                    color = HexfallColors.energyAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    card.def.name,
                                    color = if (card.def.upgraded) {
                                        HexfallColors.gold
                                    } else {
                                        HexfallColors.parchment
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                                Text(
                                    card.def.description,
                                    color = HexfallColors.parchment.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                )
                            }
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .background(cardColor(card.def.type), CircleShape),
                            )
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close", color = HexfallColors.gold)
                }
            }
        }
    }
}
