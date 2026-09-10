package com.labtools.semenanalysis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.labtools.semenanalysis.ui.CalibrationScreen
import com.labtools.semenanalysis.ui.CaptureScreen
import com.labtools.semenanalysis.ui.DisclaimerScreen
import com.labtools.semenanalysis.ui.HistoryScreen
import com.labtools.semenanalysis.ui.ReportScreen

object Routes {
    const val DISCLAIMER = "disclaimer"
    const val CALIBRATION = "calibration"
    const val CAPTURE = "capture"
    const val REPORT = "report/{sampleId}"
    const val HISTORY = "history"

    fun report(sampleId: String) = "report/$sampleId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost()
                }
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.DISCLAIMER) {

        composable(Routes.DISCLAIMER) {
            DisclaimerScreen(
                onAgree = {
                    navController.navigate(Routes.CALIBRATION) {
                        popUpTo(Routes.DISCLAIMER) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CALIBRATION) {
            CalibrationScreen(
                onCalibrationSaved = {
                    navController.navigate(Routes.CAPTURE)
                },
                onViewHistory = { navController.navigate(Routes.HISTORY) }
            )
        }

        composable(Routes.CAPTURE) {
            CaptureScreen(
                onAnalysisComplete = { sampleId ->
                    navController.navigate(Routes.report(sampleId))
                },
                onRecalibrate = { navController.navigate(Routes.CALIBRATION) }
            )
        }

        composable(
            route = Routes.REPORT,
            arguments = listOf(navArgument("sampleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sampleId = backStackEntry.arguments?.getString("sampleId") ?: ""
            ReportScreen(
                sampleId = sampleId,
                onDone = {
                    navController.navigate(Routes.CAPTURE) {
                        popUpTo(Routes.CAPTURE) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenSample = { sampleId -> navController.navigate(Routes.report(sampleId)) }
            )
        }
    }
}
