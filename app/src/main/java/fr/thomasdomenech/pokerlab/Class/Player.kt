package fr.thomasdomenech.pokerlab.Class

data class Player(
    val playerName: String = "",
    var playerCard1: Card? = null,
    var playerCard2: Card? = null,
    var turn: Int = 0
)
