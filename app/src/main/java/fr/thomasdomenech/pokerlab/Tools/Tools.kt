package fr.thomasdomenech.pokerlab.Tools

import android.app.Dialog
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import fr.thomasdomenech.pokerlab.Class.Card
import fr.thomasdomenech.pokerlab.Class.Player
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import fr.thomasdomenech.pokerlab.R
import java.io.File
import kotlin.contracts.contract

data class PlayerHand(val name: String, val hand: String, val highCards: List<Int>)

fun evaluateHand(cards: List<Card>): PlayerHand {
    val values = cards.map { it.value }
    val symbols = cards.map { it.symbol }

    val hand = when {
        isRoyalFlush(cards) -> "Royal Flush"
        isStraightFlush(cards) -> "Straight Flush"
        isFourOfAKind(values) -> "Four of a Kind"
        isFullHouse(values) -> "Full House"
        isFlush(symbols) -> "Flush"
        isStraight(values) -> "Straight"
        isThreeOfAKind(values) -> "Three of a Kind"
        isTwoPair(values) -> "Two Pair"
        isPair(values) -> "Pair"
        else -> "High Card"
    }

    val highCards = when (hand) {
        "Royal Flush", "Straight Flush", "Straight" -> values.map { cardValueToRank(it) }.sortedDescending().take(5)
        "Four of a Kind" -> values.groupBy { it }.filter { it.value.size == 4 }.keys.map { cardValueToRank(it) }
        "Full House" -> values.groupBy { it }.filter { it.value.size == 3 || it.value.size == 2 }.keys.map { cardValueToRank(it) }
        "Flush" -> values.map { cardValueToRank(it) }.sortedDescending().take(5)
        "Three of a Kind" -> values.groupBy { it }.filter { it.value.size == 3 }.keys.map { cardValueToRank(it) }
        "Two Pair" -> values.groupBy { it }.filter { it.value.size == 2 }.keys.map { cardValueToRank(it) }
        "Pair" -> values.groupBy { it }.filter { it.value.size == 2 }.keys.map { cardValueToRank(it) }
        else -> values.map { cardValueToRank(it) }.sortedDescending().take(5)
    }

    Log.d("evaluateHand", "Cards: $cards, Hand: $hand, HighCards: $highCards")
    return PlayerHand(name = "", hand = hand, highCards = highCards)
}

fun getWinnerName(players: List<Player>, communityCards: List<Card?>): String {
    val hands = evaluateHands(players, communityCards)
    if (hands.isEmpty()) {
        Log.e("getWinnerName", "No valid hands found. There should always be at least one winner.")
        return "No winner"
    }
    val sortedHands = hands.sortedWith { hand1, hand2 -> compareHands(hand1, hand2) }
    val maxRank = sortedHands.last().let { handToValueRank(it) }
    val topHands = sortedHands.filter { hand -> handToValueRank(hand) == maxRank }

    Log.d("getWinnerName", "Top Hands: $topHands")
    return if (topHands.size > 1) {
        val winners = topHands.map { it.name }
        winners.joinToString(", ")
    } else {
        topHands.first().name
    }
}

fun compareHands(hand1: PlayerHand, hand2: PlayerHand): Int {
    val rank1 = handToValueRank(hand1)
    val rank2 = handToValueRank(hand2)

    return when {
        rank1 > rank2 -> 1
        rank1 < rank2 -> -1
        else -> {
            // Compare the cards that form the hand
            when (hand1.hand) {
                "Pair" -> comparePairs(hand1, hand2)
                "Two Pair" -> compareTwoPairs(hand1, hand2)
                "Three of a Kind" -> compareThreeOfAKind(hand1, hand2)
                "Straight" -> compareStraights(hand1, hand2)
                "Flush" -> compareFlushes(hand1, hand2)
                "Full House" -> compareFullHouses(hand1, hand2)
                "Four of a Kind" -> compareFourOfAKind(hand1, hand2)
                "Straight Flush" -> compareStraights(hand1, hand2)
                "Royal Flush" -> 0 // Royal Flushes are always equal
                else -> compareHighCards(hand1, hand2)
            }
        }
    }
}

fun comparePairs(hand1: PlayerHand, hand2: PlayerHand): Int {
    val pair1 = hand1.highCards.groupBy { it }.filter { it.value.size == 2 }.keys.maxOrNull() ?: 0
    val pair2 = hand2.highCards.groupBy { it }.filter { it.value.size == 2 }.keys.maxOrNull() ?: 0

    if (pair1 != pair2) {
        return pair1.compareTo(pair2)
    }

    // Compare the highest cards from the players' own cards
    val playerCards1 = hand1.highCards.filter { card -> hand1.highCards.count { it == card } == 1 }
    val playerCards2 = hand2.highCards.filter { card -> hand2.highCards.count { it == card } == 1 }

    for (i in playerCards1.indices) {
        if (i >= playerCards2.size) return 1
        val comparison = playerCards1[i].compareTo(playerCards2[i])
        if (comparison != 0) return comparison
    }
    return if (playerCards1.size < playerCards2.size) -1 else 0
}

