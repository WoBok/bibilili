package com.wobok.bibilili.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
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
import kotlinx.coroutines.launch

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
            // 登录页自己避让系统栏
            Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                LoginScreen(container.qrLogin) {
                    nav.navigate(Routes.MAIN) { popUpTo(Routes.LOGIN) { inclusive = true } }
                }
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
            Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                SearchScreen(container.catalogRepo) { nav.navigate(Routes.player(it)) }
            }
        }

        composable(Routes.USER) {
            Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                UserScreen(container) {
                    nav.navigate(Routes.LOGIN) { popUpTo(Routes.MAIN) { inclusive = true } }
                }
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

    LaunchedEffect(seasonId) {
        if (seasonId > 0) vm.load(seasonId = seasonId, epId = null)
    }

    // 全屏时锁横屏，退出时放回竖屏
    LockOrientation(landscape = state.fullscreen)

    BackHandler(enabled = state.fullscreen || state.showDetail) {
        when {
            state.showDetail -> vm.setDetail(false)
            state.fullscreen -> vm.toggleFullscreen()
        }
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
    val colors = PaperTheme.colors
    val pager = rememberPagerState(pageCount = { Tab.entries.size })
    val scope = rememberCoroutineScope()
    var avatar by remember { mutableStateOf("") }

    LaunchedEffect(loggedIn) {
        if (!loggedIn) return@LaunchedEffect
        avatar = runCatching { container.authApi.nav() }.getOrNull()?.payload?.face.orEmpty()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            // 顶栏整体避开状态栏与灵动岛
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Tab.entries.forEachIndexed { index, item ->
                val selected = index == pager.currentPage
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        scope.launch { pager.animateScrollToPage(index) }
                    },
                ) {
                    Text(
                        text = item.label,
                        style = if (selected) MaterialTheme.typography.headlineMedium
                        else MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.Black else FontWeight.Normal,
                        color = if (selected) colors.onBg else colors.onBg2,
                    )
                    Box(
                        Modifier
                            .padding(top = 7.dp)
                            .width(20.dp)
                            .height(2.dp)
                            .background(if (selected) colors.primary else colors.bg)
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
            AsyncImage(
                model = avatar,
                contentDescription = "我的",
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.outline)
                    .clickable(onClick = onGoUser),
            )
        }

        Box(
            Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.outline)
        )

        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxSize()
                // 首页整页左滑进用户页
                .pointerInput(pager.currentPage) {
                    if (pager.currentPage != Tab.Home.ordinal) return@pointerInput
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (total < -SWIPE_THRESHOLD_PX) onGoUser() },
                    ) { _, drag -> total += drag }
                },
        ) { page ->
            when (Tab.entries[page]) {
                Tab.Home -> HomeScreen(
                    historyRepo = container.historyRepo,
                    followRepo = container.followRepo,
                    loggedIn = loggedIn,
                    onOpenSeason = onOpenSeason,
                    onGoChannel = { scope.launch { pager.animateScrollToPage(Tab.Movie.ordinal) } },
                    onGoLogin = onGoLogin,
                    onGoFollowList = { scope.launch { pager.animateScrollToPage(Tab.Anime.ordinal) } },
                )

                Tab.Movie -> ChannelScreen(MovieTabs, container, onOpenSeason)
                Tab.Anime -> ChannelScreen(AnimeTabs, container, onOpenSeason)
            }
        }
    }
}

/** 左滑进用户页的判定阈值。 */
private const val SWIPE_THRESHOLD_PX = 160f
