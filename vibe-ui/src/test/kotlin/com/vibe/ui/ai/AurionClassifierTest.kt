package com.vibe.ui.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AurionClassifierTest {

    @Test
    fun fallback_channelsAreWork() {
        assertTrue(!AurionClassifier.fallbackIsPersonal("CHANNEL", "Мой отпуск"))
        assertTrue(!AurionClassifier.fallbackIsPersonal("channel", "Котики"))
    }

    @Test
    fun fallback_keywordTitlesAreWork() {
        assertTrue(!AurionClassifier.fallbackIsPersonal("PRIVATE", "Работа — бухгалтерия"))
        assertTrue(!AurionClassifier.fallbackIsPersonal("GROUP", "Команда проекта"))
        assertTrue(!AurionClassifier.fallbackIsPersonal("SUPERGROUP", "Sales отдел"))
        assertTrue(!AurionClassifier.fallbackIsPersonal("PRIVATE", "Техподдержка"))
    }

    @Test
    fun fallback_personalByDefault() {
        assertTrue(AurionClassifier.fallbackIsPersonal("PRIVATE", "Мама"))
        assertTrue(AurionClassifier.fallbackIsPersonal("GROUP", "Друзья из универа"))
        assertTrue(AurionClassifier.fallbackIsPersonal("SUPERGROUP", "Футбольный клуб"))
    }

    @Test
    fun fallback_caseInsensitive() {
        assertTrue(!AurionClassifier.fallbackIsPersonal("PRIVATE", "OFFICE"))
        assertTrue(!AurionClassifier.fallbackIsPersonal("PRIVATE", "Проект Z"))
    }

    @Test
    fun parseClassification_parsesJsonObject() {
        val parsed = AurionClassifier.parseClassification(
            """{"1":"рабочая","3":"личная","4":"рабоч"}""",
            4
        )
        assertEquals(mapOf(1 to false, 3 to true, 4 to false), parsed)
    }

    @Test
    fun parseClassification_parsesBareLines() {
        val parsed = AurionClassifier.parseClassification(
            "1: личная\n2 - рабочая\n3: personal",
            3
        )
        assertEquals(mapOf(1 to true, 2 to false, 3 to true), parsed)
    }

    @Test
    fun parseClassification_filtersOutOfRangeAndUnknown() {
        val parsed = AurionClassifier.parseClassification(
            """{"0":"рабочая","2":"личная","99":"личная","7":"неизвестное","8":"да"}""",
            3
        )
        assertEquals(mapOf(2 to true), parsed)
    }

    @Test
    fun parseClassification_returnsNullForGarbage() {
        assertNull(AurionClassifier.parseClassification("", 5))
        assertNull(AurionClassifier.parseClassification(null, 5))
        assertNull(AurionClassifier.parseClassification("просто текст без цифр", 5))
        assertNull(AurionClassifier.parseClassification("[1,2,3]", 3))
        assertNull(AurionClassifier.parseClassification("""{"1":"рабочая"}""", 0))
    }

    @Test
    fun parseClassification_returnsNullWhenNothingUsable() {
        assertNull(AurionClassifier.parseClassification("""{"1":"среднее"}""", 3))
    }
}
