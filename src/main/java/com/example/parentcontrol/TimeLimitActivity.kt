package com.example.parentcontrol

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.parentcontrol.databinding.ActivityTimeLimitBinding

class TimeLimitActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimeLimitBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimeLimitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Делаем активность поверх блокировки экрана
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        binding.unlockButton.setOnClickListener {
            startActivity(Intent(this, TaskActivity::class.java))
            finish()
        }
    }

//    override fun onBackPressed() {
//        super.onBackPressed()
//        // Запрещаем выход
//    }
}