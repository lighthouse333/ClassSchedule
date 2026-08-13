package com.example.timetable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.timetable.ui.TimetableScreen
import com.example.timetable.ui.TimetableViewModel
import com.example.timetable.ui.theme.ClassScheduleTheme

class MainActivity : ComponentActivity() {
    private var foregroundEntry by mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        foregroundEntry++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val timetableViewModel = ViewModelProvider(this)[TimetableViewModel::class.java]

        setContent {
            ClassScheduleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TimetableScreen(
                        viewModel = timetableViewModel,
                        foregroundEntry = foregroundEntry,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
