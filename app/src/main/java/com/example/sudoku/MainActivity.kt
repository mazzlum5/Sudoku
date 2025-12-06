package com.example.sudoku

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.widget.Button
import android.widget.Chronometer
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var sudokuGame: SudokuGame
    private lateinit var sudokuGrid: GridLayout
    private lateinit var cells: Array<Array<EditText?>>
    private lateinit var newGameButton: Button
    private lateinit var checkSolutionButton: Button
    private lateinit var messageTextView: TextView
    private lateinit var bestTimeTextView: TextView
    private lateinit var timer: Chronometer
    private var timerShouldStart = false
    private lateinit var difficulty: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sudokuGrid = findViewById(R.id.sudokuGrid)
        newGameButton = findViewById(R.id.newGameButton)
        checkSolutionButton = findViewById(R.id.checkSolutionButton)
        messageTextView = findViewById(R.id.messageTextView)
        bestTimeTextView = findViewById(R.id.bestTimeTextView)
        timer = findViewById(R.id.timer)

        difficulty = intent.getStringExtra("DIFFICULTY") ?: "Medium"

        sudokuGame = SudokuGame()
        setupSudokuGrid()
        startNewGame(difficulty)

        newGameButton.setOnClickListener {
            finish() // Go back to the StartMenuActivity
        }

        checkSolutionButton.setOnClickListener {
            if (sudokuGame.checkSolution()) {
                timer.stop()
                val elapsedMillis = SystemClock.elapsedRealtime() - timer.base
                val elapsedSeconds = elapsedMillis / 1000

                messageTextView.text = "Congratulations! You solved it in ${formatTime(elapsedSeconds)}"
                messageTextView.setTextColor(Color.GREEN)

                updateBestTime(elapsedSeconds)
            } else {
                messageTextView.text = "Keep trying!"
                messageTextView.setTextColor(Color.RED)
            }
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
        cells = Array(size) { arrayOfNulls<EditText>(size) }
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
                    editText.setBackgroundColor(Color.parseColor("#333333"))
                } else {
                    editText.setBackgroundColor(Color.parseColor("#222222"))
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
        messageTextView.text = ""
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
                    cell?.setText("")
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
                 messageTextView.text = ""
            }
        })
    }
    
    private fun loadBestTime() {
        val sharedPref = getSharedPreferences("SudokuPrefs", Context.MODE_PRIVATE)
        val bestTime = sharedPref.getLong("bestTime_$difficulty", -1)
        if (bestTime != -1L) {
            bestTimeTextView.text = "Best: ${formatTime(bestTime)}"
        } else {
            bestTimeTextView.text = "Best: --:--"
        }
    }

    private fun updateBestTime(newTime: Long) {
        val sharedPref = getSharedPreferences("SudokuPrefs", Context.MODE_PRIVATE)
        val bestTime = sharedPref.getLong("bestTime_$difficulty", -1)

        if (bestTime == -1L || newTime < bestTime) {
            with(sharedPref.edit()) {
                putLong("bestTime_$difficulty", newTime)
                apply()
            }
            loadBestTime()
        }
    }

    private fun formatTime(timeInSeconds: Long): String {
        val minutes = timeInSeconds / 60
        val seconds = timeInSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
