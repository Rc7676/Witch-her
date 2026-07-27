package com.hexfall.game.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hexfall.core.CardLibrary
import com.hexfall.core.MoonPhase
import com.hexfall.game.GameScreen
import com.hexfall.game.GameViewModel

@Composable
fun TitleScreen(vm: GameViewModel) {
    val pulse by rememberInfiniteTransition(label = "moon")
        .animateFloat(
            initialValue = 0.9f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
            label = "moonGlow",
        )

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawNightSky(starSeed = 3)
            drawMoon(
                MoonPhase.FULL,
                Offset(size.width * 0.5f, size.height * 0.24f),
                size.minDimension * 0.16f,
                glowScale = pulse,
            )
            drawSpire(alpha = 1f)
        }
        Canvas(
            Modifier
                .size(120.dp)
                .align(Alignment.BottomStart)
                .padding(start = 24.dp),
        ) { drawWitch() }

        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1.2f))
            Text(
                "H E X F A L L",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = HexfallColors.gold,
            )
            Text(
                "The moon turns. The spire waits.",
                fontSize = 14.sp,
                color = Art.moonGlow.copy(alpha = 0.85f),
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { vm.newRun() },
                colors = ButtonDefaults.buttonColors(containerColor = HexfallColors.purple),
            ) {
                Text("Begin the Climb", fontSize = 18.sp, modifier = Modifier.padding(6.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Bind spells to your grimoire. Watch the moon.\nDeath is permanent.",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = HexfallColors.parchment.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun RewardScreen(vm: GameViewModel, screen: GameScreen.Reward) {
    val reward = screen.reward
    NightScene(starSeed = 21) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Victory!",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = HexfallColors.gold,
            )
            Spacer(Modifier.height(8.dp))
            Text("🪙 +${reward.gold} gold", fontSize = 16.sp, color = HexfallColors.gold)

            reward.relic?.let { relic ->
                Spacer(Modifier.height(14.dp))
                if (!vm.rewardRelicTaken) {
                    OutlinedButton(onClick = { vm.takeRewardRelic(relic) }) {
                        Text("Take charm: 🔮 ${relic.name}", color = HexfallColors.purple)
                    }
                    Text(
                        relic.description,
                        fontSize = 11.sp,
                        color = HexfallColors.parchment.copy(alpha = 0.7f),
                    )
                } else {
                    Text("🔮 ${relic.name} taken", color = HexfallColors.purple, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
            if (!vm.rewardCardTaken) {
                Text(
                    "Bind a spell to your grimoire:",
                    fontSize = 14.sp,
                    color = HexfallColors.parchment,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    reward.cardChoices.forEach { def ->
                        CardView(def = def, onClick = { vm.takeRewardCard(def) })
                    }
                }
            } else {
                Text("Spell bound.", fontSize = 14.sp, color = HexfallColors.parchment)
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = { vm.backToMap() },
                colors = ButtonDefaults.buttonColors(containerColor = HexfallColors.purple),
            ) {
                Text(if (vm.rewardCardTaken) "Continue" else "Skip spell & continue")
            }
        }
    }
}

@Composable
fun EventScreen(vm: GameViewModel, screen: GameScreen.Event) {
    val run = vm.run ?: return
    val event = screen.def
    NightScene(starSeed = event.id.hashCode()) {
        Column(Modifier.fillMaxSize()) {
            RunHeader(vm)
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(12.dp))
                Text("❓", fontSize = 44.sp)
                Text(
                    event.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = HexfallColors.gold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                ScenePanel(Modifier.fillMaxWidth()) {
                    Text(
                        event.text,
                        fontSize = 15.sp,
                        color = HexfallColors.parchment,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    )
                }
                Spacer(Modifier.height(24.dp))

                val outcome = vm.eventOutcome
                if (outcome == null) {
                    event.choices.forEachIndexed { index, choice ->
                        val enabled = choice.available(run)
                        Button(
                            onClick = { vm.chooseEventOption(event, index) },
                            enabled = enabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HexfallColors.surfaceLight,
                                contentColor = HexfallColors.parchment,
                            ),
                        ) {
                            Text(choice.label, fontSize = 14.sp)
                        }
                    }
                } else {
                    Text(
                        outcome,
                        fontSize = 15.sp,
                        color = HexfallColors.purple,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { vm.backToMap() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HexfallColors.purple,
                        ),
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}

@Composable
fun ShopScreen(vm: GameViewModel, screen: GameScreen.Shop) {
    val run = vm.run ?: return
    val inv = screen.inventory
    var removing by remember { mutableStateOf(false) }

    NightScene(starSeed = 31) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = 12.dp),
        ) {
            RunHeader(vm)
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(
                    "💰 The Peddler's Wares",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = HexfallColors.gold,
                )
                Spacer(Modifier.height(14.dp))

                Text("Spells", fontSize = 15.sp, color = HexfallColors.parchment)
                Spacer(Modifier.height(6.dp))
                inv.cards.forEachIndexed { index, item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .background(Color(0xCC241A3E), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${item.def.name} · ${item.def.cost} Mana",
                                color = cardColor(item.def.type),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                            Text(
                                item.def.description,
                                color = HexfallColors.parchment.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        if (item.sold) {
                            Text("SOLD", color = HexfallColors.curseGrey, fontSize = 12.sp)
                        } else {
                            TextButton(
                                onClick = { vm.shopBuyCard(inv, index) },
                                enabled = run.gold >= item.price,
                            ) {
                                Text(
                                    "🪙${item.price}",
                                    color = HexfallColors.gold,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("Charms", fontSize = 15.sp, color = HexfallColors.parchment)
                Spacer(Modifier.height(6.dp))
                inv.relics.forEachIndexed { index, item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .background(Color(0xCC241A3E), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "🔮 ${item.def.name}",
                                color = HexfallColors.purple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                            Text(
                                item.def.description,
                                color = HexfallColors.parchment.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        if (item.sold) {
                            Text("SOLD", color = HexfallColors.curseGrey, fontSize = 12.sp)
                        } else {
                            TextButton(
                                onClick = { vm.shopBuyRelic(inv, index) },
                                enabled = run.gold >= item.price,
                            ) {
                                Text(
                                    "🪙${item.price}",
                                    color = HexfallColors.gold,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { removing = true },
                    enabled = !inv.removalUsed && run.gold >= inv.removalPrice,
                ) {
                    Text(
                        if (inv.removalUsed) {
                            "Spell forgotten"
                        } else {
                            "Forget a spell · 🪙${inv.removalPrice}"
                        },
                        color = HexfallColors.parchment,
                    )
                }
            }

            Button(
                onClick = { vm.backToMap() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.buttonColors(containerColor = HexfallColors.purple),
            ) {
                Text("Leave shop")
            }
        }
    }

    if (removing) {
        DeckDialog(
            title = "Forget which spell?",
            cards = run.deck,
            onDismiss = { removing = false },
            onPick = { card ->
                vm.shopRemoveCard(inv, card)
                removing = false
            },
        )
    }
}

@Composable
fun RestScreen(vm: GameViewModel) {
    val run = vm.run ?: return
    var upgrading by remember { mutableStateOf(false) }

    NightScene(starSeed = 41) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("🔥", fontSize = 54.sp)
            Text(
                "Campfire",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = HexfallColors.gold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "The flames keep the dark at bay, for a moment.",
                fontSize = 14.sp,
                color = HexfallColors.parchment.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(30.dp))
            Button(
                onClick = { vm.restHeal() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = HexfallColors.surfaceLight),
            ) {
                Text(
                    "Rest — heal ${(run.maxHp * 3) / 10} HP",
                    color = HexfallColors.hpRed,
                    fontSize = 15.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { upgrading = true },
                enabled = run.deck.any { CardLibrary.upgradedVersion(it.def) != null },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = HexfallColors.surfaceLight),
            ) {
                Text("Study — upgrade a spell", color = HexfallColors.purple, fontSize = 15.sp)
            }
        }
    }

    if (upgrading) {
        DeckDialog(
            title = "Upgrade which spell?",
            cards = run.deck.filter { CardLibrary.upgradedVersion(it.def) != null },
            onDismiss = { upgrading = false },
            onPick = { card ->
                upgrading = false
                vm.restUpgrade(card)
            },
        )
    }
}

@Composable
fun TreasureScreen(vm: GameViewModel, screen: GameScreen.Treasure) {
    NightScene(starSeed = 51) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("🎁", fontSize = 54.sp)
            Text(
                "A Forgotten Cache",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = HexfallColors.gold,
            )
            Spacer(Modifier.height(16.dp))
            Text("🪙 ${screen.gold} gold", fontSize = 16.sp, color = HexfallColors.gold)
            screen.relic?.let { relic ->
                Spacer(Modifier.height(8.dp))
                Text("🔮 ${relic.name}", fontSize = 16.sp, color = HexfallColors.purple)
                Text(
                    relic.description,
                    fontSize = 12.sp,
                    color = HexfallColors.parchment.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(30.dp))
            Button(
                onClick = { vm.openTreasure(screen) },
                colors = ButtonDefaults.buttonColors(containerColor = HexfallColors.purple),
            ) {
                Text("Take everything")
            }
        }
    }
}

@Composable
fun GameOverScreen(vm: GameViewModel, screen: GameScreen.GameOver) {
    NightScene(
        moon = if (screen.victory) MoonPhase.FULL else MoonPhase.NEW,
        spire = true,
        starSeed = 61,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(if (screen.victory) "👑" else "💀", fontSize = 60.sp)
            Text(
                if (screen.victory) "The Spire Falls" else "The Climb Ends",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (screen.victory) HexfallColors.gold else HexfallColors.hpRed,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (screen.victory) {
                    "The Hollow Queen's crown clatters onto the empty throne. " +
                        "Above the spire, the moon finally sets."
                } else {
                    "Your bones join the countless others on floor ${screen.floors}."
                },
                fontSize = 15.sp,
                color = HexfallColors.parchment,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Floors climbed: ${screen.floors}",
                fontSize = 13.sp,
                color = HexfallColors.parchment.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { vm.abandonRun() },
                colors = ButtonDefaults.buttonColors(containerColor = HexfallColors.purple),
            ) {
                Text("Return to Title")
            }
        }
    }
}
