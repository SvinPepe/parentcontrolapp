package com.example.parentcontrol

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parentcontrol.databinding.ActivityTaskBinding
import java.util.Random

class TaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskBinding
    private var correctAnswer: Int = 0
    private lateinit var prefs: SharedPreferences
    private val random = Random()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("parent_control_prefs", MODE_PRIVATE)
        generateTask()

        binding.submitButton.setOnClickListener {
            checkAnswer()
        }
    }

    private fun generateTask() {
        val taskType = TaskType.values()[random.nextInt(TaskType.values().size)]

        when (taskType) {
            TaskType.MULTIPLICATION -> {
                val a = (2..9).random()
                val b = (2..9).random()
                correctAnswer = a * b
                binding.taskText.text = "How much is $a × $b?"
            }

            TaskType.ADDITION -> {
                val a = (10..99).random()
                val b = (10..99).random()
                correctAnswer = a + b
                binding.taskText.text = "How much is $a + $b?"
            }

            TaskType.SUBTRACTION -> {
                val a = (50..100).random()
                val b = (10..49).random()
                correctAnswer = a - b
                binding.taskText.text = "How much is $a - $b?"
            }
        }
    }

    private fun checkAnswer() {
        val userAnswer = binding.answerInput.text.toString().toIntOrNull()
        if (userAnswer == correctAnswer) {
            addExtraTime(15 * 60 * 1000) // 15 минут
            Toast.makeText(this, "Correct! 15 minutes added", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Incorrect, try again!", Toast.LENGTH_SHORT).show()
            binding.answerInput.text?.clear()
        }
    }

    private fun addExtraTime(timeMillis: Long) {
        val extraTime = prefs.getLong("extra_time", 0)
        prefs.edit().putLong("extra_time", extraTime + timeMillis).apply()
    }
}

enum class TaskType {
    MULTIPLICATION, ADDITION, SUBTRACTION
}