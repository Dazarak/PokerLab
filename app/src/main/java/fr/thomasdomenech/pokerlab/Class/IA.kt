package fr.thomasdomenech.pokerlab.Class

import com.google.gson.Gson
import fr.thomasdomenech.pokerlab.Tools.cardValueToRank
import fr.thomasdomenech.pokerlab.Tools.evaluateHand
import java.io.File
import kotlin.Float
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class State(
    val hand: String,
    val stack: String,
    val pot: String,
    val phase: String,
    val position: Double
)

data class IA(
    val Q: MutableMap<State, MutableMap<Int, Double>> = mutableMapOf(),
    var name: String,
    var money: Int,
    var bet: Int = 0,
    var action: String = "",
    var turn: Int = 0,
    var card1: Card? = null,
    var card2: Card? = null,
    var alpha: Float = 0.1F,
    var gamma: Float = 0.9F,
    var epsilon: Float = 0.9F,
    var epsilon_decay: Float = 0.9995F,
    var epsilon_min: Float = 0.05F
) {

    fun getQ(state: State, action: Int): Double {
        val stateMap = Q.getOrPut(state) {
            ACTIONS.associateWith { Random.nextDouble(-0.1, 0.1) }.toMutableMap()
        }
        return stateMap.getOrDefault(action, 0.0)
    }

    fun setQ(state: State, action: Int, value: Double) {
        val stateMap = Q.getOrPut(state) { mutableMapOf() }
        stateMap[action] = value
    }

    fun getPossibleActions(amountToCall: Int, potSize: Int): List<Int> {
        val possible = mutableSetOf(0) // 0 = Coucher
        if (money >= amountToCall) possible.add(1) // Suivre / Checker
        if (money > amountToCall + potSize) possible.add(2) // Relancer
        if (money > 0) possible.add(3) // Tapis
        return possible.toList()
    }

    fun chooseAction(state: State, amountToCall: Int, potSize: Int): Pair<Int, Int> {
        val possibleActions = getPossibleActions(amountToCall, potSize)
        if (possibleActions.isEmpty()) return 0 to 0

        val action = if (Random.nextDouble() < epsilon) {
            possibleActions.random()
        } else {
            possibleActions.maxByOrNull { getQ(state, it) } ?: 0
        }

        val amount = decideInvestmentAmount(action, amountToCall, potSize)
        return action to amount
    }

    fun decideInvestmentAmount(action: Int, amountToCall: Int, potSize: Int): Int {
        return when (action) {
            0 -> 0
            1 -> amountToCall
            2 -> {
                val minRaise = amountToCall * 2
                val maxRaise = money
                min(maxRaise, max(minRaise, (potSize * Random.nextDouble(0.5, 1.0)).toInt()))
            }
            3 -> money
            else -> 0
        }
    }

    fun updateQ(state: State, action: Int, reward: Double, nextState: State) {
        val oldQ = getQ(state, action)
        val maxNextQ = ACTIONS.maxOf { getQ(nextState, it) }
        val newQ = oldQ + alpha * (reward + gamma * maxNextQ - oldQ)
        setQ(state, action, newQ)
        if (epsilon > epsilon_min) epsilon *= epsilon_decay
    }

    fun clone(): IA {
        val newQ = Q.mapValues { (_, inner) -> inner.toMutableMap() }.toMutableMap()
        return IA(
            Q = newQ,
            name = name,
            money = money,
            bet = bet,
            action = action,
            turn = turn,
            card1 = card1?.copy(),
            card2 = card2?.copy(),
            alpha = alpha,
            gamma = gamma,
            epsilon = epsilon,
            epsilon_decay = epsilon_decay,
            epsilon_min = epsilon_min
        )
    }

    fun save(filePath: String) {
        try {
            val gson = Gson()
            File(filePath).writeText(gson.toJson(this))
        } catch (e: Exception) {
            println("Erreur lors de la sauvegarde de l'IA : ${e.message}")
        }
    }

    companion object {
        val ACTIONS = listOf(0, 1, 2, 3)

        fun load(filePath: String): IA? {
            return try {
                val file = File(filePath)
                if (!file.exists()) return null
                val gson = Gson()
                gson.fromJson(file.readText(), IA::class.java)
            } catch (e: Exception) {
                println("Erreur lors du chargement de l'IA : ${e.message}")
                null
            }
        }
    }
}