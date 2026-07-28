package com.hexfall.game.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hexfall.game.GameScreen
import com.hexfall.game.GameViewModel
import com.hexfall.game.GameViewModelFactory
import com.hexfall.game.RunStore

@Composable
fun HexfallApp(
    store: RunStore? = null,
    vm: GameViewModel = viewModel(factory = GameViewModelFactory(store)),
) {
    HexfallTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            // Remembered above the key() below, so playing a card does not
            // scroll the hand back to the first card.
            val handScroll = rememberLazyListState()

            // The core engine mutates in place; re-key the whole tree on the
            // ViewModel's version counter so every action re-renders.
            key(vm.version) {
                when (val screen = vm.screen) {
                    is GameScreen.Title -> TitleScreen(vm)
                    is GameScreen.Map -> MapScreen(vm)
                    is GameScreen.Combat -> CombatScreen(vm, handScroll)
                    is GameScreen.Reward -> RewardScreen(vm, screen)
                    is GameScreen.Event -> EventScreen(vm, screen)
                    is GameScreen.Shop -> ShopScreen(vm, screen)
                    is GameScreen.Rest -> RestScreen(vm)
                    is GameScreen.Treasure -> TreasureScreen(vm, screen)
                    is GameScreen.GameOver -> GameOverScreen(vm, screen)
                }
            }
        }
    }
}
