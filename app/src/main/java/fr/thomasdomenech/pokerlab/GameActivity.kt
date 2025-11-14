package fr.thomasdomenech.pokerlab

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import fr.thomasdomenech.pokerlab.Tools.showInformationPopup
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import fr.thomasdomenech.pokerlab.Model.*
import fr.thomasdomenech.pokerlab.Class.*
import fr.thomasdomenech.pokerlab.Model.ActionString
import fr.thomasdomenech.pokerlab.Tools.*
import java.io.File
import kotlin.math.floor
import androidx.core.view.isInvisible

class GameActivity : AppCompatActivity() {
    //  ------------------------ Variables joueurs ------------------------
    private var user: User? = null
    private val iaList = mutableListOf<IA>()
    private var playerTurn: Int? = 1
    private var playerStats: PlayerStats? = null
    // ------------------------ Variables jeux ------------------------
    private var deck: Deck? = null
    private var communityCards: List<Card?> = emptyList()
    private var raiseText: TextView? = null
    private var seekBar: SeekBar? = null
    private var exitButton: ImageView? = null
    private var round: Int? = 0
    private var pot: Int? = 0
    private var maxMoney: Int = 0
    private var partyNumber: Int = 1
    private var partyAlreadyStart: Boolean = true
    private var smallBlind: Int = 0
    private var bigBlind: Int = 0
    private var firstActionPlayed: Boolean = false
    private val iaVictoryCounts = mutableMapOf<String, Int>()
    private val iaLossesCounts = mutableMapOf<String, Int>()
    private var iaSpeed: Int = 500
    private var totalBet: Int = 0
    private var playerPlayed: Boolean = false
    private var ListDeNom = listOf("Elise", "Antoine", "Catherine", "Philippe", "Marie", "Julie", "Jade", "Pierre", "Paul", "Jacques", "Jean", "Luc", "Lucas", "Lucie", "Lea", "Leo", "Lola", "Leon", "Lena", "Lilou", "Lina", "Louna", "Louane", "Louise")

    private fun updatePlayerStats() { // Met a jour les stat du joueur
        if (playerStats != null) {
            PlayerStats.writePlayerStatsToFile(applicationContext.filesDir.path + "/player_stats.json", playerStats!!)
        }
    }

    // Calculate the win rate for each AI
    private fun calculateWinRate(ia: IA): Double {
        val wins = iaVictoryCounts.getOrDefault(ia.name, 0)
        val losses = iaLossesCounts.getOrDefault(ia.name, 0)
        val totalGames = wins + losses
        val winRate = if (totalGames == 0) 0.0 else wins.toDouble() / totalGames

        val gain = if (totalGames == 0) 0.0 else ia.money - maxMoney

        return (winRate * gain.toDouble()) // Combine win rate and gain rate
    }

