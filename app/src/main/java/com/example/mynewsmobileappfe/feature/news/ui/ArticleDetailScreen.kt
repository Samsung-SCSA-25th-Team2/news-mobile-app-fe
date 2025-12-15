package com.example.mynewsmobileappfe.feature.news.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import com.example.mynewsmobileappfe.feature.news.domain.model.ReactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    articleId: Long,
    onNavigateBack: () -> Unit,
    onLoginRequired: () -> Unit = {},
    viewModel: ArticleDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val articleState by viewModel.articleState.collectAsStateWithLifecycle()
    val userReaction by viewModel.userReaction.collectAsStateWithLifecycle()
    val bookmarkEvent by viewModel.bookmarkEvent.collectAsStateWithLifecycle()

    // 기사 로드
    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("기사 상세") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    // 공유 버튼 (NFC 아이콘 사용)
                    IconButton(
                        onClick = {
                            when (val state = articleState) {
                                is ArticleDetailState.Success -> {
                                    // 공유 Intent 생성
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, state.article.title)
                                        putExtra(Intent.EXTRA_TEXT, "${state.article.title}\n\n${state.article.url}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "기사 공유하기"))
                                }
                                else -> {
                                    Toast.makeText(context, "기사를 불러오는 중입니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Nfc,
                            contentDescription = "기사 공유",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = articleState) {
                is ArticleDetailState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is ArticleDetailState.Error -> {
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
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                is ArticleDetailState.Success -> {
                    val article = state.article
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 썸네일 이미지
                        article.thumbnailUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "기사 이미지",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // 제목
                            Text(
                                text = article.title,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            // 출처 및 날짜
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = article.publisher,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = " • ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDate(article.publishedAt),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(16.dp))
                            Divider()
                            Spacer(Modifier.height(16.dp))

                            // 본문 내용
                            article.content?.let { content ->
                                Text(
                                    text = content,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(16.dp))
                            }

                            // 원문 링크
                            Text(
                                text = "원문 보기",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = article.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(24.dp))
                            Divider()
                            Spacer(Modifier.height(16.dp))

                            ReactionButtons(
                                article = article,
                                userReaction = userReaction,
                                onReact = { newReaction ->
                                    viewModel.reactToArticle(articleId, newReaction)
                                }
                            )

                            Spacer(Modifier.height(32.dp))

                            // 북마크 버튼
                            BookmarkButtonRow(
                                article = article,
                                bookmarkEvent = bookmarkEvent,
                                onToggle = { isBookmarked ->
                                    viewModel.toggleBookmark(articleId, isBookmarked)
                                },
                                onResetEvent = { viewModel.resetBookmarkEvent() }
                            )

                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun BookmarkButtonRow(
    article: ArticleResponse,
    bookmarkEvent: BookmarkEvent,
    onToggle: (Boolean) -> Unit,
    onResetEvent: () -> Unit
) {
    var isBookmarked by remember(article.articleId, article.bookmarked) { mutableStateOf(article.bookmarked) }

    // 이벤트 반영
    LaunchedEffect(bookmarkEvent) {
        when (bookmarkEvent) {
            is BookmarkEvent.Success -> {
                isBookmarked = bookmarkEvent.isBookmarked
                onResetEvent()
            }
            is BookmarkEvent.Error -> {
                onResetEvent()
            }
            else -> {}
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledIconButton(
            onClick = {
                onToggle(isBookmarked)
            },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isBookmarked)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(
                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = "북마크",
                tint = if (isBookmarked)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = if (isBookmarked) "북마크됨" else "북마크",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@Composable
private fun ReactionButtons(
    article: ArticleResponse,
    userReaction: ReactionType,
    onReact: (ReactionType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 좋아요 버튼
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilledIconButton(
                onClick = {
                    val next = if (userReaction == ReactionType.LIKE) ReactionType.NONE else ReactionType.LIKE
                    onReact(next)
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (userReaction == ReactionType.LIKE)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = if (userReaction == ReactionType.LIKE)
                        Icons.Filled.ThumbUp
                    else
                        Icons.Outlined.ThumbUp,
                    contentDescription = "좋아요",
                    tint = if (userReaction == ReactionType.LIKE)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${article.likes}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "좋아요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 싫어요 버튼
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilledIconButton(
                onClick = {
                    val next = if (userReaction == ReactionType.DISLIKE) ReactionType.NONE else ReactionType.DISLIKE
                    onReact(next)
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (userReaction == ReactionType.DISLIKE)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = if (userReaction == ReactionType.DISLIKE)
                        Icons.Filled.ThumbDown
                    else
                        Icons.Outlined.ThumbDown,
                    contentDescription = "싫어요",
                    tint = if (userReaction == ReactionType.DISLIKE)
                        MaterialTheme.colorScheme.onError
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${article.dislikes}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "싫어요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
