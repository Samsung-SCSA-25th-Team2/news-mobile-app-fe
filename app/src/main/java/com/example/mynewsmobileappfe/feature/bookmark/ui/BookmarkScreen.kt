package com.example.mynewsmobileappfe.feature.bookmark.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import com.example.mynewsmobileappfe.feature.news.ui.ArticleCache
import com.example.mynewsmobileappfe.feature.news.ui.ArticleItem

/**
 * ========================================
 * BookmarkScreen - 동료 개발자 구현 가이드
 * ========================================
 *
 * [변경 사항]
 * - 북마크는 Bottom Nav에서 제거됨
 * - Profile 화면에서만 접근 가능
 * - 뒤로가기 시 Profile로 복귀 (onNavigateBack)
 *
 * [화면 구조]
 * ```
 * Scaffold(
 *     topBar = {
 *         TopAppBar(
 *             title = { Text("북마크") },
 *             navigationIcon = {
 *                 IconButton(onClick = onNavigateBack) {
 *                     Icon(Icons.Default.ArrowBack, "뒤로가기")
 *                 }
 *             }
 *         )
 *     }
 * ) {
 *     when (bookmarksState) {
 *         is BookmarkState.Loading -> LoadingIndicator()
 *         is BookmarkState.Empty -> EmptyState()
 *         is BookmarkState.Success -> BookmarkList()
 *         is BookmarkState.Error -> ErrorState()
 *     }
 * }
 * ```
 *
 * [빈 상태 UI]
 * ```
 * Column(
 *     modifier = Modifier.fillMaxSize(),
 *     horizontalAlignment = Alignment.CenterHorizontally,
 *     verticalArrangement = Arrangement.Center
 * ) {
 *     Icon(Icons.Default.BookmarkBorder, ...)
 *     Text("북마크한 기사가 없습니다")
 *     Text("관심 있는 기사를 북마크해보세요", style = MaterialTheme.typography.bodySmall)
 * }
 * ```
 *
 * [스와이프하여 삭제]
 * ```
 * SwipeToDismiss(
 *     state = dismissState,
 *     background = { DeleteBackground() },
 *     dismissContent = { ArticleItem(article) },
 *     directions = setOf(DismissDirection.EndToStart)
 * )
 *
 * LaunchedEffect(dismissState.isDismissed(DismissDirection.EndToStart)) {
 *     if (dismissState.isDismissed(DismissDirection.EndToStart)) {
 *         viewModel.removeBookmark(article.articleId)
 *     }
 * }
 * ```
 *
 * [삭제 확인 다이얼로그 (선택)]
 * - 스와이프 후 Snackbar로 "삭제됨 - 실행 취소" 표시 가능
 *
 * [기사 클릭]
 * - 외부 브라우저로 기사 원문 열기
 */
@Composable
fun BookmarkScreen(
    onNavigateBack: () -> Unit = {},  // Profile로 뒤로가기
    onArticleClick: (ArticleResponse) -> Unit = {},
    viewModel: BookmarkViewModel = hiltViewModel()
) {
    val state by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (state) {
            is BookmarkState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is BookmarkState.Empty -> Text("북마크한 기사가 없습니다", modifier = Modifier.align(Alignment.Center))
            is BookmarkState.Error -> Text(
                (state as BookmarkState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center)
            )
            is BookmarkState.Success -> {
                LazyColumn {
                    items(bookmarks, key = { it.articleId }) { article ->
                        val bookmarkedArticle = article.copy(bookmarked = true)
                        ArticleItem(
                            article = bookmarkedArticle,
                            onArticleClick = {
                                ArticleCache.putArticle(bookmarkedArticle)
                                onArticleClick(bookmarkedArticle)
                            },
                            onLikeClick = {},
                            onDislikeClick = {},
                            onBookmarkClick = {},
                            enableActions = false
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            else -> {}
        }
    }
}
