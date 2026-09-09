package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.screens.AppTab
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SmmViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SmmViewModel by viewModels {
        SmmViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val navigateExtra = intent?.getStringExtra("EXTRA_NAVIGATE_TO")
        val initialTab = if (navigateExtra == "orders") AppTab.ORDERS else AppTab.NEW_ORDER

        setContent {
            MyApplicationTheme {
                MainAppScreen(
                    viewModel = viewModel,
                    initialTab = initialTab
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("SMMXpert") }
}
