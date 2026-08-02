package com.vibe.ui.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vibe.ui.data.db.dao.AccountDao
import com.vibe.ui.data.db.dao.BotDao
import com.vibe.ui.data.db.dao.BotMessageDao
import com.vibe.ui.data.db.dao.ChatDao
import com.vibe.ui.data.db.dao.ContactDao
import com.vibe.ui.data.db.dao.MarketplaceDao
import com.vibe.ui.data.db.dao.MeshDao
import com.vibe.ui.data.db.dao.MessageDao
import com.vibe.ui.data.db.dao.PayoutRequestDao
import com.vibe.ui.data.db.dao.PurchaseDao
import com.vibe.ui.data.db.dao.SparkBalanceDao
import com.vibe.ui.data.db.dao.TimelineDao
import com.vibe.ui.data.db.dao.VibePlusStatusDao
import com.vibe.ui.data.db.entity.AccountEntity
import com.vibe.ui.data.db.entity.BotEntity
import com.vibe.ui.data.db.entity.BotMessageEntity
import com.vibe.ui.data.db.entity.ChatEntity
import com.vibe.ui.data.db.entity.ContactEntity
import com.vibe.ui.data.db.entity.MarketplaceListingEntity
import com.vibe.ui.data.db.entity.MeshMessageEntity
import com.vibe.ui.data.db.entity.MessageEntity
import com.vibe.ui.data.db.entity.PayoutRequestEntity
import com.vibe.ui.data.db.entity.PurchaseEntity
import com.vibe.ui.data.db.entity.SparkBalanceEntity
import com.vibe.ui.data.db.entity.TimelinePostEntity
import com.vibe.ui.data.db.entity.VibePlusStatusEntity

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

private val SQL_CREATE_ACCOUNTS = """
    CREATE TABLE IF NOT EXISTS `vibe_accounts` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `name` TEXT NOT NULL,
        `username` TEXT NOT NULL,
        `email` TEXT NOT NULL,
        `passwordHash` TEXT NOT NULL,
        `vibeId` TEXT NOT NULL,
        `avatarPath` TEXT,
        `bio` TEXT NOT NULL DEFAULT '',
        `createdAt` INTEGER NOT NULL
    )
""".trimIndent()

private val SQL_CREATE_BOTS = """
    CREATE TABLE IF NOT EXISTS `bots` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `serverId` TEXT NOT NULL DEFAULT '',
        `token` TEXT NOT NULL DEFAULT '',
        `name` TEXT NOT NULL,
        `username` TEXT NOT NULL,
        `description` TEXT NOT NULL DEFAULT '',
        `avatarInitial` TEXT NOT NULL DEFAULT '',
        `systemPrompt` TEXT NOT NULL DEFAULT '',
        `commandsJson` TEXT NOT NULL DEFAULT '[]',
        `scriptRepliesJson` TEXT NOT NULL DEFAULT '[]',
        `isAi` INTEGER NOT NULL DEFAULT 1,
        `isEnabled` INTEGER NOT NULL DEFAULT 1,
        `isLocal` INTEGER NOT NULL DEFAULT 1,
        `lastUpdateId` INTEGER NOT NULL DEFAULT 0,
        `createdAt` INTEGER NOT NULL
    )
""".trimIndent()

private val SQL_CREATE_BOT_MESSAGES = """
    CREATE TABLE IF NOT EXISTS `bot_messages` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `botId` INTEGER NOT NULL,
        `text` TEXT NOT NULL,
        `isUser` INTEGER NOT NULL,
        `timestamp` INTEGER NOT NULL
    )
""".trimIndent()

internal val MIGRATION_1_2 = Migration(1, 2) { db ->
    db.execSQL(SQL_CREATE_CHATS)
    db.execSQL(SQL_CREATE_MESSAGES)
    db.execSQL(SQL_CREATE_CONTACTS)
    db.execSQL(SQL_CREATE_AI_MESSAGES)
    db.execSQL(SQL_CREATE_TIMELINE)
    db.execSQL(SQL_CREATE_MARKETPLACE)
}

internal val MIGRATION_2_3 = Migration(2, 3) { db ->
    db.execSQL(SQL_CREATE_CHATS)
    db.execSQL(SQL_CREATE_MESSAGES)
    db.execSQL(SQL_CREATE_CONTACTS)
    db.execSQL(SQL_CREATE_AI_MESSAGES)
    db.execSQL(SQL_CREATE_TIMELINE)
    db.execSQL(SQL_CREATE_MARKETPLACE)
}

internal val MIGRATION_3_4 = Migration(3, 4) { db ->
    db.execSQL("ALTER TABLE `chats` ADD COLUMN `isPersonal` INTEGER NOT NULL DEFAULT 1")
    db.execSQL("ALTER TABLE `chats` ADD COLUMN `lastSynced` INTEGER NOT NULL DEFAULT 0")
    db.execSQL("ALTER TABLE `messages` ADD COLUMN `lastSynced` INTEGER NOT NULL DEFAULT 0")
    db.execSQL("ALTER TABLE `contacts` ADD COLUMN `lastSynced` INTEGER NOT NULL DEFAULT 0")
}

internal val MIGRATION_4_5 = Migration(4, 5) { db ->
    db.execSQL("ALTER TABLE `timeline_posts` ADD COLUMN `isLiked` INTEGER NOT NULL DEFAULT 0")
}

internal val MIGRATION_5_6 = Migration(5, 6) { db ->
    db.execSQL(SQL_CREATE_ACCOUNTS)
}