    fun nextGame() { // Fonction de transition entre deux partie
        partyNumber++ // On incrémente le nombre de main jouer de 1
        if (partyNumber % 10 == 0) { // Si dix parties ont été jouer on double les mises de départ
            smallBlind *= 2
            bigBlind *= 2
        }
        findViewById<TextView>(R.id.tv_partyNumber).text = "$partyNumber" // On pet a jour l'affichage
        playerPlayed = false // On remet a zero la variable pour qu'à la prochaine partie le jeu sache que personne n'a encore jouer

        if (playerStats != null) {
            playerStats?.playedHand = playerStats?.playedHand?.plus(1) ?: 0
            updatePlayerStats()
        }

        setActionButtonsVisibility(
            mapOf(
                R.id.bt_Check to View.GONE,
                R.id.bt_Call to View.GONE,
                R.id.bt_Fold to View.GONE,
                R.id.bt_Raise to View.GONE,
                R.id.sb_Raise to View.GONE,
                R.id.tx_Raise to View.GONE
            )
        )
        val iaItems = listOf(
            findViewById<View>(R.id.iaItem1),
            findViewById<View>(R.id.iaItem2),
            findViewById<View>(R.id.iaItem3),
            findViewById<View>(R.id.iaItem4),
            findViewById<View>(R.id.iaItem5)
        )

        // On créé une list des joueurs
        var playerList: List<Player> = mutableListOf(Player(user!!.name, user!!.card1!!, user!!.card2!!))
        for (ia in iaList) {
            playerList = playerList + Player(ia.name, ia.card1!!, ia.card2!!)
        }
        // On créé une liste du niveau des main de chaque joueur
        var playerHands = evaluateHands(playerList, communityCards)
        // On affiche les hauteur de main des AI
        for (pH in playerHands) {
            for (ia in iaItems) {
                if (pH.name == ia.findViewById<TextView>(R.id.iaName).text) {
                    ia.findViewById<TextView>(R.id.iaHand).text = pH.hand
                }
            }
        }

        // Evaluate hands of all players not folded
        val allPlayersNotFolded = mutableListOf<Player>()
        if (user!!.action != ActionString.Fold && user!!.action != "Lost") {
            allPlayersNotFolded.add(Player(user!!.name, user!!.card1!!, user!!.card2!!))
        }
        for (ia in iaList) {
            if (ia.action != ActionString.Fold) {
                allPlayersNotFolded.add(Player(ia.name, ia.card1!!, ia.card2!!))
            }
        }

        // Donner l'argent aux gagnants
        val result = getWinnerName(allPlayersNotFolded, communityCards) // TODO : La lsite des joueur ne contient pas le score des mains
        val winnerList: List<String> = result.split(", ")
        for (winner in winnerList) {
            // Donner l'argent aux gagnants
            if (winner == user!!.name) {
                user!!.money += floor(pot!!.toDouble() / winnerList.size).toInt()
                playerStats?.earnedMoney = playerStats?.earnedMoney?.plus(floor(pot!!.toDouble() / winnerList.size).toInt()) ?: 0
                playerStats?.winnedHand = playerStats?.winnedHand?.plus(1) ?: 0
                if (playerStats?.biggestWin ?: 0 < floor(pot!!.toDouble() / winnerList.size).toInt()) {
                    playerStats?.biggestWin = floor(pot!!.toDouble() / winnerList.size).toInt()
                }
            } else {
                for (ia in iaList) {
                    if (winner == ia.name) {
                        iaVictoryCounts[ia.name] = iaVictoryCounts.getOrDefault(ia.name, 0) + 1 // Increment the victory count
                        ia.money += floor(pot!!.toDouble() / winnerList.size).toInt()
                    } else {
                        iaLossesCounts[ia.name] = iaLossesCounts.getOrDefault(ia.name, 0) + 1 // Increment the loss count
                    }
                }
            }
        }
        if (playerStats != null) {
            if (!winnerList.contains(user!!.name) && user!!.action != "Lost") {
                playerStats?.lostMoney = playerStats?.lostMoney?.plus(totalBet) ?: 0
                totalBet = 0
            }
            updatePlayerStats()
        }

        // Modifier les Qtable de chaque IA
        for (ia in iaList)
        {
            // TODO : Utiliser allPlayers pour determiner si les ia aurait du se coucher ou non !
            ia.learnFromResult()
            ia.evolveBehavior(ia.money - ia.initialMoney)
        }
        // Save common QTable
        val mergedQ = mergeQTables(iaList) { ia -> calculateWinRate(ia) }
        saveQTable(mergedQ, applicationContext.filesDir.path + "/merged_qtable.json")

        // Reset inital money of the IA to the next hand
        for (ia in iaList)
        {
            ia.initialMoney = ia.money
        }
        // show winner(s)
        if (result.length == 1)
            showInformationPopup("Winner", result, this@GameActivity)
        else
            showInformationPopup("Winners", result, this@GameActivity)
        // reset pot
        pot = 0
        round = 0
        // reset user and ia
        user!!.bet = 0
        for (ia in iaList) {
            ia.bet = 0
        }
        user!!.action = ""
        for (ia in iaList) {
            if (ia.money == 0) {
                ia.action = "Deletable"
            }
        }
        // Reset le tour du joueur principal
        playerTurn = 1
        // Réinitialise le statut de jeu du joueur
        playerPlayed = false
        // Rotate player and IA turns
        user!!.turn += 1
        if (user!!.turn == iaList.size + 2) {
            user!!.turn = 1
        }
        for (ia in iaList) {
            ia.turn += 1
            if (ia.turn == iaList.size + 2) {
                ia.turn = 1
            }
        }
        // supprimer les ia deletable de la vue
        val iterator = iaList.iterator()
        while (iterator.hasNext()) {
            val ia = iterator.next()
            if (ia.action == "Deletable") {
                iterator.remove()
            }
        }
        for (ia in iaList) {
            ia.action = ""
        }

        // Caché les Zone d'affichage d'IA si il n'y a plus d'IA a afficher dedans
        for (i in 0 until 5) {
            if (i >= iaList.size) {
                findViewById<View>(R.id.iaItem1 + i).visibility = View.INVISIBLE
            }
        }
        // Trier les IA par ordre croissant de la valeur turn
        iaList.sortBy { it.turn }
        // Créé une liste de Player pour remettre les joueurs dans l'ordre
        playerList = mutableListOf(Player(user!!.name, user!!.card1!!, user!!.card2!!, user!!.turn))
        for (ia in iaList) {
            playerList = playerList + Player(ia.name, ia.card1!!, ia.card2!!, ia.turn)
        }
        // Trier les joueurs par ordre croissant de la valeur turn
        playerList = playerList.sortedBy { it.turn }
        var i = 1
        for (player in playerList) {
            player.turn = i
            i++
        }
        // Remettre les valeurs des joueurs et des ia dans l'ordre
        for (player in playerList){
            if (player.playerName == user!!.name) {
                user!!.turn = player.turn
            } else {
                for (ia in iaList) {
                    if (ia.name == player.playerName) {
                        ia.turn = player.turn
                    }
                }
            }
        }
        // reset user and ia cards
        deck = Deck()
        // reset user and ia cards
        val playerCards = GivePlayerCard(deck!!)
        user!!.card1 = playerCards[0]
        user!!.card2 = playerCards[1]
        for (ia in iaList) {
            val iaCards = GivePlayerCard(deck!!)
            ia.card1 = iaCards[0]
            ia.card2 = iaCards[1]
        }
        updateUserView()
        updateIaViews(showAIAction = false, showAICards = false)
        // update pot
        findViewById<TextView>(R.id.tx_Pot).text = "Pot: $pot €"
        // redistribute community cards
        communityCards = distributeCommunityCards(deck!!)
        updateCommunityCards("None")
    }
    fun playGame() {
        val handler = Handler(Looper.getMainLooper())
        val gameRunnable = object : Runnable {
            override fun run() {
                if (!partyAlreadyStart) {
                    finish()
                    return
                }
                updateUserView()
                if (round == -2) {
                    nextGame()
                    if (user!!.money <= 0) { // Fin de partie Joueur
                        user!!.action = "Lost"
                        user!!.card1 = Card("", 0)
                        user!!.card2 = Card("",0)
                        updateUserView()
                        if (iaList.size <= 1) {
                            if (playerStats != null) {
                                showInformationPopup("Sorry", "You lost the game !", this@GameActivity)
                            }
                            updatePlayerStats()
                            // Attendre 4 secondes avant de revenir a la page mainactivity
                            handler.postDelayed({
                                finish()
                            }, 4000)
                            finish()
                            return
                        }
                        handler.postDelayed(this, 1000)
                    }else if (iaList.size <= 0) {
                        // Afficher un message toast pour informer le joueur qu'il a gagné et revenir a la page mainactivity
                        showInformationPopup("Congratulation", "You won the game !", this@GameActivity)
                        playerStats?.winnedGame= playerStats?.winnedGame?.plus(1) ?: 0
                        updatePlayerStats()
                        // Attendre 4 secondes avant de revenir a la page mainactivity
                        handler.postDelayed({
                            finish()
                        }, 4000)
                        finish()
                        return
                    } else {
                        handler.postDelayed(this, 1000)
                    }
                } else if (round == -1) {
                    updateUserView()
                    val iaItems = listOf(
                        findViewById<View>(R.id.iaItem1),
                        findViewById<View>(R.id.iaItem2),
                        findViewById<View>(R.id.iaItem3),
                        findViewById<View>(R.id.iaItem4),
                        findViewById<View>(R.id.iaItem5)
                    )
                    var playerList: List<Player> = mutableListOf(Player(user!!.name, user!!.card1!!, user!!.card2!!))
                    for (ia in iaList) {
                        playerList = playerList + Player(ia.name, ia.card1!!, ia.card2!!)
                    }
                    var playerHands = evaluateHands(playerList, communityCards)
                    for (pH in playerHands) {
                        for (ia in iaItems) {
                            if (pH.name == ia.findViewById<TextView>(R.id.iaName).text) {
                                ia.findViewById<TextView>(R.id.iaHand).text = pH.hand
                            }
                        }
                    }
                    // Get the player's hand
                    val playerHand = evaluateHand(listOfNotNull(user!!.card1, user!!.card2) + communityCards.filterNotNull())
                    // Update the TextView with the player's hand
                    if (user!!.money <= 0 && user!!.action == "Lost") {
                        findViewById<TextView>(R.id.tv_playerHand).text = ""
                        findViewById<LinearLayout>(R.id.ll_PlayerData).visibility = View.GONE
                        findViewById<TextView>(R.id.tx_playerBet).visibility = View.GONE
                    } else {
                        findViewById<LinearLayout>(R.id.ll_PlayerData).visibility = View.VISIBLE
                        findViewById<TextView>(R.id.tx_playerBet).visibility = View.VISIBLE
                        findViewById<TextView>(R.id.tv_playerHand).visibility = View.VISIBLE
                        findViewById<TextView>(R.id.tv_playerHand).text = "${playerHand.hand}"
                    }
                    updateIaViews(showAIAction = true, showAICards = true)
                    round = -2
                    handler.postDelayed(this, 3000)
                } else {
                    if (shouldEndRound(user!!, iaList)) {
                        pot = pot?.plus(user!!.bet)
                        for (ia in iaList) {
                            if (ia.action != ActionString.Fold) {
                                if (ia.money == 0) {
                                    ia.action = ActionString.AllIn
                                } else {
                                    ia.action = ""
                                }
                            }
                            pot = pot?.plus(ia.bet)
                        }
                        user!!.bet = 0
                        for (ia in iaList) {
                            ia.bet = 0
                        }
                        round = round?.plus(1)
                        playerPlayed = false
                        findViewById<TextView>(R.id.tx_Pot).setText("Pot: $pot €")
                        updateIaViews(showAIAction = false, showAICards = false)
                        updateUserView()
                    }
                    if (round == 0) {
                        findViewById<TextView>(R.id.tv_playerHand).visibility = View.GONE
                        if (firstActionPlayed == false) {
                            for (ia in iaList) {
                                if (ia.turn == 1) {
                                    if (ia.money >= smallBlind) {
                                        ia.bet = smallBlind
                                        ia.money -= smallBlind
                                    } else {
                                        ia.bet = ia.money
                                        ia.money = 0
                                    }
                                }
                                if (ia.turn == 2) {
                                    if (ia.money >= bigBlind) {
                                        ia.bet = bigBlind
                                        ia.money -= bigBlind
                                    } else {
                                        ia.bet = ia.money
                                        ia.money = 0
                                    }
                                }
                            }
                            if (user!!.turn == 1) {
                                if (user!!.money >= smallBlind) {
                                    user!!.bet = smallBlind
                                    totalBet += smallBlind
                                    user!!.money -= smallBlind
                                } else {
                                    user!!.bet = user!!.money
                                    totalBet += user!!.money
                                    user!!.money = 0
                                }
                            } else if (user!!.turn == 2) {
                                if (user!!.money >= bigBlind) {
                                    user!!.bet = bigBlind
                                    totalBet += bigBlind
                                    user!!.money -= bigBlind
                                } else {
                                    user!!.bet = user!!.money
                                    totalBet += user!!.money
                                    user!!.money = 0
                                }
                            }
                            firstActionPlayed = true
                            updateIaViews(showAIAction = false, showAICards = false)
                            updateUserView()
                            playerTurn = 3
                        }
                        updateCommunityCards("None")
                    } else if (round == 1) {
                        updateCommunityCards("River")
                    } else if (round == 2) {
                        updateCommunityCards("Turn")
                    } else if (round == 3) {
                        updateCommunityCards("Flop")
                    } else if (round == 4) {
                        round = -1
                        firstActionPlayed = false
                    }
                    if (user!!.turn == playerTurn) {
                        if (user!!.money >= 0) {
                            if (user!!.action == ActionString.Fold) {
                                continueGame()
                            } else if (shouldPlayerPlay()) {
                                if (round != -1)
                                    playerTurn()
                            } else {
                                continueGame()
                            }
                        } else {
                            continueGame()
                        }
                    } else {
                        for (ia in iaList) {
                            if (ia.turn == playerTurn) {
                                if (ia.money > 0) {
                                    if (ia.action != ActionString.Fold) {
                                        if (iaShouldPlay(ia)) {
                                            if (round != -1) {
                                                var biggest_bet = iaList.maxByOrNull { it.bet }?.bet ?: 0

                                                if (biggest_bet < user!!.bet) {
                                                    biggest_bet = user!!.bet
                                                }

                                                // Cartes visibles pour l'IA
                                                var visibleCards = mutableListOf<Card>()
                                                for (i in 0 until round!!) {
                                                    visibleCards.add(communityCards[i]!!)
                                                }
                                                // Etat de la partie qui va determiner l'action
                                                val Etat = State(
                                                    _hand = listOf(ia.card1!!, ia.card2!!), // main de l'IA
                                                    _commonCard = visibleCards,             // cartes visibles
                                                    currentMoney = ia.money,                // argent actuel
                                                    maxMoney = maxMoney,                    // stack max ou initial
                                                    currentPot = pot!!,                     // pot actuel
                                                    maxPot = (maxMoney * iaList.size),      // pot max possible
                                                    _phase = round!!,                       // phase actuelle ("preflop", "flop", etc.)
                                                    pos = ia.turn,                          // position à la table
                                                    nbPlayer = (iaList.size + 1)            // Nombre de joueur
                                                )

                                                // Ajout de miseEnCours au pot
                                                ia.play(Etat, biggest_bet, bigBlind)

                                                // Mise à jour de l'affichage
                                                updateIaViews(showAIAction = true, showAICards = false)
                                            }
                                        }
                                    }
                                } else if (ia.money <= 0 && ia.bet != 0 && ia.action == "") {
                                    ia.action = "..."
                                }
                            }
                        }
                        playerTurn = playerTurn?.plus(1)
                        if (playerTurn!! >= iaList.size + 2) {
                            playerTurn = 1
                        }
                    }
                    handler.postDelayed(this, iaSpeed.toLong())
                }
            }
        }
        // Start the game loop
        handler.post(gameRunnable)
    }

