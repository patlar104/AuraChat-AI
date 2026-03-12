package com.aurachat.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole

/**
 * Room entity representing a row in the `chat_messages` table.
 *
 * A composite index on `(session_id, timestamp)` enables efficient `ORDER BY timestamp ASC`
 * queries scoped to a single session. Deleting a parent [ChatSessionEntity] cascades to
 * all child messages via the [ForeignKey.CASCADE] constraint.
 *
 * Note: [ChatMessage.isStreaming] is a runtime-only flag and is intentionally not persisted.
 *
 * Use [toDomain] to convert to the domain [ChatMessage] model, and [ChatMessage.toEntity]
 * to convert back for persistence.
 */
@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["session_id", "timestamp"]),
    ],
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null,

    /** Stored as the enum name (`"USER"` or `"MODEL"`); reconstructed via [MessageRole.valueOf]. */
    @ColumnInfo(name = "role")
    val role: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "is_error")
    val isError: Boolean = false,
)

/** Maps this entity to the domain [ChatMessage] model. [ChatMessage.isStreaming] is always false when reading from DB. */
fun ChatMessageEntity.toDomain() = ChatMessage(
    id = id,
    sessionId = sessionId,
    content = content,
    imageUri = imageUri,
    role = MessageRole.valueOf(role),
    timestamp = timestamp,
    isStreaming = false,
    isError = isError,
)

/** Maps the domain [ChatMessage] to its Room entity representation. [ChatMessage.isStreaming] is not mapped — not a DB field. */
fun ChatMessage.toEntity() = ChatMessageEntity(
    id = id,
    sessionId = sessionId,
    content = content,
    imageUri = imageUri,
    role = role.name,
    timestamp = timestamp,
    isError = isError,
)
