package fr.thomasdomenech.pokerlab

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.ImageButton
import android.widget.TextView
import fr.thomasdomenech.pokerlab.Tools.ReadUserChoice
import fr.thomasdomenech.pokerlab.Tools.SavePlayerChoice
import fr.thomasdomenech.pokerlab.Tools.showInformationPopup

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var iaSpeed: Int = 0
    private var token: Int = 0
    private var isIncrementing = false

    override fun onResume() {
        val data = ReadUserChoice(this)
        iaSpeed = data[0]
        token = data[1]
        findViewById<TextView>(R.id.tv_TokenMain).text = "Token: $token"
        super.onResume()
    }

    override fun onRestart() {
        val data = ReadUserChoice(this)
        iaSpeed = data[0]
        token = data[1]
        findViewById<TextView>(R.id.tv_TokenMain).text = "Token: $token"
        super.onRestart()
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val smallBlindEditText = findViewById<EditText>(R.id.smallBlind)
        val bigBlindEditText = findViewById<EditText>(R.id.bigBlind)
        val startingAmountEditText = findViewById<EditText>(R.id.startingAmount)
        val numIaEditText = findViewById<EditText>(R.id.numIa)

        val smallBlindSeekBar = findViewById<SeekBar>(R.id.smallBlindSeekBar)
        val startingAmountSeekBar = findViewById<SeekBar>(R.id.startingAmountSeekBar)
        val numIaSeekBar = findViewById<SeekBar>(R.id.numIaSeekBar)
        val startGameButton = findViewById<Button>(R.id.startGameButton)
        val startAIGameButton = findViewById<Button>(R.id.startAIGameButton)

        val data = ReadUserChoice(this)
        iaSpeed = data[0]
        token = data[1]

        findViewById<TextView>(R.id.tv_TokenMain).text = "Token: $token"

        // Ajoutez ce code dans la méthode `onCreate`
        val settingsButton = findViewById<ImageButton>(R.id.imageButton)
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        bigBlindEditText.setText(2.toString())

        smallBlindEditText.addTextChangedListener(object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                val smallBlind = s.toString().toIntOrNull() ?: 0
                bigBlindEditText.setText((smallBlind * 2).toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        startingAmountEditText.addTextChangedListener(object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                val startingAmount = s.toString().toIntOrNull() ?: 0
                if (startingAmount < 50)
                    startingAmountEditText.setText("50")
                if (startingAmount > 10000)
                    startingAmountEditText.setText("10000")
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        smallBlindSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener(smallBlindEditText, ::incrementSmallBlind, ::decrementSmallBlind))
        startingAmountSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener(startingAmountEditText, ::incrementStartingAmount, ::decrementStartingAmount))
        numIaSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener(numIaEditText, ::incrementNumIa, ::decrementNumIa))

        startGameButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("smallBlindEditText", smallBlindEditText.text.toString())
            intent.putExtra("bigBlindEditText", bigBlindEditText.text.toString())
            intent.putExtra("startingAmountEditText", startingAmountEditText.text.toString())
            intent.putExtra("numIaEditText", numIaEditText.text.toString())
            intent.putExtra("isPlayerPlaying", true) // Player is playing
            startActivity(intent)
        }

        startAIGameButton.setOnClickListener {
            // Vérifier si l'utilisateur a des token ?
            if (token < 10) {
                showInformationPopup("Sorry", "You need token to start an AI training game !", this)
            }else {
                token = token - 10
                showInformationPopup("Token", "-10 token", this)
                SavePlayerChoice(iaSpeed, token, this)
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("smallBlindEditText", smallBlindEditText.text.toString())
                intent.putExtra("bigBlindEditText", bigBlindEditText.text.toString())
                intent.putExtra("startingAmountEditText", startingAmountEditText.text.toString())
                intent.putExtra("numIaEditText", numIaEditText.text.toString())
                intent.putExtra("isPlayerPlaying", false) // Player is not playing
                startActivity(intent)
            }
        }
    }

    private fun createSeekBarChangeListener(editText: EditText, incrementFunction: () -> Unit, decrementFunction: () -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Log.d("logSeekBar", "Progress is $progress")
                modifie(incrementFunction, decrementFunction, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // No action needed here
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.progress = 1 // Reset to middle position
                stopIncrementingOrDecrementing()
            }
        }
    }

    private fun modifie(incrementFunction: () -> Unit, decrementFunction: () -> Unit, progress: Int) {
        handler.removeCallbacksAndMessages(null) // Clear any previous callbacks
        if (progress > 1) {
            isIncrementing = true
            handler.post(object : Runnable {
                override fun run() {
                    if (isIncrementing) {
                        incrementFunction()
                        handler.postDelayed(this, 200)
                    }
                }
            })
        } else if (progress < 1) {
            isIncrementing = false
            handler.post(object : Runnable {
                override fun run() {
                    if (!isIncrementing) {
                        decrementFunction()
                        handler.postDelayed(this, 200)
                    }
                }
            })
        }
    }

    private fun stopIncrementingOrDecrementing() {
        isIncrementing = false
        handler.removeCallbacksAndMessages(null)
    }

    @SuppressLint("SetTextI18n")
    private fun incrementSmallBlind() {
        val smallBlindEditText = findViewById<EditText>(R.id.smallBlind)
        val currentValue = smallBlindEditText.text.toString().toIntOrNull() ?: 0
        smallBlindEditText.setText((currentValue + 1).toString())
    }

    @SuppressLint("SetTextI18n")
    private fun decrementSmallBlind() {
        val smallBlindEditText = findViewById<EditText>(R.id.smallBlind)
        val currentValue = smallBlindEditText.text.toString().toIntOrNull() ?: 0
        if (currentValue > 1)
            smallBlindEditText.setText((currentValue - 1).toString())
    }

    @SuppressLint("SetTextI18n")
    private fun incrementStartingAmount() {
        val startingAmountEditText = findViewById<EditText>(R.id.startingAmount)
        val currentValue = startingAmountEditText.text.toString().toIntOrNull() ?: 0
        startingAmountEditText.setText((currentValue + 50).toString())
    }

    @SuppressLint("SetTextI18n")
    private fun decrementStartingAmount() {
        val startingAmountEditText = findViewById<EditText>(R.id.startingAmount)
        val currentValue = startingAmountEditText.text.toString().toIntOrNull() ?: 0
        if (currentValue > 50)
            startingAmountEditText.setText((currentValue - 50).toString())
    }

    @SuppressLint("SetTextI18n")
    private fun incrementNumIa() {
        val numIaEditText = findViewById<EditText>(R.id.numIa)
        val currentValue = numIaEditText.text.toString().toIntOrNull() ?: 0
        if (currentValue < 5)
            numIaEditText.setText((currentValue + 1).toString())
    }

    @SuppressLint("SetTextI18n")
    private fun decrementNumIa() {
        val numIaEditText = findViewById<EditText>(R.id.numIa)
        val currentValue = numIaEditText.text.toString().toIntOrNull() ?: 0
        if (currentValue > 1)
            numIaEditText.setText((currentValue - 1).toString())
    }
}