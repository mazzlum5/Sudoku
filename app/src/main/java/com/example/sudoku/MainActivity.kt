package com.example.sudoku

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.Chronometer
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var sudokuGame: SudokuGame
    private lateinit var sudokuGrid: GridLayout
    private lateinit var cells: Array<Array<EditText?>>
    private lateinit var newGameButton: Button
    private lateinit var hintButton: Button
    private lateinit var checkSolutionButton: Button
    private lateinit var messageTextView: TextView
    private lateinit var bestTimeTextView: TextView
    private lateinit var timer: Chronometer
    private var timerShouldStart = false
    private lateinit var difficulty: String
    private var rewardedAd: RewardedAd? = null
    private val adUnitId = "ca-app-pub-3940256099942544/5224354917" // Test Ad ID

    companion object {
        private const val TAG = "MainActivity"
        const val EXTRA_DIFFICULTY = "DIFFICULTY"
        private const val DEFAULT_DIFFICULTY = "Medium"
        private const val PREFS_NAME = "SudokuPrefs"
        private const val PREFS_KEY_BEST_TIME_PREFIX = "bestTime_"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        MobileAds.initialize(this) {}
        loadRewardedAd()

        sudokuGrid = findViewById(R.id.sudokuGrid)
        newGameButton = findViewById(R.id.newGameButton)
        hintButton = findViewById(R.id.hintButton)
        checkSolutionButton = findViewById(R.id.checkSolutionButton)
        messageTextView = findViewById(R.id.messageTextView)
        bestTimeTextView = findViewById(R.id.bestTimeTextView)
        timer = findViewById(R.id.timer)

        difficulty = intent.getStringExtra(EXTRA_DIFFICULTY) ?: DEFAULT_DIFFICULTY

        sudokuGame = SudokuGame()
        setupSudokuGrid()
        startNewGame(difficulty)

        newGameButton.setOnClickListener {
            finish() // Go back to the StartMenuActivity
        }

        hintButton.setOnClickListener {
            showRewardedAd()
        }

        checkSolutionButton.setOnClickListener {
            if (sudokuGame.checkSolution()) {
                timer.stop()
                val elapsedMillis = SystemClock.elapsedRealtime() - timer.base
                val elapsedSeconds = elapsedMillis / 1000

                messageTextView.text = getString(R.string.congratulations_message, formatTime(elapsedSeconds))
                messageTextView.setTextColor(Color.GREEN)

                updateBestTime(elapsedSeconds)
            } else {
                messageTextView.text = getString(R.string.keep_trying)
                messageTextView.setTextColor(Color.RED)
            }
        }
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.toString())
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Ad was loaded.")
                rewardedAd = ad
            }
        })
    }

    private fun showRewardedAd() {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad was dismissed.")
                    // Load the next ad
                    loadRewardedAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Ad failed to show.")
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
                    // Called when ad is dismissed.
                    rewardedAd = null
                }
            }

            rewardedAd?.show(this) {
                Log.d(TAG, "User earned the reward.")
                val hint = sudokuGame.getHint()
                if (hint != null) {
                    val (row, col, number) = hint
                    val cell = cells[row][col]
                    cell?.setText(number.toString())
                }
            }
        } else {
            Log.d(TAG, "The rewarded ad wasn\'t ready yet.")
            Toast.makeText(this, getString(R.string.hint_not_available), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && timerShouldStart) {
            timer.start()
            timerShouldStart = false
        }
    }

    private fun setupSudokuGrid() {
        val size = 9
        cells = Array(size) { arrayOfNulls(size) }
        val displayMetrics = resources.displayMetrics
        val cellSideInDp = 40
        val cellSideInPixels = (cellSideInDp * displayMetrics.density).toInt()

        val thickMargin = (2 * displayMetrics.density).toInt()
        val thinMargin = (1 * displayMetrics.density).toInt()

        for (row in 0 until size) {
            for (col in 0 until size) {
                val editText = EditText(this)
                val params = GridLayout.LayoutParams(
                    GridLayout.spec(row),
                    GridLayout.spec(col)
                )
                params.width = cellSideInPixels
                params.height = cellSideInPixels

                params.topMargin = if (row % 3 == 0) thickMargin else thinMargin
                params.leftMargin = if (col % 3 == 0) thickMargin else thinMargin

                if (row == 8) params.bottomMargin = thickMargin
                if (col == 8) params.rightMargin = thickMargin

                editText.layoutParams = params
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                editText.filters = arrayOf(InputFilter.LengthFilter(1))
                editText.gravity = Gravity.CENTER
                editText.setTextColor(Color.WHITE)

                if ((row / 3 + col / 3) % 2 == 0) {
                    editText.setBackgroundColor("#333333".toColorInt())
                } else {
                    editText.setBackgroundColor("#222222".toColorInt())
                }

                sudokuGrid.addView(editText)
                cells[row][col] = editText
                addTextWatcher(editText, row, col)
            }
        }
    }

    private fun startNewGame(difficulty: String) {
        sudokuGame.generateNewGame(difficulty)
        val board = sudokuGame.getBoard()
        messageTextView.text = null
        loadBestTime()

        for (row in 0 until 9) {
            for (col in 0 until 9) {
                val cell = cells[row][col]
                val number = board[row][col]
                if (number != 0) {
                    cell?.setText(number.toString())
                    cell?.isEnabled = false
                    cell?.setTextColor(Color.LTGRAY) // Lighter color for pre-filled numbers
                } else {
                    cell?.text?.clear()
                    cell?.isEnabled = true
                }
            }
        }

        timer.base = SystemClock.elapsedRealtime()
        timerShouldStart = true
    }

    private fun addTextWatcher(editText: EditText, row: Int, col: Int) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                val number = if (text.isNotEmpty()) text.toInt() else 0
                val currentBoard = sudokuGame.getBoard()
                currentBoard[row][col] = number

                if (sudokuGame.isMoveValid(currentBoard, row, col, number)) {
                    editText.setTextColor(Color.CYAN)
                } else {
                    editText.setTextColor(Color.RED)
                }
                messageTextView.text = null
            }
        })
    }

    private fun loadBestTime() {
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val bestTime = sharedPref.getLong("$PREFS_KEY_BEST_TIME_PREFIX$difficulty", -1)
        if (bestTime != -1L) {
            bestTimeTextView.text = getString(R.string.best_time, formatTime(bestTime))
        } else {
            bestTimeTextView.text = getString(R.string.best_time_default)
        }
    }

    private fun updateBestTime(newTime: Long) {
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val bestTime = sharedPref.getLong("$PREFS_KEY_BEST_TIME_PREFIX$difficulty", -1)

        if (bestTime == -1L || newTime < bestTime) {
            sharedPref.edit {
                putLong("$PREFS_KEY_BEST_TIME_PREFIX$difficulty", newTime)
            }
            loadBestTime()
        }
    }

    private fun formatTime(timeInSeconds: Long): String {
        val minutes = timeInSeconds / 60
        val seconds = timeInSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
