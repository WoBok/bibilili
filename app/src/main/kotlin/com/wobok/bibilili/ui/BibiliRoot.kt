package com.wobok.bibilili.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.wobok.bibilili.AppContainer
import com.wobok.bibilili.data.repo.asHttps
import com.wobok.bibilili.player.PlayerViewModel
import com.wobok.bibilili.ui.screen.AnimeTabs
import com.wobok.bibilili.ui.screen.ChannelScreen
import com.wobok.bibilili.ui.screen.FollowScreen
import com.wobok.bibilili.ui.screen.HistoryScreen
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
    const val HISTORY = "history"
    const val FOLLOW = "follow"
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
            Page {
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
                onGoHistory = { nav.navigate(Routes.HISTORY) },
                onGoFollow = { nav.navigate(Routes.FOLLOW) },
            )
        }

        composable(Routes.SEARCH) {
            Page { SearchScreen(container.catalogRepo) { nav.navigate(Routes.player(it)) } }
        }

        composable(Routes.HISTORY) {
            Page {
                HistoryScreen(
                    repo = container.historyRepo,
                    onBack = { nav.popBackStack() },
                    onOpenSeason = { nav.navigate(Routes.player(it)) },
                )
            }
        }

        composable(Routes.FOLLOW) {
            Page {
                FollowScreen(
                    repo = container.followRepo,
                    onBack = { nav.popBackStack() },
                    onOpenSeason = { nav.navigate(Routes.player(it)) },
                )
            }
        }

        composable(Routes.USER) {
            // 个人页往右一划退回主页，和它从右侧滑进来的方向对称
            Page(
                modifier = Modifier.pointerInput(Unit) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (total > SWIPE_THRESHOLD_PX) nav.popBackStack() },
                    ) { _, drag -> total += drag }
                }
            ) {
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

/**
 * 页面外壳。
 *
 * 背景必须画在**避让之前**——只给内容加 inset padding 的话，状态栏和手势条
 * 那两条带子露的是 Activity 的窗口底色，深色模式下就是上下两条白边。
 */
@Composable
private fun Page(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(PaperTheme.colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        content()
    }
}

@Composable
private fun PlayerRoute(seasonId: Long, onBack: () -> Unit) {
    val vm: PlayerViewModel = viewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(seasonId) {
        if (seasonId > 0) vm.load(seasonId = seasonId, epId = null)
    }

    // 切后台暂停；回前台时播放地址可能已经过期，交给 ViewModel 决定要不要重取
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> vm.onEnterBackground()
                Lifecycle.Event.ON_START -> vm.onEnterForeground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 全屏时锁横屏，退出时放回竖屏
    LockOrientation(landscape = state.fullscreen)

    BackHandler(enabled = state.fullscreen || state.showDetail || state.overlay != com.wobok.bibilili.player.PlayerOverlay.None) {
        when {
            state.overlay != com.wobok.bibilili.player.PlayerOverlay.None -> vm.closeOverlay()
            state.showDetail -> vm.setDetail(false)
            state.fullscreen -> vm.toggleFullscreen()
        }
    }

    PlayerScreen(state = state, player = vm.player, vm = vm, onExit = onBack)
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
    onGoHistory: () -> Unit,
    onGoFollow: () -> Unit,
) {
    val colors = PaperTheme.colors
    val pager = rememberPagerState(pageCount = { Tab.entries.size })
    val scope = rememberCoroutineScope()
    var avatar by remember { mutableStateOf("") }

    LaunchedEffect(loggedIn) {
        if (!loggedIn) return@LaunchedEffect
        avatar = runCatching { container.authApi.nav() }.getOrNull()?.payload?.face.orEmpty().asHttps()
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
                .padding(start = 22.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
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
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "搜索",
                tint = colors.onBg2,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .size(22.dp)
                    .clickable(onClick = onGoSearch),
            )
            AsyncImage(
                model = avatar,
                contentDescription = "我的",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(start = 14.dp, bottom = 4.dp)
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

        Box(Modifier.fillMaxSize()) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                when (Tab.entries[page]) {
                    Tab.Home -> HomeScreen(
                        historyRepo = container.historyRepo,
                        followRepo = container.followRepo,
                        loggedIn = loggedIn,
                        onOpenSeason = onOpenSeason,
                        onGoChannel = { scope.launch { pager.animateScrollToPage(Tab.Movie.ordinal) } },
                        onGoLogin = onGoLogin,
                        onGoHistory = onGoHistory,
                        onGoFollowList = onGoFollow,
                    )

                    Tab.Movie -> ChannelScreen(MovieTabs, container, onOpenSeason)
                    Tab.Anime -> ChannelScreen(AnimeTabs, container, onOpenSeason)
                }
            }

            /*
             * 首页右缘的一条窄带：从这里往左划进个人页。
             *
             * 三个主 tab 之间也是左右滑动切换的，所以「首页左滑进个人页」只能靠
             * 边缘手势区分——整页左滑已经被「去影视」占掉了。这条带只有 20dp 宽，
             * 而且只在首页存在，不会妨碍翻页。
             */
            if (pager.currentPage == Tab.Home.ordinal) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .width(20.dp)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            var total = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { total = 0f },
                                onDragEnd = { if (total < -EDGE_SWIPE_THRESHOLD_PX) onGoUser() },
                            ) { change, drag ->
                                change.consume()
                                total += drag
                            }
                        }
                )
            }
        }
    }
}

/** 整页横划的判定阈值。 */
private const val SWIPE_THRESHOLD_PX = 120f

/** 边缘窄带的判定阈值，比整页宽松一些，不然那 20dp 里划不出距离。 */
private const val EDGE_SWIPE_THRESHOLD_PX = 60f