    fun iaShouldPlay(ia: IA)
    : Boolean {

        // Vérifier si l'ia a déjà miser la biggest_bet
        var biggest_bet = iaList.maxByOrNull { it.bet }?.bet ?: 0
        if (biggest_bet < user!!.bet) {
            biggest_bet = user!!.bet
        }
        if ((biggest_bet <= ia.bet) && (biggest_bet != 0) && (ia.action != "")) {
            return false
        }
        if (iaList.all { it.action == ActionString.Fold || it.name == ia.name || it.action == ActionString.AllIn } && (user!!.action == ActionString.Fold || user!!.action == "Lost") && ia.bet >= biggest_bet) {
            ia.action = "..."
            return false
        }

        return true
    }

    fun shouldPlayerPlay(): Boolean {
        // Vérifier si le joueur a déjà miser la biggest_bet
        var biggest_bet = iaList.maxByOrNull { it.bet }?.bet ?: 0
        if (biggest_bet <= user!!.bet && biggest_bet != 0 && user!!.action != "") {
            return false
        }

        // Vérifier si toute les autres IA sont couché ou all-in ou pas d'argent
        if (iaList.all { it.action == ActionString.Fold || it.action == ActionString.AllIn || it.money <= 0 } && user!!.bet >= biggest_bet) {
            return false
        }

        return true
    }

