package com.example.mynewsmobileappfe.feature.news.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import android.util.Log
import androidx.compose.material.icons.filled.BrokenImage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import com.example.mynewsmobileappfe.feature.news.cache.ArticleCache
import com.example.mynewsmobileappfe.feature.news.cache.ReactionCache
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import com.example.mynewsmobileappfe.feature.news.domain.model.ReactionType
import com.example.mynewsmobileappfe.feature.news.domain.model.Section

/**
 * ========================================
 * HomeScreen - 동료 개발자 구현 가이드
 * ========================================
 *
 * [변경 사항]
 * - 이제 각 섹션(정치/경제/사회/기술)이 별도의 Bottom Navigation 탭
 * - 섹션은 파라미터로 전달받음 (section: Section?)
 * - 섹션 탭 UI는 제거됨
 *
 * [화면 구조]
 * ```
 * Scaffold(
 *     topBar = { TopAppBar("정치" or "경제" or ...) },
 *     bottomBar = { BottomNavigation (정치, 경제, 사회, 기술, 내 정보) }
 * ) {
 *     LazyColumn {
 *         // 랜덤 기사 배너 (선택)
 *         item { RandomArticleBanner() }
 *
 *         // 기사 목록
 *         items(articles) { article ->
 *             ArticleItem(article)
 *         }
 *
 *         // 로딩 인디케이터 (페이지네이션)
 *         if (isLoading) {
 *             item { CircularProgressIndicator() }
 *         }
 *     }
 * }
 * ```
 *
 * [기사 아이템 구성]
 * - Row 레이아웃
 * - 좌측: 썸네일 이미지 (AsyncImage from Coil)
 * - 우측: 제목, 출처, 날짜
 * - 하단: 좋아요/싫어요 버튼, 북마크 버튼
 *
 * [무한 스크롤 구현]
 * ```
 * val listState = rememberLazyListState()
 *
 * LaunchedEffect(listState) {
 *     snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
 *         .collect { lastIndex ->
 *             if (lastIndex == articles.size - 1) {
 *                 viewModel.loadMore()
 *             }
 *         }
 * }
 * ```
 *
 * [Pull-to-Refresh]
 * ```
 * val pullRefreshState = rememberPullRefreshState(
 *     refreshing = isRefreshing,
 *     onRefresh = { viewModel.refresh() }
 * )
 *
 * Box(Modifier.pullRefresh(pullRefreshState)) {
 *     LazyColumn { ... }
 *     PullRefreshIndicator(isRefreshing, pullRefreshState)
 * }
 * ```
 *
 * [기사 클릭 시]
 * - article.url을 Intent로 열어 외부 브라우저에서 기사 보기
 * - 또는 WebView로 앱 내에서 보기
 *
 * [날짜 포맷팅]
 * - publishedAt: "2025-12-11T15:10:20" (ISO 8601)
 * - 표시: "2025.12.11" 또는 "1시간 전" 등
 */
@Composable
fun HomeScreen(
    section: Section? = Section.POLITICS,
    isLoggedIn: Boolean = false,
    onLoginRequired: () -> Unit = {},
    onArticleClick: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val listState = rememberLazyListState()
    val articlesState by viewModel.articlesState.collectAsStateWithLifecycle()
    val randomArticle by viewModel.randomArticle.collectAsStateWithLifecycle()
    var shouldScrollToTop by remember { mutableStateOf(false) }

    // 섹션 변경 시 기사 로드
    LaunchedEffect(section) {
        Log.d("HomeScreen", "LaunchedEffect triggered with section: $section")
        Log.d("HomeScreen", "ViewModel instance: ${viewModel.hashCode()}")
        section?.let {
            viewModel.selectSection(it)
            shouldScrollToTop = true
        }
    }

    // 섹션 변경 시 스크롤 최상단 이동 (데이터 로드 후)
    LaunchedEffect(shouldScrollToTop, articlesState) {
        if (shouldScrollToTop && articlesState is HomeState.Success) {
            Log.d("HomeScreen", "Scrolling to top for section: $section")
            // 약간의 delay로 렌더링 완료 보장
            delay(700)
            listState.scrollToItem(0, 0)
            shouldScrollToTop = false
        }
    }

    // 하이라이트 기사 자동 로테이션 (7초마다)
    LaunchedEffect(section) {
        section?.let { currentSection ->
            while (true) {
                Log.d("HomeScreen", "Auto-rotating highlight for section: $currentSection")
                viewModel.loadRandomArticle(currentSection)
                delay(7000) // 7초 대기
            }
        }
    }

    // 무한 스크롤: 마지막 아이템 근처 도달 시 다음 페이지 로드
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem != null && lastVisibleItem.index >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && articlesState is HomeState.Success) {
            viewModel.loadMore()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (articlesState) {
            is HomeState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is HomeState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "오류가 발생했습니다",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = (articlesState as HomeState.Error).message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            is HomeState.Success -> {
                val data = (articlesState as HomeState.Success).data

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // 랜덤 기사 (크게) - Crossfade 애니메이션
                    item(key = "random_article_container") {
                        Crossfade(
                            targetState = randomArticle,
                            animationSpec = tween(durationMillis = 500),
                            label = "random_article_crossfade"
                        ) { random ->
                            random?.let {
                                val randomAsArticle = it.toArticleResponse()
                                RandomArticleCard(
                                    article = randomAsArticle,
                                    onClick = {
                                        ArticleCache.putArticle(randomAsArticle)
                                        onArticleClick(it.articleId)
                                    }
                                )
                            }
                        }
                    }

                    // 기사 목록
                    items(
                        items = data.content,
                        key = { it.articleId }
                    ) { article ->
                        val displayArticle = ArticleCache.getArticle(article.articleId) ?: article
                        val userReaction = ReactionCache.getReaction(displayArticle.articleId)

                        ArticleItem(
                            article = displayArticle,
                            onArticleClick = {
                                // ArticleCache에 저장 후 상세 화면으로 이동
                                ArticleCache.putArticle(displayArticle)
                                onArticleClick(displayArticle.articleId)
                            },
                            onLikeClick = {
                                if (isLoggedIn) {
                                    viewModel.toggleLike(displayArticle.articleId)
                                } else {
                                    onLoginRequired()
                                }
                            },
                            onDislikeClick = {
                                if (isLoggedIn) {
                                    viewModel.toggleDislike(displayArticle.articleId)
                                } else {
                                    onLoginRequired()
                                }
                            },
                            onBookmarkClick = {
                                if (isLoggedIn) {
                                    viewModel.toggleBookmark(displayArticle.articleId, displayArticle.bookmarked)
                                } else {
                                    onLoginRequired()
                                }
                            },
                            enableActions = true,
                            userReaction = userReaction
                        )
                    }

                    // 로딩 인디케이터 (페이지네이션)
                    if (data.content.isNotEmpty() && !data.last) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }

            else -> {}
        }
    }
}

