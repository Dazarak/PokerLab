// Deck.kt
package fr.thomasdomenech.pokerlab.Class

import fr.thomasdomenech.pokerlab.R

class Deck {
    private val cards: MutableList<Card> = mutableListOf()

    init {
        val values = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
        val symbols = listOf(
            R.drawable.diamond,
            R.drawable.heart,
            R.drawable.spade,
            R.drawable.club
        )

        for (value in values) {
            for (symbol in symbols) {
                cards.add(Card(value, symbol))
            }
        }
        cards.shuffle()
    }

    fun drawCard(): Card? {
        return if (cards.isNotEmpty()) cards.removeAt(0) else null
    }
}