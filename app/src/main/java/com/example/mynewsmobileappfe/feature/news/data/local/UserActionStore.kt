package com.example.mynewsmobileappfe.feature.news.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mynewsmobileappfe.feature.news.domain.model.ReactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userActionDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_actions")

/**
 * 사용자의 기사 액션(좋아요/싫어요/북마크)을 로컬에 영구 저장하여
 * 앱 재시작 후에도 상태를 복원한다.
 *
 * - liked/disliked/bookmarked를 각각 ID 집합으로 관리
 * - ReactionType.NONE이면 좋아요/싫어요 집합에서 제거
 */
@Singleton
class UserActionStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val likedKey = stringSetPreferencesKey("liked_ids")
    private val dislikedKey = stringSetPreferencesKey("disliked_ids")
    private val bookmarkedKey = stringSetPreferencesKey("bookmarked_ids")

    data class Snapshot(
        val likedIds: Set<Long> = emptySet(),
        val dislikedIds: Set<Long> = emptySet(),
        val bookmarkedIds: Set<Long> = emptySet()
    ) {
        val reactions: Map<Long, ReactionType>
            get() {
                val map = mutableMapOf<Long, ReactionType>()
                likedIds.forEach { map[it] = ReactionType.LIKE }
                dislikedIds.forEach { map[it] = ReactionType.DISLIKE }
                return map
            }
    }

    val snapshotFlow: Flow<Snapshot> = context.userActionDataStore.data.map { prefs ->
        Snapshot(
            likedIds = prefs[likedKey].orEmpty().toLongSet(),
            dislikedIds = prefs[dislikedKey].orEmpty().toLongSet(),
            bookmarkedIds = prefs[bookmarkedKey].orEmpty().toLongSet()
        )
    }

    suspend fun getSnapshot(): Snapshot = snapshotFlow.first()

    suspend fun setReaction(articleId: Long, reactionType: ReactionType) {
        context.userActionDataStore.edit { prefs ->
            val liked = prefs[likedKey].orEmpty().toMutableSet()
            val disliked = prefs[dislikedKey].orEmpty().toMutableSet()

            // 반응은 단일 선택이므로 두 집합 모두에서 제거 후 다시 추가
            liked.remove(articleId.toString())
            disliked.remove(articleId.toString())

            when (reactionType) {
                ReactionType.LIKE -> liked.add(articleId.toString())
                ReactionType.DISLIKE -> disliked.add(articleId.toString())
                ReactionType.NONE -> {}
            }

            prefs[likedKey] = liked
            prefs[dislikedKey] = disliked
        }
    }

    suspend fun setBookmarked(articleId: Long, bookmarked: Boolean) {
        context.userActionDataStore.edit { prefs ->
            val bookmarkedIds = prefs[bookmarkedKey].orEmpty().toMutableSet()
            if (bookmarked) {
                bookmarkedIds.add(articleId.toString())
            } else {
                bookmarkedIds.remove(articleId.toString())
            }
            prefs[bookmarkedKey] = bookmarkedIds
        }
    }

    private fun Set<String>.toLongSet(): Set<Long> = mapNotNull { it.toLongOrNull() }.toSet()
}
