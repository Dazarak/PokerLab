package fr.thomasdomenech.pokerlab.Model

data class State(
    var hand: List<Int>,
    var commonCard: List<Int>,
    var moneyPercent: Int,
    var potPercent: Int,
    var phase: Int,
    var position: Int
) {
    // Constructeur secondaire pour créer à partir de cartes et valeurs brutes
    constructor(
        _hand: List<Card>,
        _commonCard: List<Card>,
        currentMoney: Int,
        maxMoney: Int,   // stack max ou initial
        currentPot: Int,
        maxPot: Int,     // pot max possible
        _phase: Int,
        pos: Int,
        nbPlayer: Int
    ) : this(
        hand = abstractCards(_hand),
        commonCard = abstractCards(_commonCard),
        moneyPercent = ((currentMoney.toDouble() / maxMoney.toDouble()) * 100).toInt(),
        potPercent = ((currentPot.toDouble() / maxPot.toDouble()) * 100).toInt(),
        phase = _phase,
        position = abstractPosition(pos, nbPlayer)
    )

    companion object {
        fun abstractCards(cards: List<Card>): List<Int> {
            return cards
                .map { card ->
                    when (card.value) {
                        "2", "3", "4", "5" -> 0
                        "6", "7", "8", "9" -> 1
                        else -> 2
                    }
                }
                .sorted()
        }
        fun abstractPosition(position: Int, nbPlayer: Int): Int {
            val fraction = position.toDouble() / nbPlayer
            return when {
                fraction < 1.0 / 3 -> 0   // Early / début
                fraction < 2.0 / 3 -> 1   // Middle / milieu
                else -> 2                 // Late / fin
            }
        }
    }
}
