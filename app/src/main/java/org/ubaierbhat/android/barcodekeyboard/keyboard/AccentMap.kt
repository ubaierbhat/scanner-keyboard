package org.ubaierbhat.android.barcodekeyboard.keyboard

object AccentMap {

    private val variantsByLetter: Map<Char, List<Char>> = mapOf(
        'a' to listOf('à', 'á', 'â', 'ä', 'æ', 'ã', 'å', 'ā'),
        'e' to listOf('è', 'é', 'ê', 'ë', 'ė', 'ē'),
        'i' to listOf('ì', 'í', 'î', 'ï', 'ī'),
        'o' to listOf('ò', 'ó', 'ô', 'ö', 'õ', 'ø', 'ō', 'œ'),
        'u' to listOf('ù', 'ú', 'û', 'ü', 'ū'),
        'c' to listOf('ç', 'ć', 'č'),
        'n' to listOf('ñ', 'ń'),
        's' to listOf('ß', 'ś', 'š'),
        'y' to listOf('ÿ', 'ý'),
        'z' to listOf('ź', 'ž'),
        'd' to listOf('ď', 'đ'),
        'l' to listOf('ł', 'ľ'),
        'r' to listOf('ŕ', 'ř'),
        't' to listOf('ť', 'ŧ'),
        'g' to listOf('ĝ', 'ğ'),
        'h' to listOf('ĥ'),
        'j' to listOf('ĵ'),
        '-' to listOf('_'),
        '$' to listOf('€', '£', '¥'),
        '%' to listOf('‰', '¢'),
    )

    fun variants(letter: Char): List<Char> = variantsByLetter[letter.lowercaseChar()] ?: emptyList()
}
