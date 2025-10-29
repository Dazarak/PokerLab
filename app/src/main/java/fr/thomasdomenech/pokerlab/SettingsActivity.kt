package fr.thomasdomenech.pokerlab

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import fr.thomasdomenech.pokerlab.Tools.SavePlayerChoice
import fr.thomasdomenech.pokerlab.Tools.ReadUserChoice
import androidx.appcompat.app.AlertDialog
import fr.thomasdomenech.pokerlab.Tools.showInformationPopup
import java.io.File

class SettingsActivity : AppCompatActivity()  {
    private var playerStats: PlayerStats? = null
    private var iaSpeed: Int = 0
    private var token: Int = 0
    private lateinit var isThereAITextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val data = ReadUserChoice(this)
        iaSpeed = data[0]
        token = data[1]

        findViewById<TextView>(R.id.tv_Token).text = "Token: $token"

        if (iaSpeed == 1000) {
            findViewById<SeekBar>(R.id.seekBar).progress = 0
        } else if (iaSpeed == 500) {
            findViewById<SeekBar>(R.id.seekBar).progress = 1
        } else {
            findViewById<SeekBar>(R.id.seekBar).progress = 2
        }

        // Initialize the TextView after setContentView
        isThereAITextView = findViewById(R.id.tv_isThereAI)

        playerStats = PlayerStats.readPlayerStatsFromFile(applicationContext.filesDir.path + "/player_stats.json")

        if (isThereSomeAIOutThere()) {
            isThereAITextView.text = "There is AI here !"
        } else {
            isThereAITextView.text = "No AI found here !"
        }

        val resetAIButton = findViewById<Button>(R.id.bt_resetIa)
        resetAIButton.setOnClickListener {
            showResetConfirmationDialog(true)
        }


        val backButton = findViewById<ImageView>(R.id.ib_exit)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Update the TextViews with playerStats data
        updatePlayerStatsViews()
        val resetButton = findViewById<Button>(R.id.bt_resetIa2)

        resetButton.setOnClickListener {
            showResetConfirmationDialog(false)
        }

        val ADDButton = findViewById<Button>(R.id.bt_PUB)
        ADDButton.setOnClickListener {
            try {
                token = token + 100
                findViewById<TextView>(R.id.tv_Token).text = "Token: $token"
                SavePlayerChoice(iaSpeed, token, this)
            } catch (e: Exception) {
                showInformationPopup("Erreur", "Une erreur est survenu !", this)
            }
        }

        // SeekBar listener to save IA speed
        val speedSeekBar = findViewById<SeekBar>(R.id.seekBar)
        speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                iaSpeed = when (progress) {
                    0 -> 1000 // Slow
                    1 -> 500 // Medium
                    else -> 0 // Fast
                }
                SavePlayerChoice(iaSpeed, token, this@SettingsActivity)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun showResetConfirmationDialog(isForAI: Boolean) {
        // Créer un inflater pour obtenir la vue du layout personnalisé
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.popup_confirmation, null)

        // Créer un AlertDialog en utilisant le layout personnalisé
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Popup non annulable en dehors
            .create()

        // Récupérer les boutons de la popup
        val buttonYes = dialogView.findViewById<Button>(R.id.buttonYes)
        val buttonNo = dialogView.findViewById<Button>(R.id.buttonNo)

        // Configurer le comportement du bouton "Yes"
        buttonYes.setOnClickListener {
            if (isForAI) {
                resetAIData() // Réinitialiser les données de l'IA
                if (isThereSomeAIOutThere()) {
                    isThereAITextView.text = "There is AI here !"
                } else {
                    isThereAITextView.text = "No AI found here !"
                }
            } else {
                resetPlayerStats() // Réinitialiser les statistiques du joueur
            }
            dialog.dismiss() // Fermer la popup
        }

        // Configurer le comportement du bouton "No"
        buttonNo.setOnClickListener {
            dialog.dismiss() // Fermer la popup sans action
        }

        // Afficher la popup
        dialog.show()
    }

    private fun resetAIData() {
        val file1 = File(applicationContext.filesDir.path + "/best_ia.json")
        val file2 = File(applicationContext.filesDir.path + "/second_ia.json")
        val file3 = File(applicationContext.filesDir.path + "/third_ia.json")

        var allDeleted = true
        var NothingToDelete = true

        if (file1.exists()) {
            NothingToDelete = false
            file1.delete()
        }
        if (file2.exists()) {
            NothingToDelete = false
            file2.delete()
        }
        if (file3.exists()) {
            NothingToDelete = false
            file3.delete()
        }

        if (allDeleted && !NothingToDelete) {
            Toast.makeText(this, "AI data reset", Toast.LENGTH_SHORT).show()
        } else if (NothingToDelete) {
            Toast.makeText(this, "Nothing to reset", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "An error occurred while resetting the AI files", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetPlayerStats() {
        // Créer un objet PlayerStats vide
        playerStats = PlayerStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

        // Écrire cet objet vide dans le fichier JSON
        PlayerStats.writePlayerStatsToFile(applicationContext.filesDir.path + "/player_stats.json", playerStats!!)

        // Mettre à jour les vues avec les données vides
        updatePlayerStatsViews()

        // Afficher un message de confirmation
        Toast.makeText(this, "Player stats have been reset!", Toast.LENGTH_SHORT).show()
    }


    private fun updatePlayerStatsViews() {
        playerStats?.let {
            findViewById<TextView>(R.id.tv_totalpartiplayed).text = "Played game: ${it.playedGame}"
            findViewById<TextView>(R.id.tv_gameWin).text = "Winned game: ${it.winnedGame}"
            findViewById<TextView>(R.id.tv_totalhandplayed).text = "Played hand: ${it.playedHand}"
            findViewById<TextView>(R.id.tv_winnedHand).text = "Winned hand: ${it.winnedHand}"
            findViewById<TextView>(R.id.tv_moneywin).text = "Earned money: ${it.earnedMoney}€"
            findViewById<TextView>(R.id.tv_biggestWin).text = "Biggest win: ${it.biggestWin}€"
            findViewById<TextView>(R.id.tv_moneylost).text = "Lost money: ${it.lostMoney}€"

            findViewById<TextView>(R.id.tv_profiPourcentage).text = "Profit percentage: ${it.getProfitPourcentage()}%"

            findViewById<TextView>(R.id.tv_aggressiveness).text = "Aggressiveness: ${it.getAggressiveness()}%"
            findViewById<TextView>(R.id.tv_prudence).text = "Prudence: ${it.getPrudence()}%"
            findViewById<TextView>(R.id.tv_passivity).text = "Passivity: ${it.getPassivity()}%"



            findViewById<TextView>(R.id.tv_Allincount).text = "All in : ${it.allIn}"
            findViewById<TextView>(R.id.tv_RaiseCount).text = "Raise : ${it.raise}"
            findViewById<TextView>(R.id.tv_CallCount).text = "Call : ${it.call}"
            findViewById<TextView>(R.id.tv_CheckCount).text = "Check : ${it.check}"
            findViewById<TextView>(R.id.tv_FoldCount).text = "Fold : ${it.fold}"
        }
    }



    fun isThereSomeAIOutThere(): Boolean {
        val file1 = File(applicationContext.filesDir.path + "/best_ia.json")
        val file2 = File(applicationContext.filesDir.path + "/second_ia.json")
        val file3 = File(applicationContext.filesDir.path + "/third_ia.json")

        return file1.exists() || file2.exists() || file3.exists()
    }
}