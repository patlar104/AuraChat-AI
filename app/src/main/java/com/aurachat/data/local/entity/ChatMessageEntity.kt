package com.aurachat.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE // Deleting a session cascades to all its messages
        )
    ],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["session_id", "timestamp"]) // Composite for ORDER BY within session
    ]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "role")
    val role: String, // Stored as "USER" or "MODEL"; reconstructed via MessageRole.valueOf()

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    // isStreaming deliberately omitted — runtime-only, never persisted

    @ColumnInfo(name = "is_error")
    val isError: Boolean = false
)

fun ChatMessageEntity.toDomain() = ChatMessage(
    id = id,
    sessionId = sessionId,
    content = content,
    role = MessageRole.valueOf(role),
    timestamp = timestamp,
    isStreaming = false, // Always false when reading from DB
    isError = isError
)

fun ChatMessage.toEntity() = ChatMessageEntity(
    id = id,
    sessionId = sessionId,
    content = content,
    role = role.name,
    timestamp = timestamp,
    isError = isError
    // isStreaming not mapped — not a DB field
)
