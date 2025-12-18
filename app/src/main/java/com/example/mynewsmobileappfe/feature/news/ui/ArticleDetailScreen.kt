package com.example.mynewsmobileappfe.feature.news.ui

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Nfc
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.mynewsmobileappfe.MainActivity
import com.example.mynewsmobileappfe.core.database.entity.Highlight
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import com.example.mynewsmobileappfe.feature.news.domain.model.ReactionType
import com.example.mynewsmobileappfe.feature.news.nfc.HceServiceManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


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
    val uriHandler = LocalUriHandler.current

    val appContext = context.applicationContext
    val mainActivity = context as? MainActivity

    // ✅ NFC 송신 종료 + ReaderMode 복원
    fun stopSendingAndRestoreReader() {
        HceServiceManager.disableSending(appContext)
        mainActivity?.enableForegroundReaderMode()
    }

    // ✅ ReaderMode 끄고 NFC 송신 시작 (HCE 충돌 방지)
    fun startSendingAndStopReader(articleIdToSend: Long) {
        mainActivity?.disableForegroundReaderMode()
        HceServiceManager.enableSending(appContext, articleIdToSend)
    }

    // ✅ 뒤로 가기 시 NFC 송신 종료 + ReaderMode 복원
    BackHandler {
        stopSendingAndRestoreReader()
        onNavigateBack()
    }

    // ✅ 화면 종료 시 NFC 송신 종료 + ReaderMode 복원
    DisposableEffect(Unit) {
        onDispose {
            stopSendingAndRestoreReader()
        }
    }

    // 편집 모드 상태
    var isEditMode by remember { mutableStateOf(false) }

    // 기사 로드
    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "형광펜 편집" else "기사 상세") },
                actions = {
                    // ✅ 편집/완료 버튼 (북마크된 기사일 때만 표시)
                    when (val state = articleState) {
                        is ArticleDetailState.Success -> {

                            val bookmarkEventState = bookmarkEvent

                            val isBookmarkedForUi =
                                when (bookmarkEventState) {
                                    is BookmarkEvent.Success -> bookmarkEventState.isBookmarked
                                    else -> state.article.bookmarked
                                }

                            if (isBookmarkedForUi) {
                                IconButton(
                                    onClick = { isEditMode = !isEditMode }
                                ) {
                                    Icon(
                                        imageVector = if (isEditMode) Icons.Filled.Check else Icons.Filled.Edit,
                                        contentDescription = if (isEditMode) "완료" else "편집",
                                    )
                                }
                            }
                        }
                        else -> {}
                    }

                    // 공유 버튼 (토글 방식)
                    if (!isEditMode) {
                        IconButton(
                            onClick = {
                                when (val state = articleState) {
                                    is ArticleDetailState.Success -> {
                                        val articleIdToSend = state.article.articleId

                                        // ✅ 토글 방식: 이미 송신 중이면 끄고, 아니면 켜기
                                        if (HceServiceManager.isSending()) {
                                            stopSendingAndRestoreReader()
                                            Toast.makeText(context, "NFC 송신 모드 OFF", Toast.LENGTH_SHORT).show()
                                        } else {
                                            startSendingAndStopReader(articleIdToSend)
                                            Toast.makeText(
                                                context,
                                                "NFC 송신 모드 ON\n다른 폰을 태그하면 articleId=$articleIdToSend 전송!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
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
                                tint = if (HceServiceManager.isSending())
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary
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
                                    Log.e(
                                        "ArticleDetailScreen",
                                        "Failed to load image: $url, error: ${it.result.throwable?.message}"
                                    )
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
                                val rawContent = content.replace("\\n", "\n")

                                val contentForRender = if (rawContent.contains("\n")) {
                                    rawContent.replace("\n", "\n\u200B")
                                } else {
                                    rawContent
                                        .replace(". ", ".\n\n\u200B")
                                        .replace("! ", "!\n\n\u200B")
                                        .replace("? ", "?\n\n\u200B")
                                }

                                if (isEditMode) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "💡 글자를 길게 누른 뒤 드래그해서 범위를 선택하세요.\n위에 뜨는 팔레트에서 색을 선택하면 저장됩니다.\n(이미 칠해진 영역을 탭하면 삭제할 수 있어요.)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(12.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    HighlightDragText(
                                        content = contentForRender,
                                        highlights = highlights,
                                        enabled = isEditMode,
                                        onAddHighlight = { start, endExclusive, colorHex ->
                                            val safeStart = start.coerceIn(0, contentForRender.length)
                                            val safeEnd = endExclusive.coerceIn(0, contentForRender.length)
                                            if (safeEnd <= safeStart) return@HighlightDragText

                                            val text = contentForRender.substring(safeStart, safeEnd)
                                            viewModel.addHighlight(
                                                articleId = articleId,
                                                startIndex = safeStart,
                                                endIndex = safeEnd,
                                                text = text,
                                                color = colorHex
                                            )
                                        },
                                        onDeleteHighlight = { highlightIdToDelete ->
                                            viewModel.deleteHighlight(highlightIdToDelete)
                                        }
                                    )

                                } else {
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { uriHandler.openUri(article.url) },
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = article.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { uriHandler.openUri(article.url) },
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(24.dp))
                            Divider()
                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Top
                            ) {
                                ReactionLikeColumn(
                                    article = article,
                                    userReaction = userReaction,
                                    isLoggedIn = isLoggedIn,
                                    onReact = { viewModel.reactToArticle(articleId, it) },
                                    onLoginRequired = onLoginRequired
                                )

                                ReactionDislikeColumn(
                                    article = article,
                                    userReaction = userReaction,
                                    isLoggedIn = isLoggedIn,
                                    onReact = { viewModel.reactToArticle(articleId, it) },
                                    onLoginRequired = onLoginRequired
                                )

                                BookmarkButtonColumn(
                                    article = article,
                                    bookmarkEvent = bookmarkEvent,
                                    isLoggedIn = isLoggedIn,
                                    onToggle = { viewModel.toggleBookmark(articleId, it) },
                                    onResetEvent = { viewModel.resetBookmarkEvent() },
                                    onLoginRequired = onLoginRequired
                                )
                            }

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
private fun BookmarkButtonColumn(
    article: ArticleResponse,
    bookmarkEvent: BookmarkEvent,
    isLoggedIn: Boolean,
    onToggle: (Boolean) -> Unit,
    onResetEvent: () -> Unit,
    onLoginRequired: () -> Unit
) {
    var isBookmarked by remember(article.articleId, article.bookmarked) {
        mutableStateOf(article.bookmarked)
    }

    LaunchedEffect(bookmarkEvent) {
        when (bookmarkEvent) {
            is BookmarkEvent.Success -> {
                isBookmarked = bookmarkEvent.isBookmarked
                onResetEvent()
            }
            is BookmarkEvent.Error -> onResetEvent()
            else -> {}
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = {
                if (isLoggedIn) onToggle(isBookmarked)
                else onLoginRequired()
            },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor =
                    if (isBookmarked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(
                imageVector =
                    if (isBookmarked) Icons.Filled.Bookmark
                    else Icons.Filled.BookmarkBorder,
                contentDescription = "북마크",
                tint =
                    if (isBookmarked) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = if (isBookmarked) "북마크됨" else "북마크",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReactionLikeColumn(
    article: ArticleResponse,
    userReaction: ReactionType,
    isLoggedIn: Boolean,
    onReact: (ReactionType) -> Unit,
    onLoginRequired: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = {
                    if (isLoggedIn) {
                        val next =
                            if (userReaction == ReactionType.LIKE) ReactionType.NONE
                            else ReactionType.LIKE
                        onReact(next)
                    } else onLoginRequired()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor =
                        if (userReaction == ReactionType.LIKE)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector =
                        if (userReaction == ReactionType.LIKE)
                            Icons.Filled.ThumbUp
                        else
                            Icons.Outlined.ThumbUp,
                    contentDescription = "좋아요",
                    tint =
                        if (userReaction == ReactionType.LIKE)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(text = "${article.likes}")
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = "좋아요",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ReactionDislikeColumn(
    article: ArticleResponse,
    userReaction: ReactionType,
    isLoggedIn: Boolean,
    onReact: (ReactionType) -> Unit,
    onLoginRequired: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = {
                    if (isLoggedIn) {
                        val next =
                            if (userReaction == ReactionType.DISLIKE) ReactionType.NONE
                            else ReactionType.DISLIKE
                        onReact(next)
                    } else onLoginRequired()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor =
                        if (userReaction == ReactionType.DISLIKE)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector =
                        if (userReaction == ReactionType.DISLIKE)
                            Icons.Filled.ThumbDown
                        else
                            Icons.Outlined.ThumbDown,
                    contentDescription = "싫어요",
                    tint =
                        if (userReaction == ReactionType.DISLIKE)
                            MaterialTheme.colorScheme.onError
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(6.dp))

            Text(text = "${article.dislikes}")
        }

        Spacer(Modifier.height(6.dp))

        Text(text = "싫어요", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ColorSelectionBar(
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        "#F6EAC2" to "노란색",
        "#90EE90" to "초록색",
        "#C3D6F2" to "하늘색",
        "#FEE1E8" to "핑크색",
        "#FED7C3" to "주황색"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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

@Composable
fun HighlightDragText(
    content: String,
    highlights: List<Highlight>,
    enabled: Boolean,
    onAddHighlight: (startIndex: Int, endExclusive: Int, colorHex: String) -> Unit,
    onDeleteHighlight: (highlightId: Long) -> Unit, // ✅ 추가
    modifier: Modifier = Modifier
) {
    val pastel = remember {
        listOf(
            "#FFB3BA", // 빨강(파스텔)
            "#FFFFBA", // 노랑
            "#BAFFC9", // 초록
            "#BAE1FF", // 파랑
            "#E6CCFF"  // 보라
        )
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var textCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    // 선택 범위 [selStart, selEndExclusive)
    var selStart by remember { mutableIntStateOf(-1) }
    var selEndExclusive by remember { mutableIntStateOf(-1) }

    // 팔레트 팝업
    var popupOpen by remember { mutableStateOf(false) }
    var popupOffset by remember { mutableStateOf(IntOffset(0, 0)) }

    // ✅ 삭제 다이얼로그 상태
    var deleteTarget by remember { mutableStateOf<Highlight?>(null) }

    fun clearSelection() {
        selStart = -1
        selEndExclusive = -1
        popupOpen = false
    }

    // 저장된 하이라이트 + 현재 선택 프리뷰(회색)
    val annotated = remember(content, highlights, selStart, selEndExclusive) {
        buildAnnotatedString {
            append(content)

            highlights.forEach { h ->
                val s = h.startIndex.coerceIn(0, content.length)
                val e = h.endIndex.coerceIn(0, content.length)
                if (e > s) {
                    addStyle(
                        SpanStyle(background = Color(android.graphics.Color.parseColor(h.color))),
                        start = s,
                        end = e
                    )
                }
            }

            if (selStart >= 0 && selEndExclusive > selStart) {
                addStyle(
                    SpanStyle(background = Color(0x55000000)),
                    start = selStart,
                    end = selEndExclusive
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {

        // ✅ 삭제 다이얼로그
        if (enabled && deleteTarget != null) {
            val target = deleteTarget!!
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("하이라이트 삭제") },
                text = {
                    val preview = runCatching {
                        val s = target.startIndex.coerceIn(0, content.length)
                        val e = target.endIndex.coerceIn(0, content.length)
                        content.substring(s, e).trim().take(60)
                    }.getOrNull()

                    Text(
                        text = if (!preview.isNullOrBlank())
                            "이 하이라이트를 삭제할까요?\n\n\"$preview\""
                        else
                            "이 하이라이트를 삭제할까요?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // ✅ 로컬 DB 삭제 (ViewModel -> Repository)
                            onDeleteHighlight(target.id)
                            deleteTarget = null
                            clearSelection()
                        }
                    ) { Text("삭제") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) { Text("취소") }
                }
            )
        }

        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { textCoords = it }
                .pointerInput(enabled, highlights) {
                    if (!enabled) return@pointerInput

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val l = layoutResult ?: return@awaitEachGesture

                        // ✅ 먼저 "롱프레스"가 되나 기다림
                        val longPress = awaitLongPressOrCancellation(down.id)

                        // ✅ 롱프레스가 성립하지 않았다 = 보통 "탭" (하이라이트 클릭 삭제)
                        if (longPress == null) {
                            val tapped = l.getOffsetForPosition(down.position)
                                .coerceIn(0, max(0, content.length - 1))

                            // 탭한 위치가 어떤 하이라이트 범위 안인지 검사
                            val hit = highlights
                                .filter { h ->
                                    val s = h.startIndex.coerceIn(0, content.length)
                                    val e = h.endIndex.coerceIn(0, content.length)
                                    tapped in s until e
                                }
                                // 겹칠 때는 "가장 최근" 느낌으로 id가 큰 것 우선
                                .maxByOrNull { it.id }

                            if (hit != null) {
                                // 팔레트/선택이 떠있다면 닫고 삭제 다이얼로그
                                clearSelection()
                                deleteTarget = hit
                            } else {
                                // 하이라이트 아닌 곳 탭하면 선택/팝업 정리
                                deleteTarget = null
                                clearSelection()
                            }

                            return@awaitEachGesture
                        }

                        // ✅ 여기서부터는 롱프레스 + 드래그 선택 로직 (기존 그대로)
                        val coords = textCoords ?: return@awaitEachGesture

                        popupOpen = false
                        deleteTarget = null

                        val anchor = l.getOffsetForPosition(longPress.position)
                            .coerceIn(0, max(0, content.length - 1))

                        selStart = anchor
                        selEndExclusive = (anchor + 1).coerceAtMost(content.length)

                        drag(down.id) { change ->
                            val cur = l.getOffsetForPosition(change.position)
                                .coerceIn(0, max(0, content.length - 1))

                            val s = min(anchor, cur)
                            val eInclusive = max(anchor, cur)

                            selStart = s
                            selEndExclusive = (eInclusive + 1).coerceAtMost(content.length)

                            change.consume()
                        }

                        if (selStart >= 0 && selEndExclusive > selStart) {
                            val anchorIndex = selStart.coerceIn(0, max(0, content.length - 1))
                            val box = l.getBoundingBox(anchorIndex)

                            val windowPos = coords.positionInWindow()
                            val x = (windowPos.x + box.left).roundToInt()
                            val y = (windowPos.y + box.top - 120f).roundToInt()

                            popupOffset = IntOffset(x.coerceAtLeast(0), y.coerceAtLeast(0))
                            popupOpen = true
                        } else {
                            clearSelection()
                        }
                    }
                },
            onTextLayout = { layoutResult = it }
        )

        // ✅ 팝업이 떠 있을 때, 바깥 누르면 닫기
        if (enabled && popupOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            clearSelection()
                        }
                    }
            )
        }

        // ✅ “사라지지 않는” 팔레트 팝업
        if (enabled && popupOpen && selStart >= 0 && selEndExclusive > selStart) {
            Popup(
                offset = popupOffset,
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Surface(
                    tonalElevation = 6.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pastel.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(
                                        Color(android.graphics.Color.parseColor(hex)),
                                        CircleShape
                                    )
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable {
                                        onAddHighlight(selStart, selEndExclusive, hex)
                                        clearSelection()
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 하이라이트 가능한 텍스트 (구버전/미사용 가능)
 */
@Composable
fun HighlightableText(
    content: String,
    highlights: List<Highlight>,
    onTextSelected: (startIndex: Int, endIndex: Int, text: String) -> Unit
) {
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

            val sentenceHighlights = highlights.filter { highlight ->
                highlight.startIndex >= startIndex && highlight.endIndex <= endIndex
            }

            val annotatedText = buildAnnotatedString {
                append(sentenceWithPunctuation)

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
 */
@Composable
fun HighlightedText(
    content: String,
    highlights: List<Highlight>
) {
    val annotatedText = buildAnnotatedString {
        append(content)
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

private fun formatDateDetail(isoDate: String): String {
    return try {
        val publishedAt = LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
        publishedAt.format(formatter)
    } catch (e: Exception) {
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
