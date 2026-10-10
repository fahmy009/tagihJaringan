package com.fahms.tagihjaringan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahms.tagihjaringan.ui.dashboard.DashboardScreen
import com.fahms.tagihjaringan.ui.dashboard.DashboardViewModel
import com.fahms.tagihjaringan.ui.theme.TagihJaringanTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels {
        val app = application as TagihJaringanApplication
        DashboardViewModel.Factory(app.appRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TagihJaringanTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
    }
}