    @SuppressLint("SetTextI18n")
    fun playerTurn() {
        if (user!!.money <= 0) {
            continueGame()
        } else if (iaList.all { it.bet == user!!.bet }) {
            setActionButtonsVisibility(
                mapOf(
                    R.id.bt_Check to View.VISIBLE,
                    R.id.bt_Call to View.GONE,
                    R.id.bt_Fold to View.VISIBLE,
                    R.id.bt_Raise to View.VISIBLE,
                    R.id.sb_Raise to View.VISIBLE,
                    R.id.tx_Raise to View.VISIBLE
                )
            )
            // mettre la valeur max de la seekbar a l'argent du joueur
            seekBar?.max = user!!.money
            seekBar?.min = 1
        } else {
            setActionButtonsVisibility(
                mapOf(
                    R.id.bt_Check to View.GONE,
                    R.id.bt_Call to View.VISIBLE,
                    R.id.bt_Fold to View.VISIBLE,
                    R.id.bt_Raise to View.VISIBLE,
                    R.id.sb_Raise to View.VISIBLE,
                    R.id.tx_Raise to View.VISIBLE
                )
            )
            findViewById<Button>(R.id.bt_Call).text = "Call ${iaList.maxByOrNull { it.bet }!!.bet - user!!.bet} €"
            if (findViewById<Button>(R.id.bt_Call).text == "Call 0 €") {
                findViewById<Button>(R.id.bt_Call).text = "Check"
            }
            // mettre la valeur max de la seekbar a l'argent du joueur
            seekBar?.max = user!!.money
            if (user!!.money + user!!.bet > iaList.maxByOrNull { it.bet }!!.bet) {
                seekBar?.min = iaList.maxByOrNull { it.bet }!!.bet
            } else {
                seekBar?.min = user!!.money
            }
        }

        findViewById<Button>(R.id.bt_Check).setOnClickListener {
            user!!.action = ActionString.Check
            playerStats?.check = playerStats?.check?.plus(1) ?: 0
            updatePlayerStats()
            continueGame()
        }

        findViewById<Button>(R.id.bt_Call).setOnClickListener {
            user!!.action = ActionString.Call
            val biggest_bet = iaList.maxByOrNull { it.bet }!!.bet
            if (user!!.bet > 0) {
                if (biggest_bet > user!!.money) {
                    user!!.bet += user!!.money
                    totalBet += user!!.money
                    user!!.money = 0
                    playerStats?.allIn = playerStats?.allIn?.plus(1) ?: 0
                } else {
                    val oldBet = user!!.bet
                    user!!.bet += biggest_bet - user!!.bet
                    totalBet += (biggest_bet - oldBet)
                    user!!.money -= (biggest_bet - oldBet)
                    playerStats?.call = playerStats?.call?.plus(1) ?: 0
                }
            }else {
                if ( iaList.maxByOrNull { it.bet }!!.bet > user!!.money) {
                    user!!.bet = user!!.money
                    totalBet += user!!.money
                    user!!.money -= user!!.bet
                    playerStats?.allIn = playerStats?.allIn?.plus(1) ?: 0
                } else {
                    user!!.bet = iaList.maxByOrNull { it.bet }!!.bet
                    totalBet += iaList.maxByOrNull { it.bet }!!.bet
                    user!!.money -= user!!.bet
                    playerStats?.call = playerStats?.call?.plus(1) ?: 0
                }
            }
            updatePlayerStats()
            continueGame()
        }

        findViewById<Button>(R.id.bt_Fold).setOnClickListener {
            user!!.action = ActionString.Fold
            playerStats?.fold = playerStats?.fold?.plus(1) ?: 0
            continueGame()
        }

        findViewById<Button>(R.id.bt_Raise).setOnClickListener {
            user!!.action = ActionString.Raise
            if (user!!.bet > 0) {
                user!!.bet += seekBar?.progress ?: 0
                totalBet += seekBar?.progress ?: 0
                user!!.money -= seekBar?.progress ?: 0
                if (user!!.money <= 0) {
                    playerStats?.allIn = playerStats?.allIn?.plus(1) ?: 0
                } else {
                    playerStats?.raise = playerStats?.raise?.plus(1) ?: 0
                }
            } else {
                user!!.bet = seekBar?.progress ?: 0
                totalBet += user!!.bet
                user!!.money -= user!!.bet
                if (user!!.money <= 0) {
                    playerStats?.allIn = playerStats?.allIn?.plus(1) ?: 0
                } else {
                    playerStats?.raise = playerStats?.raise?.plus(1) ?: 0
                }
            }
            updatePlayerStats()
            continueGame()
        }
    }

