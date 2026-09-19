package com.wobok.bibilili.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wobok.bibilili.AppContainer
import com.wobok.bibilili.player.PlayerViewModel
import com.wobok.bibilili.ui.screen.AnimeTabs
import com.wobok.bibilili.ui.screen.ChannelScreen
import com.wobok.bibilili.ui.screen.HomeScreen
import com.wobok.bibilili.ui.screen.LoginScreen
import com.wobok.bibilili.ui.screen.MovieTabs
import com.wobok.bibilili.ui.screen.PlayerScreen
import com.wobok.bibilili.ui.screen.SearchScreen
import com.wobok.bibilili.ui.screen.UserScreen
import com.wobok.bibilili.ui.theme.PaperTheme

private object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val SEARCH = "search"
    const val USER = "user"
    const val PLAYER = "player/{seasonId}"
    fun player(seasonId: Long) = "player/$seasonId"
}

@Composable
fun BibiliRoot(container: AppContainer, loggedIn: Boolean) {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = if (loggedIn) Routes.MAIN else Routes.LOGIN,
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(container.qrLogin) {
                nav.navigate(Routes.MAIN) { popUpTo(Routes.LOGIN) { inclusive = true } }
            }
        }

        composable(Routes.MAIN) {
            MainTabs(
                container = container,
                loggedIn = loggedIn,
                onOpenSeason = { nav.navigate(Routes.player(it)) },
                onGoSearch = { nav.navigate(Routes.SEARCH) },
                onGoUser = { nav.navigate(Routes.USER) },
                onGoLogin = { nav.navigate(Routes.LOGIN) },
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(container.catalogRepo) { nav.navigate(Routes.player(it)) }
        }

        composable(Routes.USER) {
            UserScreen(container) {
                nav.navigate(Routes.LOGIN) { popUpTo(Routes.MAIN) { inclusive = true } }
            }
        }

        composable(Routes.PLAYER) { entry ->
            val seasonId = entry.arguments?.getString("seasonId")?.toLongOrNull() ?: 0L
            PlayerRoute(seasonId) { nav.popBackStack() }
        }
    }
}

@Composable
private fun PlayerRoute(seasonId: Long, onBack: () -> Unit) {
    val vm: PlayerViewModel = viewModel()
    val state by vm.state.collectAsState()

    androidx.compose.runtime.LaunchedEffect(seasonId) {
        if (seasonId > 0) vm.load(seasonId = seasonId, epId = null)
    }

    PlayerScreen(
        state = state,
        player = vm.player,
        onBack = { if (state.fullscreen) vm.toggleFullscreen() else onBack() },
        onToggleControls = vm::toggleControls,
        onSeekBy = vm::seekBy,
        onBoost = vm::boost,
        onToggleFullscreen = vm::toggleFullscreen,
        onOverlay = vm::setOverlay,
        onSelectTab = vm::selectTab,
        onSelectEpisode = { vm.playEpisode(it.epId, it.cid) },
        onSelectQuality = vm::setQuality,
        onSelectSpeed = vm::setSpeed,
        onSelectSeekStep = vm::setSeekStep,
        onToggleDetail = vm::setDetail,
    )
}

private enum class Tab(val label: String) { Home("首页"), Movie("影视"), Anime("番剧") }

@Composable
private fun MainTabs(
    container: AppContainer,
    loggedIn: Boolean,
    onOpenSeason: (Long) -> Unit,
    onGoSearch: () -> Unit,
    onGoUser: () -> Unit,
    onGoLogin: () -> Unit,
) {
    var tab by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(Tab.Home) }
    val colors = PaperTheme.colors

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Tab.entries.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { tab = item },
                ) {
                    Text(
                        text = item.label,
                        style = if (item == tab) MaterialTheme.typography.headlineMedium
                        else MaterialTheme.typography.bodyLarge,
                        fontWeight = if (item == tab) FontWeight.Black else FontWeight.Normal,
                        color = if (item == tab) colors.onBg else colors.onBg2,
                    )
                    Box(
                        Modifier
                            .padding(top = 7.dp)
                            .width(20.dp)
                            .height(2.dp)
                            .background(if (item == tab) colors.primary else colors.bg)
                    )
                }
            }
            Box(Modifier.weight(1f))
            Text(
                text = "搜索",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onBg2,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .clickable(onClick = onGoSearch),
            )
            Box(
                Modifier
                    .padding(bottom = 4.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.outline)
                    .clickable(onClick = onGoUser)
            )
        }

        Box(
            Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.outline)
        )

        when (tab) {
            Tab.Home -> HomeScreen(
                historyRepo = container.historyRepo,
                followRepo = container.followRepo,
                loggedIn = loggedIn,
                onOpenSeason = onOpenSeason,
                onGoChannel = { tab = Tab.Movie },
                onGoLogin = onGoLogin,
            )

            Tab.Movie -> ChannelScreen(MovieTabs, container.catalogRepo, onOpenSeason)
            Tab.Anime -> ChannelScreen(AnimeTabs, container.catalogRepo, onOpenSeason)
        }
    }
}