fun compareTwoPairs(hand1: PlayerHand, hand2: PlayerHand): Int {
    val pairs1 = hand1.highCards.groupBy { it }.filter { it.value.size == 2 }.keys.sortedDescending()
    val pairs2 = hand2.highCards.groupBy { it }.filter { it.value.size == 2 }.keys.sortedDescending()

    for (i in pairs1.indices) {
        val comparison = pairs1[i].compareTo(pairs2.getOrElse(i) { 0 })
        if (comparison != 0) return comparison
    }

    // Compare the highest cards from the players' own cards
    val playerCards1 = hand1.highCards.filter { card -> hand1.highCards.count { it == card } == 1 }
    val playerCards2 = hand2.highCards.filter { card -> hand2.highCards.count { it == card } == 1 }

    for (i in playerCards1.indices) {
        if (i >= playerCards2.size) return 1
        val comparison = playerCards1[i].compareTo(playerCards2[i])
        if (comparison != 0) return comparison
    }
    return if (playerCards1.size < playerCards2.size) -1 else 0
}

fun compareThreeOfAKind(hand1: PlayerHand, hand2: PlayerHand): Int {
    val three1 = hand1.highCards.groupBy { it }.filter { it.value.size == 3 }.keys.maxOrNull() ?: 0
    val three2 = hand2.highCards.groupBy { it }.filter { it.value.size == 3 }.keys.maxOrNull() ?: 0

    if (three1 != three2) {
        return three1.compareTo(three2)
    }

    // Compare the highest cards from the players' own cards
    val playerCards1 = hand1.highCards.filter { card -> hand1.highCards.count { it == card } == 1 }
    val playerCards2 = hand2.highCards.filter { card -> hand2.highCards.count { it == card } == 1 }

    for (i in playerCards1.indices) {
        if (i >= playerCards2.size) return 1
        val comparison = playerCards1[i].compareTo(playerCards2[i])
        if (comparison != 0) return comparison
    }
    return if (playerCards1.size < playerCards2.size) -1 else 0
}

fun compareStraights(hand1: PlayerHand, hand2: PlayerHand): Int {
    val straight1 = hand1.highCards.maxOrNull() ?: 0
    val straight2 = hand2.highCards.maxOrNull() ?: 0

    return straight1.compareTo(straight2)
}

fun compareFlushes(hand1: PlayerHand, hand2: PlayerHand): Int {
    // Compare the highest cards from the players' own cards
    val playerCards1 = hand1.highCards.filter { card -> hand1.highCards.count { it == card } == 1 }
    val playerCards2 = hand2.highCards.filter { card -> hand2.highCards.count { it == card } == 1 }

    for (i in playerCards1.indices) {
        if (i >= playerCards2.size) return 1
        val comparison = playerCards1[i].compareTo(playerCards2[i])
        if (comparison != 0) return comparison
    }
    return if (playerCards1.size < playerCards2.size) -1 else 0
}

fun compareFullHouses(hand1: PlayerHand, hand2: PlayerHand): Int {
    val three1 = hand1.highCards.groupBy { it }.filter { it.value.size == 3 }.keys.maxOrNull() ?: 0
    val three2 = hand2.highCards.groupBy { it }.filter { it.value.size == 3 }.keys.maxOrNull() ?: 0

    if (three1 != three2) {
        return three1.compareTo(three2)
    }

    val pair1 = hand1.highCards.groupBy { it }.filter { it.value.size == 2 }.keys.maxOrNull() ?: 0
    val pair2 = hand2.highCards.groupBy { it }.filter { it.value.size == 2 }.keys.maxOrNull() ?: 0

    return pair1.compareTo(pair2)
}

fun compareFourOfAKind(hand1: PlayerHand, hand2: PlayerHand): Int {
    val four1 = hand1.highCards.groupBy { it }.filter { it.value.size == 4 }.keys.maxOrNull() ?: 0
    val four2 = hand2.highCards.groupBy { it }.filter { it.value.size == 4 }.keys.maxOrNull() ?: 0

    if (four1 != four2) {
        return four1.compareTo(four2)
    }

    // Compare the highest cards from the players' own cards
    val playerCards1 = hand1.highCards.filter { card -> hand1.highCards.count { it == card } == 1 }
    val playerCards2 = hand2.highCards.filter { card -> hand2.highCards.count { it == card } == 1 }

    for (i in playerCards1.indices) {
        if (i >= playerCards2.size) return 1
        val comparison = playerCards1[i].compareTo(playerCards2[i])
        if (comparison != 0) return comparison
    }
    return if (playerCards1.size < playerCards2.size) -1 else 0
}