    fun continueGame() {
        // Hide action buttons
        setActionButtonsVisibility(
            mapOf(
                R.id.bt_Check to View.GONE,
                R.id.bt_Call to View.GONE,
                R.id.bt_Fold to View.GONE,
                R.id.bt_Raise to View.GONE,
                R.id.sb_Raise to View.GONE,
                R.id.tx_Raise to View.GONE
            )
        )

        playerTurn = playerTurn?.plus(1)
        if (playerTurn!! >= iaList.size + 2) {
            playerTurn = 1
        }
        playerPlayed = true
        updateUserView()
    }

    fun shouldEndRound(user: User, iaList: List<IA>): Boolean {
        var iaAllPlayed = true

        var maxBet = iaList.maxByOrNull { it.bet }?.bet ?: 0
        if (user.bet > maxBet) {
            maxBet = user.bet
        }

        for (ia in iaList) {
          if (ia.action == "") {
              iaAllPlayed = false
          }
        }

        // Condition 1: All players have the same bet or are folded or can't bet much
        var shouldEndBecauseOfSameBetOrFoldedOrNoMonney = true
        for (ia in iaList) {
            if (ia.bet != maxBet && ia.action != ActionString.Fold && ia.money > 0) {
                shouldEndBecauseOfSameBetOrFoldedOrNoMonney = false
            }
        }
        if (user.bet != maxBet && user.action != ActionString.Fold && user.money > 0) {
            shouldEndBecauseOfSameBetOrFoldedOrNoMonney = false
        }

        return (shouldEndBecauseOfSameBetOrFoldedOrNoMonney) && (playerPlayed && iaAllPlayed)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Retrieve the value of iaSpeed from the file
        val dir = File(applicationContext.filesDir, "GameSettings")
        val speedFile = File(dir, "iaSpeed.txt")
        iaSpeed = if (speedFile.exists()) {
            speedFile.readText().toInt()
        } else {
            500
        }

        val statsFilePath = applicationContext.filesDir.path + "/player_stats.json"
        val file = File(statsFilePath)

        // Vérifie si le fichier existe, sinon crée-le avec des valeurs par défaut
        if (!file.exists()) {
            Log.d("GameActivity", "Fichier de stats manquant, création d'un nouveau fichier.")
            val defaultStats = PlayerStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
            PlayerStats.writePlayerStatsToFile(statsFilePath, defaultStats)
        }

        raiseText = findViewById(R.id.tx_Raise)
        seekBar = findViewById(R.id.sb_Raise)
        exitButton = findViewById(R.id.ib_exit)

        // Initialize the deck
        deck = Deck()

        // Retrieve the values from the intent
        smallBlind = intent.getStringExtra("smallBlindEditText")!!.toInt()
        bigBlind = intent.getStringExtra("bigBlindEditText")!!.toInt()
        val startingAmount = intent.getStringExtra("startingAmountEditText")
        val numIa = intent.getStringExtra("numIaEditText")?.toIntOrNull() ?: 0
        maxMoney = startingAmount?.toIntOrNull() ?: 0

        // Set IA visibility based on the number of IA chosen
        setIaVisibility(numIa)

        // Update the player's cards and create the user
        val playerCards = GivePlayerCard(deck!!)
        user = User().apply {
            name = "You"
            money = startingAmount?.toIntOrNull() ?: 0
            turn = 1
            card1 = playerCards[0]
            card2 = playerCards[1]
        }
        updateUserView()

        seekBar?.max = user?.money ?: 0

        // Create AI and distribute cards
        ListDeNom = ListDeNom.shuffled()
        val QTable = loadQTable(applicationContext.filesDir.path + "/merged_qtable.json")
        for (i in 0 until numIa) {
            val iaCards = GivePlayerCard(deck!!)
            val newIa = IA(name = ListDeNom[i], initialMoney = startingAmount?.toIntOrNull() ?: 0, money = startingAmount?.toIntOrNull() ?: 0, bet = 0, action = "", turn = i + 2, card1 = iaCards[0], card2 = iaCards[1])

            newIa.initQTable(QTable)
            iaList.add(newIa)
        }
        updateIaViews(showAIAction = false, showAICards = false)

        // Initialize the victory counts for each AI
        for (ia in iaList) {
            iaVictoryCounts[ia.name] = 0
            iaLossesCounts[ia.name] = 0
        }

        // distribute community cards
        communityCards = distributeCommunityCards(deck!!)
        updateCommunityCards("None")

        // Set the party number
        findViewById<TextView>(R.id.tv_partyNumber).setText("$partyNumber")


        playerStats = PlayerStats.readPlayerStatsFromFile(applicationContext.filesDir.path + "/player_stats.json")
        playerStats?.playedGame = playerStats?.playedGame?.plus(1) ?: 0
        updatePlayerStats()


        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Handle the progress change
                raiseText?.text = "$progress €"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Handle the start of the touch event
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Handle the end of the touch event
            }
        })

        exitButton?.setOnClickListener(View.OnClickListener {
            partyAlreadyStart = false
            finish()
        })

        setActionButtonsVisibility(
            mapOf(
                R.id.bt_Check to View.GONE,
                R.id.bt_Call to View.GONE,
                R.id.bt_Fold to View.GONE,
                R.id.bt_Raise to View.GONE,
                R.id.sb_Raise to View.GONE,
                R.id.tx_Raise to View.GONE
            )
        )

        playGame()
    }

    @SuppressLint("SetTextI18n")
    private fun updateUserView() {
        val playerCard1View = findViewById<View>(R.id.playerCard1)
        val playerCard2View = findViewById<View>(R.id.playerCard2)

        playerCard1View.findViewById<TextView>(R.id.cardValue).text = user?.card1?.value
        playerCard1View.findViewById<ImageView>(R.id.cardSymbol).setImageResource(user?.card1?.symbol ?: 0)

        playerCard2View.findViewById<TextView>(R.id.cardValue).text = user?.card2?.value
        playerCard2View.findViewById<ImageView>(R.id.cardSymbol).setImageResource(user?.card2?.symbol ?: 0)

        findViewById<TextView>(R.id.PlayerMonney).text = "${user?.money} €"
        findViewById<TextView>(R.id.tx_playerBet).text = "Bet: ${user?.bet} €"
        findViewById<TextView>(R.id.tx_playerPos).text = "Pos: ${user?.turn}"
    }

    private fun setIaVisibility(numIa: Int) {
        val iaItems = listOf(
            findViewById<View>(R.id.iaItem1),
            findViewById<View>(R.id.iaItem2),
            findViewById<View>(R.id.iaItem3),
            findViewById<View>(R.id.iaItem4),
            findViewById<View>(R.id.iaItem5)
        )

        for (i in iaItems.indices) {
            iaItems[i].visibility = if (i < numIa) View.VISIBLE else View.INVISIBLE
        }
    }

    @SuppressLint("CutPasteId")
    private fun updateIaViews(showAIAction: Boolean, showAICards: Boolean) {
        val iaItems = listOf(
            findViewById<View>(R.id.iaItem1),
            findViewById<View>(R.id.iaItem2),
            findViewById<View>(R.id.iaItem3),
            findViewById<View>(R.id.iaItem4),
            findViewById<View>(R.id.iaItem5)
        )

        for (i in iaList.indices) {
            val ia = iaList[i]
            val iaItem = iaItems[i]
            if (iaItem.isInvisible) {
                continue
            }else {
                iaItem.findViewById<TextView>(R.id.IAposition).text = ia.turn.toString()
                iaItem.findViewById<TextView>(R.id.iaName).text = ia.name
                iaItem.findViewById<TextView>(R.id.iaMoney).text = "Money: ${ia.money} €"

                if (showAICards) {
                    val aiCard1View = iaItem.findViewById<View>(R.id.AiCard1)
                    val aiCard2View = iaItem.findViewById<View>(R.id.AiCard2)

                    aiCard1View.findViewById<TextView>(R.id.cardValue).text = ia.card1?.value
                    aiCard1View.findViewById<ImageView>(R.id.cardSymbol).setImageResource(ia.card1?.symbol ?: 0)

                    aiCard2View.findViewById<TextView>(R.id.cardValue).text = ia.card2?.value
                    aiCard2View.findViewById<ImageView>(R.id.cardSymbol).setImageResource(ia.card2?.symbol ?: 0)

                    aiCard1View.visibility = View.VISIBLE
                    aiCard2View.visibility = View.VISIBLE
                    iaItem.findViewById<View>(R.id.iaHand).visibility = View.VISIBLE
                } else {
                    iaItem.findViewById<View>(R.id.AiCard1).visibility = View.GONE
                    iaItem.findViewById<View>(R.id.AiCard2).visibility = View.GONE
                    iaItem.findViewById<View>(R.id.iaHand).visibility = View.GONE
                }

                if (showAIAction) {
                    iaItem.findViewById<View>(R.id.iaAction).visibility = View.VISIBLE
                    iaItem.findViewById<TextView>(R.id.iaAction).text = "${ia.action}"
                } else {
                    iaItem.findViewById<View>(R.id.iaAction).visibility = View.INVISIBLE
                }
                iaItem.findViewById<TextView>(R.id.iaBet).text = "Bet: ${ia.bet} €"
            }
        }
        // Cacher les Zone d'affichage d'IA si il n'y a plus d'IA à afficher dedans
        for (i in 0 until 5) {
            if (i >= iaList.size) {
                iaItems[i].visibility = View.INVISIBLE
                iaItems[i].findViewById<View>(R.id.AiCard1).visibility = View.GONE
                iaItems[i].findViewById<View>(R.id.AiCard2).visibility = View.GONE
                iaItems[i].findViewById<View>(R.id.iaHand).visibility = View.GONE
            }
        }
    }

    private fun updateCommunityCards(turn: String) {
        val communityCardViews = listOf(
            findViewById<View>(R.id.communityCard1),
            findViewById<View>(R.id.communityCard2),
            findViewById<View>(R.id.communityCard3),
            findViewById<View>(R.id.communityCard4),
            findViewById<View>(R.id.communityCard5)
        )

        when (turn) {
            "None" -> {
                for (i in 0 until 5) {
                    communityCardViews[i].visibility = View.INVISIBLE
                }
            }
            "River" -> {
                for (i in 0 until 3) {
                    val card = communityCards[i]
                    communityCardViews[i].findViewById<TextView>(R.id.cardValue).text = card?.value
                    communityCardViews[i].findViewById<ImageView>(R.id.cardSymbol).setImageResource(card?.symbol ?: 0)
                    communityCardViews[i].visibility = View.VISIBLE
                }
                communityCardViews[3].visibility = View.INVISIBLE
                communityCardViews[4].visibility = View.INVISIBLE
            }
            "Turn" -> {
                for (i in 0 until 4) {
                    val card = communityCards[i]
                    communityCardViews[i].findViewById<TextView>(R.id.cardValue).text = card?.value
                    communityCardViews[i].findViewById<ImageView>(R.id.cardSymbol).setImageResource(card?.symbol ?: 0)
                    communityCardViews[i].visibility = View.VISIBLE
                }
                communityCardViews[4].visibility = View.INVISIBLE
            }
            "Flop" -> {
                for (i in 0 until 5) {
                    val card = communityCards[i]
                    communityCardViews[i].findViewById<TextView>(R.id.cardValue).text = card?.value
                    communityCardViews[i].findViewById<ImageView>(R.id.cardSymbol).setImageResource(card?.symbol ?: 0)
                    communityCardViews[i].visibility = View.VISIBLE
                }
            }
        }
    }

    fun GivePlayerCard(deck: Deck): List<Card?> {
        return listOf(deck.drawCard(), deck.drawCard())
    }

    fun distributeCommunityCards(deck: Deck): List<Card?> {
        return listOf(deck.drawCard(), deck.drawCard(), deck.drawCard(), deck.drawCard(), deck.drawCard())
    }

    fun setActionButtonsVisibility(buttonsVisibility: Map<Int, Int>) {
        for ((buttonId, visibility) in buttonsVisibility) {
            findViewById<View>(buttonId)?.visibility = visibility
        }
    }
    override fun finish() {
        partyAlreadyStart = false
        super.finish()
    }
    override fun onStop() {
        super.onStop()
        partyAlreadyStart = false
        // stoppe les threads/timers ici si besoin
        if (!isFinishing) finish()
    }
    override fun onDestroy() {
        super.onDestroy()
        partyAlreadyStart = false
        // nettoyage mémoire ici
    }
    override fun onBackPressed() {
        super.onBackPressed()
        partyAlreadyStart = false
        finish()
    }
}

