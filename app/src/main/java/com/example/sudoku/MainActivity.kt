package com.example.sudoku

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.widget.Button
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

        val difficulty = intent.getStringExtra("DIFFICULTY") ?: "Medium"

        sudokuGame = SudokuGame()
        setupSudokuGrid()
        startNewGame(difficulty)

        newGameButton.setOnClickListener {
            finish() // Go back to the StartMenuActivity
        }

        checkSolutionButton.setOnClickListener {
            if (sudokuGame.checkSolution()) {
                messageTextView.text = "Congratulations! You solved it!"
                messageTextView.setTextColor(Color.GREEN)
            } else {
                messageTextView.text = "Keep trying!"
                messageTextView.setTextColor(Color.RED)
            }
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

                if ((row / 3 + col / 3) % 2 == 0) {
                    editText.setBackgroundColor(Color.parseColor("#E8E8E8"))
                } else {
                    editText.setBackgroundColor(Color.WHITE)
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

        for (row in 0 until 9) {
            for (col in 0 until 9) {
                val cell = cells[row][col]
                val number = board[row][col]
                if (number != 0) {
                    cell?.setText(number.toString())
                    cell?.isEnabled = false
                    cell?.setTextColor(Color.BLACK)
                } else {
                    cell?.setText("")
                    cell?.isEnabled = true
                }
            }
        }
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
                    editText.setTextColor(Color.BLUE)
                } else {
                    editText.setTextColor(Color.RED)
                }
                 messageTextView.text = ""
            }
        })
    }
}