fun compareHighCards(hand1: PlayerHand, hand2: PlayerHand): Int {
    for (i in hand1.highCards.indices) {
        if (i >= hand2.highCards.size) return 1
        val comparison = hand1.highCards[i].compareTo(hand2.highCards[i])
        if (comparison != 0) return comparison
    }
    return if (hand1.highCards.size < hand2.highCards.size) -1 else 0
}

fun handToValueRank(hand: PlayerHand): Int {
    return when (hand.hand) {
        "High Card" -> 0
        "Pair" -> 1
        "Two Pair" -> 2
        "Three of a Kind" -> 3
        "Straight" -> 4
        "Flush" -> 5
        "Full House" -> 6
        "Four of a Kind" -> 7
        "Straight Flush" -> 8
        "Royal Flush" -> 9
        else -> -1
    }
}

fun evaluateHands(players: List<Player>, communityCards: List<Card?>): List<PlayerHand> {
    return players.map { player ->
        val allCards = listOfNotNull(player.playerCard1, player.playerCard2) + communityCards.filterNotNull()
        val playerHand = evaluateHand(allCards)
        Log.d("evaluateHands", "Player: ${player.playerName}, Hand: ${playerHand.hand}, HighCards: ${playerHand.highCards}")
        playerHand.copy(name = player.playerName)
    }
}

fun isRoyalFlush(cards: List<Card>): Boolean {
    val royalValues = listOf("10", "J", "Q", "K", "A")
    val values = cards.map { it.value }
    val symbols = cards.map { it.symbol }
    return values.containsAll(royalValues) && isFlush(symbols) && isStraight(values)
}

fun isStraightFlush(cards: List<Card>): Boolean {
    if (cards.size < 5) return false

    val sortedCards = cards.sortedBy { cardValueToRank(it.value) }
    for (i in 0..(sortedCards.size - 5)) {
        val subList = sortedCards.subList(i, i + 5)
        if (isStraight(subList.map { it.value }) && isFlush(subList.map { it.symbol })) {
            return true
        }
    }
    return false
}

fun isFlush(symbols: List<Int>): Boolean {
    return symbols.groupBy { it }.any { it.value.size >= 5 }
}

fun isStraight(values: List<String>): Boolean {
    val sortedValues = values.flatMap {
        if (it == "A") listOf(1, 14) else listOf(cardValueToRank(it))
    }.sorted()
    return (0..sortedValues.size - 5).any { i ->
        (0..4).all { j -> sortedValues[i + j] == sortedValues[i] + j }
    }
}

fun isFourOfAKind(values: List<String>): Boolean {
    return values.groupBy { it }.any { it.value.size == 4 }
}

fun isFullHouse(values: List<String>): Boolean {
    val grouped = values.groupBy { it }
    return grouped.any { it.value.size == 3 } && grouped.any { it.value.size == 2 }
}

fun isThreeOfAKind(values: List<String>): Boolean {
    return values.groupBy { it }.any { it.value.size == 3 }
}

fun isTwoPair(values: List<String>): Boolean {
    return values.groupBy { it }.count { it.value.size == 2 } >= 2
}

fun isPair(values: List<String>): Boolean {
    return values.groupBy { it }.any { it.value.size == 2 }
}

fun cardValueToRank(value: String): Int {
    return when (value) {
        "2" -> 2
        "3" -> 3
        "4" -> 4
        "5" -> 5
        "6" -> 6
        "7" -> 7
        "8" -> 8
        "9" -> 9
        "10" -> 10
        "J" -> 11
        "Q" -> 12
        "K" -> 13
        "A" -> 14
        else -> 0
    }
}

fun showInformationPopup(title: String, content: String, context: Context) {
    val inflater = LayoutInflater.from(context)
    val layout = inflater.inflate(R.layout.custom_toast, null)

    val titleTextView = layout.findViewById<TextView>(R.id.toastTitleTextView)
    val contentTextView = layout.findViewById<TextView>(R.id.toastContentTextView)
    titleTextView.text = title
    contentTextView.text = content

    val toast = Toast(context)
    toast.duration = Toast.LENGTH_LONG
    toast.view = layout
    toast.setGravity(Gravity.CENTER, 0, 0)
    toast.show()
}

// Fonction pour sauvegarder la valeur de token
fun SavePlayerChoice(iaSpeed: Int, token: Int, context: Context) {
    val dir = File(context.filesDir, "GameSettings")
    if (!dir.exists()) {
        dir.mkdir()
    }
    val file = File(dir, "settingFile.txt")
    file.writeText("$iaSpeed\n$token")
}

fun ReadUserChoice(context: Context): List<Int> {
    val dir = File(context.filesDir, "GameSettings")
    val file = File(dir, "settingFile.txt")
    if (!file.exists()) {
        SavePlayerChoice(500, 20, context)
        return listOf(500, 20)
    }

    val lines = file.readLines()
    val iaSpeed = if (lines.isNotEmpty()) lines[0].toInt() else 500
    val token = if (lines.size > 1) lines[1].toInt() else 20

    return listOf(iaSpeed, token)
}