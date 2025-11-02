package fr.thomasdomenech.pokerlab.Class

import fr.thomasdomenech.pokerlab.Model.*
import java.io.File
import kotlin.Float
import kotlin.random.Random

data class IA(
    val Q: MutableMap<State, Action> = mutableMapOf(),
    val handHistory: MutableMap<State, Pair<String, Int>> = mutableMapOf(),
    var name: String,
    var initialMoney: Int,
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
    var epsilon_min: Float = 0.05F,
    var aggressivity: Float = 1.0f,   // influence la fréquence de Raise
    var prudence: Float = 1.0f,       // influence la fréquence de Fold/Check
    var bluff: Float = 0.1f           // probabilité de Raise malgré une main faible

) {

    // -------------------------
    // ACTION SELECTION
    // -------------------------
    fun executeActionChoosed(amountToCall: Int, actionName: String, raisePercent: Int, potSize: Int)
    {
        when (actionName)
        {
            ActionString.Fold -> {
                action = ActionString.Fold
            }

            ActionString.Call -> {
                val toCall = amountToCall - bet
                if (toCall <= money) {
                    money -= toCall
                    bet += toCall
                    action = ActionString.Call
                } else {
                    bet += money
                    money = 0
                    action = ActionString.AllIn
                }
            }

            ActionString.Check -> {
                action = ActionString.Check
            }

            ActionString.Raise -> {
                val raiseAmount = (amountToCall - bet) + (potSize * (raisePercent / 100.0)).toInt().coerceAtLeast(1)
                if (raiseAmount <= money) {
                    money -= raiseAmount
                    bet += raiseAmount
                    action = ActionString.Raise
                } else {
                    bet += money
                    money = 0
                    action = ActionString.AllIn
                }
            }
        }
    }
    /* Encienne version de la fonction play
    fun play(state: State, amountToCall: Int, potSize: Int) {
        val possibleActions = getPossibleActions(amountToCall, potSize)
        val qValues = Q.getOrPut(state) { Action() }

        // Exploration ou exploitation
        val doExplore = Random.nextDouble() < epsilon
        val actionName: String = if (doExplore) { // Si doExplore == true on fait une action au hasard pour voire
            // Exploration aléatoire
            possibleActions.random()
        } else { // Sinon : Exploitation de la meilleure action connue
            listOf(ActionString.Fold to qValues.Fold,
                ActionString.Check to qValues.Check,
                ActionString.Call to qValues.Call,
                ActionString.Raise to qValues.Raise)
                .filter { it.first in possibleActions }
                .maxByOrNull { it.second }?.first ?: ActionString.Fold
        }

        // Montant associé
        val raisePercent = if (actionName == ActionString.Raise) {
            qValues.RaisePourcent
        } else 0

        handHistory[state] = actionName to raisePercent

        executeActionChoosed(amountToCall, actionName, raisePercent, potSize)
    }*/

    fun play(state: State, amountToCall: Int, potSize: Int) {
        val possibleActions = getPossibleActions(amountToCall, potSize)
        val qValues = Q.getOrPut(state) { Action() }

        val doExplore = Random.nextDouble() < epsilon
        var actionName: String

        if (doExplore) {
            actionName = possibleActions.random()
        } else {
            // Exploitation pondérée par agressivité et prudence
            val baseAction = listOf(
                ActionString.Fold to qValues.Fold / prudence,
                ActionString.Check to qValues.Check,
                ActionString.Call to qValues.Call,
                ActionString.Raise to qValues.Raise * aggressivity
            )
                .filter { it.first in possibleActions }
                .maxByOrNull { it.second }?.first ?: ActionString.Fold

            actionName = baseAction
        }

        // Petit effet bluff : raise aléatoirement même sans raison
        if (Random.nextDouble() < bluff && ActionString.Raise in possibleActions) {
            actionName = ActionString.Raise
        }

        val raisePercent = if (actionName == ActionString.Raise) qValues.RaisePourcent else 0
        handHistory[state] = actionName to raisePercent
        executeActionChoosed(amountToCall, actionName, raisePercent, potSize)
    }

    fun getPossibleActions(amountToCall: Int, potSize: Int): List<String> {
        val possible = mutableListOf<String>()
        val toCall = (amountToCall - bet).coerceAtLeast(0)

        if (toCall == 0) {
            possible.add(ActionString.Check)
        } else {
            possible.add(ActionString.Fold)
            if (money >= toCall) possible.add(ActionString.Call)
        }

        val minRaise = toCall + 1
        if (money > minRaise) possible.add(ActionString.Raise)

        return possible
    }

    // -------------------------
    // Q-UPDATE (APPRENTISSAGE)
    // -------------------------
    fun learnFromResult() {
        val reward = (money - initialMoney).toDouble()

        for ((state, actionPair) in handHistory) {
            val (actionName, raisePercent) = actionPair
            updateQ(state, actionName, reward, null)
        }

        // On vide l’historique pour la prochaine main
        handHistory.clear()
    }

    fun updateQ(
        state: State,
        actionName: String,
        reward: Double,
        nextState: State?
    ) {
        val current = Q.getOrPut(state) { Action() }

        val oldValue = when (actionName) {
            ActionString.Check -> current.Check
            ActionString.Call -> current.Call
            ActionString.Fold -> current.Fold
            ActionString.Raise -> current.Raise
            else -> 0f
        }

        // Valeur future estimée
        val futureValue = if (nextState != null && Q.containsKey(nextState)) {
            val next = Q[nextState]!!
            maxOf(next.Check, next.Call, next.Fold, next.Raise)
        } else 0f

        // Mise à jour Q-learning
        val newValue = oldValue.toDouble() + alpha * (reward + (gamma * futureValue.toDouble()) - oldValue.toDouble())

        when (actionName) {
            ActionString.Check -> current.Check = newValue
            ActionString.Call -> current.Call = newValue
            ActionString.Fold -> current.Fold = newValue
            ActionString.Raise -> current.Raise = newValue
        }
        if (actionName == ActionString.Raise) {
            // Mise à jour du Q-value
            current.Raise = newValue
            // Ajustement du RaisePourcent selon la récompense reçue
            val oldPercent = current.RaisePourcent
            val updatedPercent = (oldPercent * 0.8 + (reward.coerceIn(0.0, 100.0) * 0.2)).toInt()
            current.RaisePourcent = updatedPercent
        }

        // Décroissance de epsilon
        if (epsilon > epsilon_min) epsilon *= epsilon_decay
    }

    fun initQTable(newQ: Map<State, Action>) {
        Q.clear()
        Q.putAll(newQ)
    }
    // -------------------------
    // STYLE D'ÉVOLUTION SIMPLE
    // -------------------------
    fun evolveBehavior(resultGain: Int) {
        // Adaptation simple selon le résultat de la main
        when {
            resultGain > 0 -> {
                aggressivity = (aggressivity * 1.02f).coerceAtMost(2.0f)
                prudence = (prudence * 0.98f).coerceAtLeast(0.5f)
                bluff = (bluff * 1.05f).coerceAtMost(0.4f)
            }
            resultGain < 0 -> {
                aggressivity = (aggressivity * 0.97f).coerceAtLeast(0.5f)
                prudence = (prudence * 1.03f).coerceAtMost(2.0f)
                bluff = (bluff * 0.95f).coerceAtLeast(0.05f)
            }
        }
    }
}