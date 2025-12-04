package com.example.sudoku

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StartMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_menu)

        val easyButton = findViewById<Button>(R.id.easyButton)
        val mediumButton = findViewById<Button>(R.id.mediumButton)
        val hardButton = findViewById<Button>(R.id.hardButton)
        val expertButton = findViewById<Button>(R.id.expertButton)

        easyButton.setOnClickListener {
            startGame("Easy")
        }

        mediumButton.setOnClickListener {
            startGame("Medium")
        }

        hardButton.setOnClickListener {
            startGame("Hard")
        }

        expertButton.setOnClickListener {
            startGame("Expert")
        }
    }

    private fun startGame(difficulty: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("DIFFICULTY", difficulty)
        startActivity(intent)
    }
}
