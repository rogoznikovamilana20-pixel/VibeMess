package com.vibe.ui.data.db

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.sql.DriverManager

class VibeDatabaseMigrationTest {

    private val dbUrl = "jdbc:sqlite::memory:"

    @Before
    fun setUp() {
        Class.forName("org.sqlite.JDBC")
    }

    @Test
    fun migrateV3ToV4_preservesData() {
        val conn = DriverManager.getConnection(dbUrl)
        conn.createStatement().apply {
            executeUpdate("CREATE TABLE `chats` (" +
                    "`id` INTEGER PRIMARY KEY NOT NULL, " +
                    "`title` TEXT NOT NULL, " +
                    "`type` TEXT NOT NULL, " +
                    "`lastMessageText` TEXT, " +
                    "`lastMessageTime` INTEGER, " +
                    "`unreadCount` INTEGER NOT NULL, " +
                    "`isMuted` INTEGER NOT NULL, " +
                    "`isPinned` INTEGER NOT NULL, " +
                    "`isArchived` INTEGER NOT NULL, " +
                    "`draftText` TEXT)")
            executeUpdate("INSERT INTO `chats` (`id`, `title`, `type`, `unreadCount`, `isMuted`, `isPinned`, `isArchived`) " +
                    "VALUES (1, 'Test Chat', 'private', 0, 0, 0, 0)")
            close()
        }

        conn.createStatement().executeUpdate("ALTER TABLE `chats` ADD COLUMN `isPersonal` INTEGER NOT NULL DEFAULT 1")
        conn.createStatement().executeUpdate("ALTER TABLE `chats` ADD COLUMN `lastSynced` INTEGER NOT NULL DEFAULT 0")

        val stmt = conn.createStatement()
        val rs = stmt.executeQuery("SELECT * FROM `chats` WHERE `id` = 1")
        rs.next()

        assertEquals(1, rs.getInt("id"))
        assertEquals("Test Chat", rs.getString("title"))
        assertEquals("private", rs.getString("type"))
        assertEquals(1, rs.getInt("isPersonal"))
        assertEquals(0L, rs.getLong("lastSynced"))

        rs.close()
        stmt.close()
        conn.close()
    }

    @Test
    fun migrateV3ToV4_messagesAndContacts() {
        val conn = DriverManager.getConnection(dbUrl)
        conn.createStatement().apply {
            executeUpdate("CREATE TABLE `messages` (" +
                    "`id` INTEGER PRIMARY KEY NOT NULL, " +
                    "`chatId` INTEGER NOT NULL, " +
                    "`senderId` INTEGER NOT NULL, " +
                    "`text` TEXT NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL, " +
                    "`type` TEXT NOT NULL, " +
                    "`isOutgoing` INTEGER NOT NULL, " +
                    "`isPinned` INTEGER NOT NULL, " +
                    "`replyId` INTEGER, " +
                    "`attachmentPath` TEXT, " +
                    "`deliveryStatus` TEXT)")
            executeUpdate("CREATE TABLE `contacts` (" +
                    "`id` INTEGER PRIMARY KEY NOT NULL, " +
                    "`firstName` TEXT NOT NULL, " +
                    "`lastName` TEXT, " +
                    "`username` TEXT, " +
                    "`phone` TEXT, " +
                    "`isBot` INTEGER NOT NULL, " +
                    "`isPremium` INTEGER NOT NULL, " +
                    "`avatarPhotoId` INTEGER)")
            executeUpdate("INSERT INTO `messages` (`id`, `chatId`, `senderId`, `text`, `timestamp`, `type`, `isOutgoing`, `isPinned`) " +
                    "VALUES (1, 1, 1, 'Hello', 1000, 'text', 0, 0)")
            executeUpdate("INSERT INTO `contacts` (`id`, `firstName`, `isBot`, `isPremium`) " +
                    "VALUES (1, 'Alice', 0, 0)")
            close()
        }

        conn.createStatement().executeUpdate("ALTER TABLE `messages` ADD COLUMN `lastSynced` INTEGER NOT NULL DEFAULT 0")
        conn.createStatement().executeUpdate("ALTER TABLE `contacts` ADD COLUMN `lastSynced` INTEGER NOT NULL DEFAULT 0")

        val msgStmt = conn.createStatement()
        val msgRs = msgStmt.executeQuery("SELECT * FROM `messages` WHERE `id` = 1")
        msgRs.next()
        assertEquals("Hello", msgRs.getString("text"))
        assertEquals(0L, msgRs.getLong("lastSynced"))
        msgRs.close()
        msgStmt.close()

        val cStmt = conn.createStatement()
        val cRs = cStmt.executeQuery("SELECT * FROM `contacts` WHERE `id` = 1")
        cRs.next()
        assertEquals("Alice", cRs.getString("firstName"))
        assertEquals(0L, cRs.getLong("lastSynced"))
        cRs.close()
        cStmt.close()

        conn.close()
    }
}