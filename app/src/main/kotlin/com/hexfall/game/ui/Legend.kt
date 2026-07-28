package com.hexfall.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hexfall.core.CardDef
import com.hexfall.core.CardEffect
import com.hexfall.core.CombatEngine
import com.hexfall.core.EnemyCombatant
import com.hexfall.core.EnemyMove
import com.hexfall.core.MoonPhase
import com.hexfall.core.StatusType

fun moonEmoji(phase: MoonPhase): String = when (phase) {
    MoonPhase.NEW -> "🌑"
    MoonPhase.WAXING -> "🌓"
    MoonPhase.FULL -> "🌕"
    MoonPhase.WANING -> "🌗"
}

/** Every rules keyword a card touches, as (term, explanation) pairs. */
fun keywordEntries(def: CardDef): List<Pair<String, String>> {
    val entries = LinkedHashMap<String, String>()
    fun status(s: StatusType) {
        entries["${statusIcon(s)} ${s.displayName}"] = s.rulesText
    }
    def.effects.forEach { effect ->
        when (effect) {
            is CardEffect.ApplyToTarget -> status(effect.status)
            is CardEffect.ApplyToAll -> status(effect.status)
            is CardEffect.ApplyToSelf -> status(effect.status)
            is CardEffect.GainWard -> {
                entries["🛡 Ward"] =
                    "Absorbs incoming damage. Your Ward resets at the start of your turn."
                if (effect.fullMoonBonus > 0) {
                    entries["🌕 Full Moon"] = MoonPhase.FULL.rulesText
                }
            }
            is CardEffect.Damage -> if (effect.fullMoonBonus > 0) {
                entries["🌕 Full Moon"] = MoonPhase.FULL.rulesText
            }
            is CardEffect.DamageAll -> if (effect.fullMoonBonus > 0) {
                entries["🌕 Full Moon"] = MoonPhase.FULL.rulesText
            }
            is CardEffect.Draw -> if (effect.newMoonBonus > 0) {
                entries["🌑 New Moon"] = MoonPhase.NEW.rulesText
            }
            is CardEffect.AdvanceMoon -> {
                entries["☽ The Moon"] =
                    "The moon cycles New → Waxing → Full → Waning, turning once at the " +
                        "start of every turn. This spell turns it forward immediately."
            }
            else -> Unit
        }
    }
    if (def.exhaust) {
        entries["✦ Exhaust"] =
            "After casting, this card is removed from your deck for the rest of the combat."
    }
    return entries.toList()
}

@Composable
private fun LegendRow(term: String, text: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text(
            term,
            color = HexfallColors.gold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(110.dp),
        )
        Text(
            text,
            color = HexfallColors.parchment.copy(alpha = 0.9f),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Tap-a-card inspector: an enlarged card, every keyword it uses explained,
 * and cast buttons as a tap-to-play fallback alongside drag-and-drop.
 */
@Composable
fun CardInspectDialog(
    def: CardDef,
    canCast: Boolean,
    enemies: List<EnemyCombatant>,
    onCast: ((Int?) -> Unit)?,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF17102A),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Enlarged card.
                Column(
                    Modifier
                        .width(200.dp)
                        .border(2.dp, rarityBorder(def), RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF2B2048), Color(0xFF191230)),
                            ),
                            RoundedCornerShape(14.dp),
                        ),
                ) {
                    Box {
                        SigilArt(
                            seed = def.id.hashCode(),
                            tint = cardColor(def.type),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp),
                        )
                        if (def.playable) {
                            Box(
                                Modifier
                                    .padding(6.dp)
                                    .size(34.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color(0xFFFFE29A),
                                                HexfallColors.energyAmber,
                                            ),
                                        ),
                                        CircleShape,
                                    )
                                    .border(1.dp, Color(0xFF7A5A1E), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${def.cost}",
                                    color = Color(0xFF3A2A08),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Text(
                        def.name,
                        color = if (def.upgraded) HexfallColors.gold else HexfallColors.parchment,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardColor(def.type).copy(alpha = 0.45f))
                            .padding(vertical = 5.dp),
                    )
                    Text(
                        def.description,
                        color = HexfallColors.parchment,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                    )
                }

                val keywords = keywordEntries(def)
                if (keywords.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F0A1C), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                    ) {
                        keywords.forEach { (term, text) -> LegendRow(term, text) }
                    }
                }

                if (canCast && onCast != null) {
                    Spacer(Modifier.height(12.dp))
                    if (def.needsTarget) {
                        enemies.forEachIndexed { index, enemy ->
                            ArcaneButton(
                                text = "Cast at ${enemy.def.name}",
                                onClick = { onCast(index) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                            )
                        }
                    } else {
                        ArcaneButton(
                            text = "Cast",
                            onClick = { onCast(null) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("Close", color = HexfallColors.parchment.copy(alpha = 0.7f))
                }
            }
        }
    }
}

