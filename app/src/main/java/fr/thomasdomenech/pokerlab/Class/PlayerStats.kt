package fr.thomasdomenech.pokerlab

import com.google.gson.Gson
import java.io.File

data class PlayerStats(
    var playedGame: Int,
    var winnedGame: Int,
    var playedHand: Int,
    var winnedHand: Int,
    var earnedMoney: Int,
    var lostMoney: Int,
    var biggestWin: Int,
    var allIn: Int,
    var raise: Int,
    var call: Int,
    var check: Int,
    var fold: Int
) {
    fun getAggressiveness(): String {
        val offensiveActions = (raise + allIn).toDouble()
        val defensiveActions = (call + check + fold).toDouble()
        val result = if (defensiveActions == 0.0) offensiveActions else offensiveActions / defensiveActions
        return (result*100).toString().substringBefore(".") + "." + result.toString().substringAfter(".").take(2)
    }

    fun getPrudence(): String {
        val totalActions = (raise + allIn + call + check + fold).toDouble()
        val result = if (totalActions == 0.0) 0.0 else (fold / totalActions)
        return (result*100).toString().substringBefore(".") + "." + result.toString().substringAfter(".").take(2)
    }

    fun getPassivity(): String {
        val totalActions = (raise + allIn + call + check + fold).toDouble()
        val result = if (totalActions == 0.0) 0.0 else ((call + check) / totalActions)
        return (result*100).toString().substringBefore(".") + "." + result.toString().substringAfter(".").take(2)
    }

    fun getProfitPourcentage(): String {
        val result = if (lostMoney > 0) {
            (earnedMoney - lostMoney).toDouble() / lostMoney * 100
        } else {
            if (earnedMoney > 0) 100.0 else 0.0
        }
        return result.toString().substringBefore(".") + "." + result.toString().substringAfter(".").take(2)
    }

    companion object {
        fun writePlayerStatsToFile(filePath: String, playerStats: PlayerStats) {
            val gson = Gson()
            val jsonString = gson.toJson(playerStats)
            val file = File(filePath)
            file.writeText(jsonString)
        }

        fun readPlayerStatsFromFile(filePath: String): PlayerStats? {
            val file = File(filePath)
            return if (file.exists()) {
                val jsonString = file.readText()
                val gson = Gson()
                gson.fromJson(jsonString, PlayerStats::class.java)
            } else {
                null
            }
        }
    }
}