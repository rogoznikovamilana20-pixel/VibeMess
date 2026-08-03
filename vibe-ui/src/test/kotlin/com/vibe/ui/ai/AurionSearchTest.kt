package com.vibe.ui.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AurionSearchTest {

    @Test
    fun parseVariants_handlesPlainLines() {
        val raw = """
            отпуск июнь
            отдых на море
            где отдохнуть летом
        """.trimIndent()
        val variants = AurionSearch.parseVariants(raw)
        assertEquals(listOf("отпуск июнь", "отдых на море", "где отдохнуть летом"), variants)
    }

    @Test
    fun parseVariants_stripsMarkersAndQuotes() {
        val raw = """
            1. «поездка на юг»
            2) погода в сочи
            - командировка
            * квитанция за отель
        """.trimIndent()
        val variants = AurionSearch.parseVariants(raw)
        assertEquals(
            listOf("поездка на юг", "погода в сочи", "командировка", "квитанция за отель"),
            variants
        )
    }

    @Test
    fun parseVariants_deduplicatesAndDropsTooLong() {
        val raw = """
            отпуск
            ОТПУСК
            ${"а".repeat(200)}
        """.trimIndent()
        val variants = AurionSearch.parseVariants(raw)
        assertEquals(listOf("отпуск"), variants)
    }

    @Test
    fun parseVariants_returnsEmptyForGarbage() {
        assertEquals(emptyList<String>(), AurionSearch.parseVariants(""))
        assertEquals(emptyList<String>(), AurionSearch.parseVariants("   \n  \n"))
        assertEquals(emptyList<String>(), AurionSearch.parseVariants("***"))
    }

    @Test
    fun parseRerankOrder_parsesJsonArray() {
        assertEquals(listOf(3, 0, 1, 2), AurionSearch.parseRerankOrder("[3,0,1,2]", 5))
        assertEquals(listOf(2, 0, 1), AurionSearch.parseRerankOrder("Вот результат: [2, 0, 1]", 3))
    }

    @Test
    fun parseRerankOrder_filtersOutOfRangeAndDuplicates() {
        assertEquals(listOf(1, 0), AurionSearch.parseRerankOrder("[1, 99, 0, -2, 1]", 3))
    }

    @Test
    fun parseRerankOrder_returnsEmptyForInvalid() {
        assertEquals(emptyList<Int>(), AurionSearch.parseRerankOrder(null, 5))
        assertEquals(emptyList<Int>(), AurionSearch.parseRerankOrder("", 5))
        assertEquals(emptyList<Int>(), AurionSearch.parseRerankOrder("просто текст", 5))
        assertEquals(emptyList<Int>(), AurionSearch.parseRerankOrder("[3,0]", 0))
    }

    @Test
    fun parseRerankOrder_keepsOrderStableWithSpaces() {
        val order = AurionSearch.parseRerankOrder("[ 4 , 1 , 2 ]", 5)
        assertEquals(listOf(4, 1, 2), order)
        assertTrue("ids must be distinct", order.distinct().size == order.size)
    }
}