/** Tap-an-enemy inspector: figure, intent explained, statuses explained. */
@Composable
fun EnemyInspectDialog(
    engine: CombatEngine,
    enemy: EnemyCombatant,
    onDismiss: () -> Unit,
) {
    val intentDetail = when (val move = enemy.nextMove) {
        is EnemyMove.Attack -> {
            val dmg = engine.intentDamage(enemy)
            if (move.times > 1) "Intends to strike ${move.times} times for $dmg each."
            else "Intends to strike for $dmg."
        }
        is EnemyMove.Guard -> "Intends to raise ${move.ward} Ward."
        is EnemyMove.AttackGuard ->
            "Intends to strike for ${engine.intentDamage(enemy)} and raise ${move.ward} Ward."
        is EnemyMove.Buff ->
            "Intends to use ${move.label}: gains ${move.amount} ${move.status.displayName}. " +
                move.status.rulesText
        is EnemyMove.Debuff ->
            "Intends to afflict you with ${move.amount} ${move.status.displayName}. " +
                move.status.rulesText
        is EnemyMove.HealSelf -> "Intends to use ${move.label}: restores ${move.amount} HP."
        is EnemyMove.Steal ->
            "Intends to strike for ${engine.intentDamage(enemy)} and steal ${move.gold} gold."
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF17102A),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EnemyFigure(enemy.def.id, Modifier.size(110.dp))
                Text(
                    enemy.def.name,
                    color = HexfallColors.gold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "❤ ${enemy.hp}/${enemy.maxHp}" +
                        if (enemy.ward > 0) "   🛡 ${enemy.ward}" else "",
                    color = HexfallColors.parchment,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F0A1C), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                ) {
                    LegendRow("⚔ Intent", intentDetail)
                    enemy.statuses.forEach { (status, amount) ->
                        LegendRow(
                            "${statusIcon(status)} ${status.displayName} $amount",
                            status.rulesText,
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = HexfallColors.parchment.copy(alpha = 0.7f))
                }
            }
        }
    }
}

/** The full glossary: moon phases, every status, and combat basics. */
@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF17102A),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Grimoire of Terms",
                    color = HexfallColors.gold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    item {
                        Text(
                            "THE MOON",
                            color = Art.moonGlow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        LegendRow(
                            "☽ The Moon",
                            "Turns one phase forward at the start of every turn: " +
                                "New → Waxing → Full → Waning. Cards and monsters react to it.",
                        )
                        MoonPhase.entries.forEach { phase ->
                            LegendRow("${moonEmoji(phase)} ${phase.displayName}", phase.rulesText)
                        }
                    }
                    item {
                        Text(
                            "COMBAT BASICS",
                            color = Art.moonGlow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        LegendRow(
                            "◈ Mana",
                            "Spent to cast spells. Refills to 3 at the start of your turn.",
                        )
                        LegendRow(
                            "🛡 Ward",
                            "Absorbs incoming damage. Your Ward resets at the start of " +
                                "your turn; enemy Ward resets on theirs.",
                        )
                        LegendRow(
                            "✦ Exhaust",
                            "An exhausted card leaves your deck for the rest of the combat.",
                        )
                        LegendRow(
                            "⚔ Intent",
                            "The badge above each enemy shows what it will do on its " +
                                "next turn. Tap an enemy for details.",
                        )
                        LegendRow(
                            "🃏 Casting",
                            "Drag a card onto an enemy to strike it, or drag onto the " +
                                "battlefield to cast spells on yourself. Tap a card to inspect it.",
                        )
                    }
                    item {
                        Text(
                            "BLESSINGS & AFFLICTIONS",
                            color = Art.moonGlow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    StatusType.entries.forEach { status ->
                        item {
                            LegendRow(
                                "${statusIcon(status)} ${status.displayName}",
                                status.rulesText,
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