/**
 * 랜덤 기사 카드 (크게 표시)
 */
@Composable
fun RandomArticleCard(
    article: ArticleResponse,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp)
        ) {
        Column {
            // 썸네일 이미지
            article.thumbnailUrl?.let { url ->
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    },
                    error = {
                        Log.e("HomeScreen", "Failed to load image: $url, error: ${it.result.throwable?.message}")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BrokenImage,
                                contentDescription = "이미지 로드 실패",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    contentDescription = "기사 이미지",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center
                )
            }

            // 내용
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 배지 (TODAY'S PICK)
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "TODAY'S PICK",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 제목
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                // 출처 및 날짜
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(article.publishedAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = article.publisher,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 기사 아이템 컴포넌트 (목록)
 */
@Composable
fun ArticleItem(
    article: ArticleResponse,
    onArticleClick: () -> Unit,
    onLikeClick: () -> Unit,
    onDislikeClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    enableActions: Boolean = true,
    userReaction: ReactionType = ReactionType.NONE  // 사용자의 현재 반응 상태
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onArticleClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp)       // 마우스 오버 시
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            // 썸네일 (왼쪽)
            article.thumbnailUrl?.let { url ->
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    loading = {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    },
                    error = {
                        Log.e("HomeScreen", "Failed to load thumbnail: $url, error: ${it.result.throwable?.message}")
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BrokenImage,
                                contentDescription = "이미지 로드 실패",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    contentDescription = "기사 썸네일",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }

            // 내용 (오른쪽)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 제목
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(16.dp))   //이거 고치면 사진 작아짐

                // 출처
                Text(
                    text = article.publisher,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(16.dp))

                // 액션 버튼들 (좋아요, 싫어요, 북마크)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 좋아요
                    IconButton(
                        onClick = onLikeClick,
                        modifier = Modifier.size(36.dp), //사이즈가 충분히 커야 숫자,아이콘 둘다 보임
                        enabled = enableActions
                    ) {
                        val isLiked = userReaction == ReactionType.LIKE
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "좋아요",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${article.likes}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 싫어요
                    IconButton(
                        onClick = onDislikeClick,
                        modifier = Modifier.size(36.dp),
                        enabled = enableActions
                    ) {
                        val isDisliked = userReaction == ReactionType.DISLIKE
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                contentDescription = "싫어요",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${article.dislikes}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDisliked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    //날짜
                    Text(
                        text = formatDate(article.publishedAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 북마크
                    IconButton(
                        onClick = onBookmarkClick,
                        modifier = Modifier.size(32.dp),
                        enabled = enableActions
                    ) {
                        val isBookmarked = article.bookmarked
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "북마크",
                            modifier = Modifier.size(20.dp),
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }


            }
        }
    }
}

/**
 * 날짜 포맷팅 헬퍼 함수 (HomeScreen용)
 * - 24시간 이내: "N시간 전"
 * - 24시간 이후: "yyyy.MM.dd"
 */
fun formatDate(isoDate: String): String {
    return try {
        val publishedAt = LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val now = LocalDateTime.now()
        val hoursDiff = ChronoUnit.HOURS.between(publishedAt, now)

        when {
            hoursDiff < 1 -> "방금 전"
            hoursDiff < 24 -> "${hoursDiff}시간 전"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
                publishedAt.format(formatter)
            }
        }
    } catch (e: Exception) {
        // 파싱 실패 시 기본 포맷으로 fallback
        try {
            val parts = isoDate.split("T")[0].split("-")
            "${parts[0]}.${parts[1]}.${parts[2]}"
        } catch (e2: Exception) {
            isoDate
        }
    }
}

/**
 * 랜덤 기사 응답을 목록/상세에서 재사용 가능한 ArticleResponse로 변환
 */
private fun com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleRandomResponse.toArticleResponse(): ArticleResponse {
    return ArticleResponse(
        articleId = articleId,
        section = section,
        title = title,
        content = content,
        url = url,
        thumbnailUrl = thumbnailUrl,
        source = source,
        publisher = publisher,
        publishedAt = publishedAt,
        likes = 0,
        dislikes = 0,
        bookmarked = bookmarked,
        userReaction = userReaction
    )
}
