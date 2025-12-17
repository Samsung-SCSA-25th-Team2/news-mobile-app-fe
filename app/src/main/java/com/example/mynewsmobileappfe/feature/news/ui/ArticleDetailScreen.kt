package com.example.mynewsmobileappfe.feature.news.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import android.util.Log
import androidx.compose.material.icons.filled.BrokenImage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.mynewsmobileappfe.core.database.entity.Highlight
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import com.example.mynewsmobileappfe.feature.news.domain.model.ReactionType
import com.example.mynewsmobileappfe.feature.news.nfc.LinkHceService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    articleId: Long,
    isLoggedIn: Boolean = false,
    onNavigateBack: () -> Unit,
    onLoginRequired: () -> Unit = {},
    viewModel: ArticleDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val articleState by viewModel.articleState.collectAsStateWithLifecycle()
    val userReaction by viewModel.userReaction.collectAsStateWithLifecycle()
    val bookmarkEvent by viewModel.bookmarkEvent.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()

    // 편집 모드 상태
    var isEditMode by remember { mutableStateOf(false) }
    var selectedTextRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var selectedText by remember { mutableStateOf<String?>(null) }

    // 기사 로드
    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "형광펜 편집" else "기사 상세") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    // 편집/완료 버튼 (북마크된 기사일 때만 표시)
                    when (val state = articleState) {
                        is ArticleDetailState.Success -> {
                            if (state.article.bookmarked) {
                                IconButton(
                                    onClick = {
                                        isEditMode = !isEditMode
                                        if (!isEditMode) {
                                            // 편집 모드 종료 시 선택 초기화
                                            selectedTextRange = null
                                            selectedText = null
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isEditMode) Icons.Filled.Check else Icons.Filled.Edit,
                                        contentDescription = if (isEditMode) "완료" else "편집",
                                        tint = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        else -> {}
                    }

                    // 공유 버튼
                    if (!isEditMode) {
                        IconButton(
                            onClick = {
                                when (val state = articleState) {
                                    is ArticleDetailState.Success -> {
                                        // 여기서 기사 ID로 송신 모드 ON
                                        val articleIdToSend = state.article.articleId

                                        LinkHceService.startSending(articleIdToSend)

                                        Toast.makeText(
                                            context,
                                            "이 기사를 NFC로 보낼 준비가 되었어요.\n다른 폰을 태그하면 articleId=$articleIdToSend 전송!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    else -> {
                                        Toast.makeText(context, "기사를 불러오는 중입니다.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Nfc,
                                contentDescription = "기사 NFC 공유",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(url)
                                    .crossfade(true)
                                    .build(),
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(250.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                },
                                error = {
                                    Log.e("ArticleDetailScreen", "Failed to load image: $url, error: ${it.result.throwable?.message}")
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(250.dp),
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

                            // 출처, 기자, 날짜
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
                                    text = article.source,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = " • ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDateDetail(article.publishedAt),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(16.dp))
                            Divider()
                            Spacer(Modifier.height(16.dp))

                            // 본문 내용 (하이라이트 포함)
                            article.content?.let { content ->
                                // "\\n"을 줄바꿈으로 렌더링하되, 길이(인덱스) 보존을 위해 zero-width를 추가
                                val contentForRender = content.replace("\\n", "\n\u200B")

                                if (isEditMode) {
                                    // 안내 메시지
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "💡 형광펜을 칠할 문장을 클릭하세요",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(12.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    // 편집 모드: 문장 클릭 가능
                                    HighlightableText(
                                        content = contentForRender,
                                        highlights = highlights,
                                        onTextSelected = { start, end, text ->
                                            selectedTextRange = Pair(start, end)
                                            selectedText = text
                                        }
                                    )

                                    // 색상 선택 바 (문장 선택 시 표시)
                                    if (selectedText != null) {
                                        Spacer(Modifier.height(16.dp))
                                        ColorSelectionBar(
                                            onColorSelected = { color ->
                                                selectedTextRange?.let { (start, end) ->
                                                    selectedText?.let { text ->
                                                        viewModel.addHighlight(
                                                            articleId = articleId,
                                                            startIndex = start,
                                                            endIndex = end,
                                                            text = text,
                                                            color = color
                                                        )
                                                    }
                                                }
                                            },
                                            onDismiss = {
                                                selectedTextRange = null
                                                selectedText = null
                                            }
                                        )
                                    }
                                } else {
                                    // 보기 모드: 하이라이트만 표시
                                    HighlightedText(
                                        content = contentForRender,
                                        highlights = highlights
                                    )
                                }
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
                                isLoggedIn = isLoggedIn,
                                onReact = { newReaction ->
                                    viewModel.reactToArticle(articleId, newReaction)
                                },
                                onLoginRequired = onLoginRequired
                            )

                            Spacer(Modifier.height(32.dp))

                            // 북마크 버튼
                            BookmarkButtonRow(
                                article = article,
                                bookmarkEvent = bookmarkEvent,
                                isLoggedIn = isLoggedIn,
                                onToggle = { isBookmarked ->
                                    viewModel.toggleBookmark(articleId, isBookmarked)
                                },
                                onResetEvent = { viewModel.resetBookmarkEvent() },
                                onLoginRequired = onLoginRequired
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
    isLoggedIn: Boolean,
    onToggle: (Boolean) -> Unit,
    onResetEvent: () -> Unit,
    onLoginRequired: () -> Unit
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
                if (isLoggedIn) {
                    onToggle(isBookmarked)
                } else {
                    onLoginRequired()
                }
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
    isLoggedIn: Boolean,
    onReact: (ReactionType) -> Unit,
    onLoginRequired: () -> Unit
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
                    if (isLoggedIn) {
                        val next = if (userReaction == ReactionType.LIKE) ReactionType.NONE else ReactionType.LIKE
                        onReact(next)
                    } else {
                        onLoginRequired()
                    }
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
                    if (isLoggedIn) {
                        val next = if (userReaction == ReactionType.DISLIKE) ReactionType.NONE else ReactionType.DISLIKE
                        onReact(next)
                    } else {
                        onLoginRequired()
                    }
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

/**
 * 형광펜 색상 선택 바
 *
 * 사용자가 텍스트를 선택하면 나타나는 색상 팔레트입니다.
 */
@Composable
fun ColorSelectionBar(
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        "#FFFF00" to "노란색",
        "#90EE90" to "초록색",
        "#87CEEB" to "하늘색",
        "#FFB6C1" to "핑크색",
        "#FFA500" to "주황색"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "형광펜 색상 선택",
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "닫기",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                colors.forEach { (hex, name) ->
                    ColorButton(
                        colorHex = hex,
                        colorName = name,
                        onClick = {
                            onColorSelected(hex)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

/**
 * 색상 버튼
 */
@Composable
private fun ColorButton(
    colorHex: String,
    colorName: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(android.graphics.Color.parseColor(colorHex)))
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = colorName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 하이라이트 가능한 텍스트
 *
 * 문장 단위로 클릭하여 형광펜을 칠할 수 있습니다.
 * 이미 하이라이트된 부분은 색상으로 표시됩니다.
 */
@Composable
fun HighlightableText(
    content: String,
    highlights: List<Highlight>,
    onTextSelected: (startIndex: Int, endIndex: Int, text: String) -> Unit
) {
    // 문장 단위로 분리 (마침표, 느낌표, 물음표 기준)
    val sentences = content.splitToSequence(". ", "! ", "? ")
        .filter { it.isNotBlank() }
        .toList()

    var currentIndex = 0

    Column(modifier = Modifier.fillMaxWidth()) {
        sentences.forEach { sentence ->
            val sentenceWithPunctuation = sentence + when {
                content.substring(currentIndex).startsWith(sentence + ". ") -> ". "
                content.substring(currentIndex).startsWith(sentence + "! ") -> "! "
                content.substring(currentIndex).startsWith(sentence + "? ") -> "? "
                else -> ""
            }

            val startIndex = currentIndex
            val endIndex = currentIndex + sentenceWithPunctuation.length

            // 이 문장에 해당하는 하이라이트 찾기
            val sentenceHighlights = highlights.filter { highlight ->
                highlight.startIndex >= startIndex && highlight.endIndex <= endIndex
            }

            // AnnotatedString 생성
            val annotatedText = buildAnnotatedString {
                append(sentenceWithPunctuation)

                // 하이라이트 적용
                sentenceHighlights.forEach { highlight ->
                    val relativeStart = highlight.startIndex - startIndex
                    val relativeEnd = highlight.endIndex - startIndex

                    addStyle(
                        style = SpanStyle(
                            background = Color(android.graphics.Color.parseColor(highlight.color))
                        ),
                        start = relativeStart.coerceAtLeast(0),
                        end = relativeEnd.coerceAtMost(sentenceWithPunctuation.length)
                    )
                }
            }

            // 클릭 가능한 텍스트
            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onTextSelected(startIndex, endIndex, sentenceWithPunctuation.trim())
                    }
                    .padding(vertical = 4.dp)
            )

            currentIndex = endIndex
        }
    }
}

/**
 * 하이라이트된 텍스트 (보기 전용)
 *
 * 저장된 하이라이트를 표시합니다. 클릭 불가능.
 */
@Composable
fun HighlightedText(
    content: String,
    highlights: List<Highlight>
) {
    val annotatedText = buildAnnotatedString {
        append(content)

        // 모든 하이라이트 적용
        highlights.forEach { highlight ->
            addStyle(
                style = SpanStyle(
                    background = Color(android.graphics.Color.parseColor(highlight.color))
                ),
                start = highlight.startIndex.coerceAtLeast(0),
                end = highlight.endIndex.coerceAtMost(content.length)
            )
        }
    }

    Text(
        text = annotatedText,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * 날짜 포맷팅 헬퍼 함수 (ArticleDetailScreen용)
 * ISO 8601 형식 → "yyyy.MM.dd HH:mm"
 */
private fun formatDateDetail(isoDate: String): String {
    return try {
        val publishedAt = LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
        publishedAt.format(formatter)
    } catch (e: Exception) {
        // 파싱 실패 시 기본 포맷으로 fallback
        try {
            val parts = isoDate.split("T")
            val date = parts[0].split("-")
            val time = parts.getOrNull(1)?.substring(0, 5) ?: "00:00"
            "${date[0]}.${date[1]}.${date[2]} $time"
        } catch (e2: Exception) {
            isoDate
        }
    }
}
