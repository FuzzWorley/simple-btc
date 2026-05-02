package com.bitcoinportfolio

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.bitcoinportfolio.data.secure.SecureStorage
import com.bitcoinportfolio.navigation.NavGraph
import com.bitcoinportfolio.navigation.Screen
import com.bitcoinportfolio.ui.theme.BitcoinPortfolioTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var secureStorage: SecureStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()
        setContent {
            BitcoinPortfolioTheme {
                val startDestination = if (secureStorage.isSetupComplete()) {
                    Screen.Auth.route
                } else {
                    Screen.Setup.route
                }
                NavGraph(startDestination = startDestination)
            }
        }
    }
}
