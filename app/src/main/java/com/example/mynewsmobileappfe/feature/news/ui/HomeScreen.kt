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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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
    onLoginRequired: () -> Unit = {},
    onArticleClick: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // 섹션 변경 시 기사 로드
    LaunchedEffect(section) {
        android.util.Log.d("HomeScreen", "LaunchedEffect triggered with section: $section")
        android.util.Log.d("HomeScreen", "ViewModel instance: ${viewModel.hashCode()}")
        section?.let { viewModel.selectSection(it) }
    }

    val articlesState by viewModel.articlesState.collectAsStateWithLifecycle()
    val randomArticle by viewModel.randomArticle.collectAsStateWithLifecycle()

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
                    // 랜덤 기사 (크게)
                    randomArticle?.let { random ->
                        val randomAsArticle = random.toArticleResponse()
                        item {
                            RandomArticleCard(
                                article = randomAsArticle,
                                onClick = {
                                    ArticleCache.putArticle(randomAsArticle)
                                    onArticleClick(random.articleId)
                                }
                            )
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
                                viewModel.toggleLike(displayArticle.articleId)
                            },
                            onDislikeClick = {
                                viewModel.toggleDislike(displayArticle.articleId)
                            },
                            onBookmarkClick = {
                                viewModel.toggleBookmark(displayArticle.articleId, displayArticle.bookmarked)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // 썸네일 이미지
            article.thumbnailUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "기사 이미지",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    contentScale = ContentScale.Crop
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = article.publisher,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(article.publishedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            // 썸네일 (왼쪽)
            article.thumbnailUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "기사 썸네일",
                    modifier = Modifier
                        .size(80.dp)
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
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                // 출처 및 날짜
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = article.publisher,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(article.publishedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 액션 버튼들 (좋아요, 싫어요, 북마크)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 좋아요
                    IconButton(
                        onClick = onLikeClick,
                        modifier = Modifier.size(32.dp),
                        enabled = enableActions
                    ) {
                        val isLiked = userReaction == ReactionType.LIKE
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "좋아요",
                                modifier = Modifier.size(16.dp),
                                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                        modifier = Modifier.size(32.dp),
                        enabled = enableActions
                    ) {
                        val isDisliked = userReaction == ReactionType.DISLIKE
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                contentDescription = "싫어요",
                                modifier = Modifier.size(16.dp),
                                tint = if (isDisliked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${article.dislikes}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDisliked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

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
 * 날짜 포맷팅 헬퍼 함수
 * ISO 8601 형식 → 간단한 날짜 표시
 */
fun formatDate(isoDate: String): String {
    return try {
        // "2025-12-11T15:10:20" → "2025.12.11"
        val parts = isoDate.split("T")[0].split("-")
        "${parts[0]}.${parts[1]}.${parts[2]}"
    } catch (e: Exception) {
        isoDate
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