internal val MIGRATION_6_7 = Migration(6, 7) { db ->
    db.execSQL(SQL_CREATE_BOTS)
    db.execSQL(SQL_CREATE_BOT_MESSAGES)
}

private val SQL_CREATE_SPARK_BALANCE = """
    CREATE TABLE IF NOT EXISTS `spark_balance` (
        `id` INTEGER PRIMARY KEY NOT NULL,
        `balance` INTEGER NOT NULL,
        `updatedAt` INTEGER NOT NULL
    )
""".trimIndent()

private val SQL_CREATE_VIBE_PLUS_STATUS = """
    CREATE TABLE IF NOT EXISTS `vibe_plus_status` (
        `id` INTEGER PRIMARY KEY NOT NULL,
        `isActive` INTEGER NOT NULL,
        `expiresAt` INTEGER NOT NULL,
        `trialUsed` INTEGER NOT NULL
    )
""".trimIndent()

private val SQL_CREATE_PURCHASES = """
    CREATE TABLE IF NOT EXISTS `purchases` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `itemType` TEXT NOT NULL,
        `amountKopecks` INTEGER NOT NULL,
        `status` TEXT NOT NULL,
        `providerPaymentId` TEXT NOT NULL DEFAULT '',
        `createdAt` INTEGER NOT NULL
    )
""".trimIndent()

private val SQL_CREATE_PAYOUT_REQUESTS = """
    CREATE TABLE IF NOT EXISTS `payout_requests` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `sparks` INTEGER NOT NULL,
        `bankName` TEXT NOT NULL,
        `accountNumber` TEXT NOT NULL,
        `status` TEXT NOT NULL,
        `createdAt` INTEGER NOT NULL
    )
""".trimIndent()

internal val MIGRATION_7_8 = Migration(7, 8) { db ->
    db.execSQL(SQL_CREATE_SPARK_BALANCE)
    db.execSQL(SQL_CREATE_VIBE_PLUS_STATUS)
    db.execSQL(SQL_CREATE_PURCHASES)
    db.execSQL(SQL_CREATE_PAYOUT_REQUESTS)
}

private val SQL_CREATE_MESH_MESSAGES = """
    CREATE TABLE IF NOT EXISTS `mesh_messages` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `messageId` TEXT NOT NULL,
        `fromPeerId` TEXT NOT NULL,
        `toPeerId` TEXT NOT NULL DEFAULT '',
        `text` TEXT NOT NULL DEFAULT '',
        `mediaPath` TEXT,
        `status` TEXT NOT NULL,
        `isOutgoing` INTEGER NOT NULL,
        `deliveredViaMesh` INTEGER NOT NULL,
        `createdAt` INTEGER NOT NULL
    )
""".trimIndent()

internal val MIGRATION_8_9 = Migration(8, 9) { db ->
    db.execSQL(SQL_CREATE_MESH_MESSAGES)
}

/**
 * v10: chats get a composite primary key (accountId, id).
 * Telegram chat ids are only unique per account; without this, multiple accounts
 * would overwrite each other's chats.
 */
internal val MIGRATION_9_10 = Migration(9, 10) { db ->
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `chats_new` (
            `accountId` INTEGER NOT NULL,
            `id` INTEGER NOT NULL,
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
            `lastSynced` INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(`accountId`, `id`)
        )
        """.trimIndent()
    )
    db.execSQL(
        """
        INSERT INTO `chats_new`
            (accountId, id, title, type, lastMessageText, lastMessageTime, unreadCount,
             isMuted, isPinned, isArchived, draftText, isPersonal, lastSynced)
        SELECT 0, id, title, type, lastMessageText, lastMessageTime, unreadCount,
             isMuted, isPinned, isArchived, draftText, isPersonal, lastSynced
        FROM `chats`
        """.trimIndent()
    )
    db.execSQL("DROP TABLE `chats`")
    db.execSQL("ALTER TABLE `chats_new` RENAME TO `chats`")
}

/**
 * v11: chats get a cached Telegram avatar file path ([ChatEntity.avatarPath]).
 */
internal val MIGRATION_10_11 = Migration(10, 11) { db ->
    db.execSQL("ALTER TABLE `chats` ADD COLUMN `avatarPath` TEXT")
}

@Database(
    entities = [
        AccountEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        ContactEntity::class,
        TimelinePostEntity::class,
        MarketplaceListingEntity::class,
        BotEntity::class,
        BotMessageEntity::class,
        SparkBalanceEntity::class,
        VibePlusStatusEntity::class,
        PurchaseEntity::class,
        PayoutRequestEntity::class,
        MeshMessageEntity::class
    ],
    version = 11,
    exportSchema = true
)
abstract class VibeDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao
    abstract fun timelineDao(): TimelineDao
    abstract fun marketplaceDao(): MarketplaceDao
    abstract fun botDao(): BotDao
    abstract fun botMessageDao(): BotMessageDao
    abstract fun sparkBalanceDao(): SparkBalanceDao
    abstract fun vibePlusStatusDao(): VibePlusStatusDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun payoutRequestDao(): PayoutRequestDao
    abstract fun meshDao(): MeshDao

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
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                        MIGRATION_9_10, MIGRATION_10_11
                    )
                    // Never crash the app on a schema mismatch — the DB is a cache
                    // that can be rebuilt. Vibe data lives in Telegram's storage.
                    .fallbackToDestructiveMigration()
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
