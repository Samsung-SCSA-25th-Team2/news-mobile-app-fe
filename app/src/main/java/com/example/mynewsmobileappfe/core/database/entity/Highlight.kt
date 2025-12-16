package com.example.mynewsmobileappfe.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 형광펜 하이라이트 엔티티
 *
 * 사용자가 기사 본문에서 선택한 텍스트와 색상 정보를 저장합니다.
 */
@Entity(tableName = "highlights")
data class Highlight(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 기사 ID (ArticleResponse.articleId) */
    val articleId: Long,

    /** 하이라이트 시작 위치 (텍스트 인덱스) */
    val startIndex: Int,

    /** 하이라이트 끝 위치 (텍스트 인덱스) */
    val endIndex: Int,

    /** 하이라이트한 텍스트 내용 */
    val text: String,

    /** 형광펜 색상 (Hex 코드, 예: "#FFFF00") */
    val color: String,

    /** 생성 시간 (밀리초) */
    val createdAt: Long = System.currentTimeMillis()
)