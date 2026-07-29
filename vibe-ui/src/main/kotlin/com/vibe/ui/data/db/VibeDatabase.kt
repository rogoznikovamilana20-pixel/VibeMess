package com.vibe.ui.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vibe.ui.data.db.dao.ChatDao
import com.vibe.ui.data.db.dao.ContactDao
import com.vibe.ui.data.db.dao.MarketplaceDao
import com.vibe.ui.data.db.dao.MessageDao
import com.vibe.ui.data.db.dao.TimelineDao
import com.vibe.ui.data.db.entity.ChatEntity
import com.vibe.ui.data.db.entity.ContactEntity
import com.vibe.ui.data.db.entity.MarketplaceListingEntity
import com.vibe.ui.data.db.entity.MessageEntity
import com.vibe.ui.data.db.entity.TimelinePostEntity

private val SQL_CREATE_CHATS = """
    CREATE TABLE IF NOT EXISTS `chats` (
        `id` INTEGER PRIMARY KEY NOT NULL,
        `title` TEXT NOT NULL,
        `type` TEXT NOT NULL,
        `lastMessageText` TEXT,
        `lastMessageTime` INTEGER,
        `unreadCount` INTEGER NOT NULL,
        `isMuted` INTEGER NOT NULL,
        `isPinned` INTEGER NOT NULL,
        `isArchived` INTEGER NOT NULL,
        `draftText` TEXT,
        `isPersonal` INTEGER NOT NULL DEFAULT 1,
        `lastSynced` INTEGER NOT NULL DEFAULT 0
    )
""".trimIndent()

private val SQL_CREATE_MESSAGES = """
    CREATE TABLE IF NOT EXISTS `messages` (
        `id` INTEGER PRIMARY KEY NOT NULL,
        `chatId` INTEGER NOT NULL,
        `senderId` INTEGER NOT NULL,
        `text` TEXT NOT NULL,
        `timestamp` INTEGER NOT NULL,
        `type` TEXT NOT NULL,
        `isOutgoing` INTEGER NOT NULL,
        `isPinned` INTEGER NOT NULL,
        `replyId` INTEGER,
        `attachmentPath` TEXT,
        `deliveryStatus` TEXT,
        `lastSynced` INTEGER NOT NULL DEFAULT 0
    )
""".trimIndent()

private val SQL_CREATE_CONTACTS = """
    CREATE TABLE IF NOT EXISTS `contacts` (
        `id` INTEGER PRIMARY KEY NOT NULL,
        `firstName` TEXT NOT NULL,
        `lastName` TEXT,
        `username` TEXT,
        `phone` TEXT,
        `isBot` INTEGER NOT NULL,
        `isPremium` INTEGER NOT NULL,
        `avatarPhotoId` INTEGER,
        `lastSynced` INTEGER NOT NULL DEFAULT 0
    )
""".trimIndent()

private val SQL_CREATE_AI_MESSAGES = """
    CREATE TABLE IF NOT EXISTS `ai_messages` (
        `id` INTEGER PRIMARY KEY NOT NULL,
        `content` TEXT NOT NULL,
        `isUser` INTEGER NOT NULL,
        `timestamp` INTEGER NOT NULL,
        `model` TEXT,
        `tokensUsed` INTEGER NOT NULL DEFAULT 0
    )
""".trimIndent()

private val SQL_CREATE_TIMELINE = """
    CREATE TABLE IF NOT EXISTS `timeline_posts` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `content` TEXT NOT NULL,
        `authorName` TEXT NOT NULL,
        `timestamp` INTEGER NOT NULL,
        `imageUri` TEXT,
        `likes` INTEGER NOT NULL DEFAULT 0,
        `comments` INTEGER NOT NULL DEFAULT 0
    )
""".trimIndent()

private val SQL_CREATE_MARKETPLACE = """
    CREATE TABLE IF NOT EXISTS `marketplace_listings` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `title` TEXT NOT NULL,
        `description` TEXT NOT NULL,
        `price` REAL NOT NULL,
        `category` TEXT NOT NULL,
        `imageUri` TEXT,
        `createdAt` INTEGER NOT NULL,
        `sellerName` TEXT NOT NULL,
        `isActive` INTEGER NOT NULL DEFAULT 1
    )
""".trimIndent()

private val MIGRATION_1_2 = Migration(1, 2) { db ->
    db.execSQL(SQL_CREATE_CHATS)
    db.execSQL(SQL_CREATE_MESSAGES)
    db.execSQL(SQL_CREATE_CONTACTS)
    db.execSQL(SQL_CREATE_AI_MESSAGES)
    db.execSQL(SQL_CREATE_TIMELINE)
    db.execSQL(SQL_CREATE_MARKETPLACE)
}

private val MIGRATION_2_3 = Migration(2, 3) { db ->
    db.execSQL(SQL_CREATE_CHATS)
    db.execSQL(SQL_CREATE_MESSAGES)
    db.execSQL(SQL_CREATE_CONTACTS)
    db.execSQL(SQL_CREATE_AI_MESSAGES)
    db.execSQL(SQL_CREATE_TIMELINE)
    db.execSQL(SQL_CREATE_MARKETPLACE)
}

// Version 3→4: Added `isPersonal` to chats, `lastSynced` to chats and messages.
// Uses CREATE TABLE IF NOT EXISTS for idempotent schema updates.
private val MIGRATION_3_4 = Migration(3, 4) { db ->
    db.execSQL("ALTER TABLE `chats` ADD COLUMN `isPersonal` INTEGER NOT NULL DEFAULT 1")
    db.execSQL("ALTER TABLE `chats` ADD COLUMN `lastSynced` INTEGER NOT NULL DEFAULT 0")
    db.execSQL("ALTER TABLE `messages` ADD COLUMN `lastSynced` INTEGER NOT NULL DEFAULT 0")
    db.execSQL("ALTER TABLE `contacts` ADD COLUMN `lastSynced` INTEGER NOT NULL DEFAULT 0")
}

@Database(
    entities = [
        ChatEntity::class,
        MessageEntity::class,
        ContactEntity::class,
        TimelinePostEntity::class,
        MarketplaceListingEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class VibeDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao
    abstract fun timelineDao(): TimelineDao
    abstract fun marketplaceDao(): MarketplaceDao

    companion object {
        @Volatile
        private var INSTANCE: VibeDatabase? = null

        fun getDatabase(context: Context): VibeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VibeDatabase::class.java,
                    "vibe_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun resetInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
