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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hexfall.core.CardDef
import com.hexfall.core.CardInstance
import com.hexfall.core.CardType
import com.hexfall.core.Rarity
import com.hexfall.core.StatusType

fun cardColor(type: CardType): Color = when (type) {
    CardType.SPELL_ATTACK -> Color(0xFFC4574A)
    CardType.SPELL_WARD -> Color(0xFF4A7EC4)
    CardType.RITE -> Color(0xFF9B5FD0)
    CardType.CURSE -> Color(0xFF4A4A55)
}

fun rarityBorder(def: CardDef): Color = when {
    def.upgraded -> HexfallColors.gold
    def.rarity == Rarity.RARE -> Color(0xFFE0B85C)
    def.rarity == Rarity.UNCOMMON -> Color(0xFF6FC9B8)
    def.rarity == Rarity.CURSE -> Color(0xFF3A3A44)
    else -> Color(0xFF6C6486)
}

fun statusIcon(type: StatusType): String = when (type) {
    StatusType.SPELLPOWER -> "☀"
    StatusType.BULWARK -> "🛡"
    StatusType.HEXED -> "🕸"
    StatusType.CHILL -> "❄"
    StatusType.VENOM -> "🐍"
    StatusType.DOOM -> "💀"
    StatusType.REGROWTH -> "🌿"
    StatusType.BRAMBLES -> "🌵"
    StatusType.FRENZY -> "💢"
    StatusType.ATTUNED -> "🔮"
    StatusType.OMEN -> "🐦"
    StatusType.BLOOD_DEBT -> "🩸"
    StatusType.EMBERHEART -> "🔥"
}

/**
 * A hand/deck card: sigil art panel, cost gem, name banner, rules text.
 * [enabled] dims unplayable cards; [selected] adds a gold glow.
 */
@Composable
fun CardView(
    def: CardDef,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val borderColor = if (selected) HexfallColors.gold else rarityBorder(def)
    var m = modifier
        .width(108.dp)
        .height(160.dp)
        .border(if (selected) 3.dp else 1.5.dp, borderColor, RoundedCornerShape(12.dp))
        .clip(RoundedCornerShape(12.dp))
        .background(
            Brush.verticalGradient(
                listOf(Color(0xFF261C40), Color(0xFF17102A)),
            ),
        )
    if (onClick != null) m = m.clickable { onClick() }

    Column(m) {
        Box {
            SigilArt(
                seed = def.id.hashCode(),
                tint = if (enabled) cardColor(def.type) else Color(0xFF55506A),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            )
            if (def.playable) {
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(24.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFFFE29A), HexfallColors.energyAmber),
                            ),
                            CircleShape,
                        )
                        .border(1.dp, Color(0xFF7A5A1E), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${def.cost}",
                        color = Color(0xFF3A2A08),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Text(
            def.name,
            color = if (def.upgraded) HexfallColors.gold else HexfallColors.parchment,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 12.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor(def.type).copy(alpha = if (enabled) 0.45f else 0.2f))
                .padding(vertical = 3.dp, horizontal = 2.dp),
        )
        Text(
            def.description,
            color = HexfallColors.parchment.copy(alpha = if (enabled) 0.92f else 0.5f),
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 4.dp),
        )
    }
}

/** Simple labeled bar (HP etc.). */
@Composable
fun StatBar(current: Int, max: Int, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(14.dp)
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(7.dp)),
    ) {
        val fraction = if (max <= 0) 0f else (current.toFloat() / max).coerceIn(0f, 1f)
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(
                    Brush.horizontalGradient(listOf(color.copy(alpha = 0.75f), color)),
                    RoundedCornerShape(7.dp),
                ),
        )
        Text(
            "$current/$max",
            color = Color.White,
            fontSize = 9.sp,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** Translucent panel used over the night sky. */
@Composable
fun ScenePanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC17102A))
            .border(1.dp, Color(0x336C57A8), RoundedCornerShape(12.dp)),
    ) {
        content()
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
