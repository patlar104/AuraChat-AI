package com.aurachat.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aurachat.domain.model.ChatSession

/**
 * Room entity representing a row in the `chat_sessions` table.
 *
 * The [updatedAt] column is indexed for efficient `ORDER BY updated_at DESC` queries
 * used when loading the history drawer.
 *
 * Use [toDomain] to convert to the domain [ChatSession] model, and [ChatSession.toEntity]
 * to convert back for persistence.
 */
@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at", index = true)
    val updatedAt: Long,

    @ColumnInfo(name = "message_count")
    val messageCount: Int = 0,

    @ColumnInfo(name = "last_message_preview")
    val lastMessagePreview: String = "",

    @ColumnInfo(name = "pending_initial_prompt")
    val pendingInitialPrompt: String? = null,
)

/** Maps this entity to the domain [ChatSession] model. */
fun ChatSessionEntity.toDomain() = ChatSession(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    messageCount = messageCount,
    lastMessagePreview = lastMessagePreview,
    pendingInitialPrompt = pendingInitialPrompt,
)

/** Maps the domain [ChatSession] to its Room entity representation. */
fun ChatSession.toEntity() = ChatSessionEntity(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    messageCount = messageCount,
    lastMessagePreview = lastMessagePreview,
    pendingInitialPrompt = pendingInitialPrompt,
)
