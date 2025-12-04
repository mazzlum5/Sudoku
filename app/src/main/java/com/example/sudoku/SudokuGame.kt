package com.example.sudoku

import kotlin.random.Random

class SudokuGame {

    private var board: Array<IntArray> = Array(9) { IntArray(9) }
    private var solution: Array<IntArray> = Array(9) { IntArray(9) }

    fun getBoard(): Array<IntArray> {
        return board
    }

    fun generateNewGame(difficulty: String) {
        // Start with an empty board
        board = Array(9) { IntArray(9) }
        solution = Array(9) { IntArray(9) }

        // Generate a full solution
        solve(board)
        for (i in 0..8) {
            solution[i] = board[i].clone()
        }

        // Determine the number of cells to remove based on difficulty
        val cellsToRemove = when (difficulty) {
            "Easy" -> 35
            "Medium" -> 45
            "Hard" -> 50
            "Expert" -> 55
            else -> 45 // Default to Medium
        }

        var cellsRemoved = 0
        while (cellsRemoved < cellsToRemove) {
            val row = Random.nextInt(9)
            val col = Random.nextInt(9)

            if (board[row][col] != 0) {
                board[row][col] = 0
                cellsRemoved++
            }
        }
    }

    fun solve(board: Array<IntArray>): Boolean {
        for (row in 0..8) {
            for (col in 0..8) {
                if (board[row][col] == 0) {
                    val numbers = (1..9).shuffled()
                    for (num in numbers) {
                        if (isValid(board, row, col, num)) {
                            board[row][col] = num
                            if (solve(board)) {
                                return true
                            } else {
                                board[row][col] = 0 // backtrack
                            }
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    private fun isValid(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        // Check row
        for (c in 0..8) {
            if (board[row][c] == num) {
                return false
            }
        }

        // Check column
        for (r in 0..8) {
            if (board[r][col] == num) {
                return false
            }
        }

        // Check 3x3 subgrid
        val startRow = row - row % 3
        val startCol = col - col % 3
        for (r in startRow until startRow + 3) {
            for (c in startCol until startCol + 3) {
                if (board[r][c] == num) {
                    return false
                }
            }
        }

        return true
    }

    fun isMoveValid(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        if (num == 0) return true // Clearing a cell is always valid

        // Check row
        for (c in 0..8) {
            if (c != col && board[row][c] == num) {
                return false
            }
        }

        // Check column
        for (r in 0..8) {
            if (r != row && board[r][col] == num) {
                return false
            }
        }

        // Check 3x3 subgrid
        val startRow = row - row % 3
        val startCol = col - col % 3
        for (r in startRow until startRow + 3) {
            for (c in startCol until startCol + 3) {
                if (r != row && c != col && board[r][c] == num) {
                    return false
                }
            }
        }

        return true
    }

    fun checkSolution(): Boolean {
        // Check if the board is full
        for (r in 0..8) {
            for (c in 0..8) {
                if (board[r][c] == 0) {
                    return false
                }
            }
        }

        // Check rows and columns for duplicates
        for (i in 0..8) {
            val rowSet = mutableSetOf<Int>()
            val colSet = mutableSetOf<Int>()
            for (j in 0..8) {
                if (!rowSet.add(board[i][j])) return false
                if (!colSet.add(board[j][i])) return false
            }
        }

        // Check 3x3 subgrids for duplicates
        for (i in 0..8 step 3) {
            for (j in 0..8 step 3) {
                val subgridSet = mutableSetOf<Int>()
                for (row in i until i + 3) {
                    for (col in j until j + 3) {
                        if (!subgridSet.add(board[row][col])) return false
                    }
                }
            }
        }

        return true
    }
}
