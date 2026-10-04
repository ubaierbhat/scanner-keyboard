package org.ubaierbhat.android.barcodekeyboard.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentMapTest {

    @Test
    fun eVariantsFollowBriefOrder() {
        assertEquals(listOf('è', 'é', 'ê', 'ë', 'ė', 'ē'), AccentMap.variants('e'))
    }

    @Test
    fun aVariantsFollowBriefOrder() {
        assertEquals(listOf('à', 'á', 'â', 'ä', 'æ', 'ã', 'å', 'ā'), AccentMap.variants('a'))
    }

    @Test
    fun lookupIsCaseInsensitive() {
        assertEquals(AccentMap.variants('e'), AccentMap.variants('E'))
        assertEquals(listOf('ç', 'ć', 'č'), AccentMap.variants('C'))
    }

    @Test
    fun lettersWithoutVariantsReturnEmpty() {
        assertTrue(AccentMap.variants('b').isEmpty())
        assertTrue(AccentMap.variants('q').isEmpty())
        assertTrue(AccentMap.variants('5').isEmpty())
    }

    @Test
    fun singleVariantLettersMatchBrief() {
        assertEquals(listOf('ĥ'), AccentMap.variants('h'))
        assertEquals(listOf('ĵ'), AccentMap.variants('j'))
        assertEquals(listOf('ÿ', 'ý'), AccentMap.variants('y'))
    }

    @Test
    fun hyphenOffersUnderscore() {
        assertEquals(listOf('_'), AccentMap.variants('-'))
    }

    @Test
    fun dollarOffersCurrencyVariants() {
        assertEquals(listOf('€', '£', '¥'), AccentMap.variants('$'))
    }

    @Test
    fun percentOffersCurrencyAndMathVariants() {
        assertEquals(listOf('‰', '¢'), AccentMap.variants('%'))
    }
}
