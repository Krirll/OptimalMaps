package ru.krirll.optimalmaps.presentation.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.krirll.optimalmaps.R


class AppActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}