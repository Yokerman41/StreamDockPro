package com.streamdock.pro

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.streamdock.pro.ui.components.MainScreen
import com.streamdock.pro.ui.theme.StreamDockTheme
import com.streamdock.pro.ui.viewmodel.StreamDockViewModel
import com.streamdock.pro.ui.viewmodel.StreamDockViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: StreamDockViewModel by viewModels {
        StreamDockViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 35 fuerza edge-to-edge en Android 15+: se activa en todas las versiones
        // para un comportamiento uniforme (la app es siempre oscura → iconos claros).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            StreamDockTheme {
                val workspace by viewModel.workspace.collectAsState()
                val connectionState by viewModel.connectionState.collectAsState()
                val connectionMode by viewModel.connectionMode.collectAsState()
                val discoveredServers by viewModel.discoveredServers.collectAsState()
                val isScanning by viewModel.isScanning.collectAsState()
                val obsScenes by viewModel.obsScenes.collectAsState()

                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        workspace = workspace,
                        connectionState = connectionState,
                        onButtonPress = viewModel::onButtonPress,
                        onCreatePage = viewModel::createNewPage,
                        onSelectPage = viewModel::selectPage,
                        onUpdateButton = viewModel::updateButton,
                        onDeleteButton = viewModel::deleteButton,
                        onMoveButton = viewModel::moveButton,
                        currentConnectionMode = connectionMode,
                        onModeSelected = viewModel::setConnectionMode,
                        isScanning = isScanning,
                        onRefreshScan = viewModel::refreshScan,
                        discoveredServers = discoveredServers,
                        onConnectToServer = viewModel::connectToWifiServer,
                        onUsbSelected = viewModel::connectToUsbServer,
                        onDeleteProfile = viewModel::deleteActiveProfile,
                        obsScenes = obsScenes,
                        onFetchObsScenes = viewModel::fetchObsScenes,
                        onSwitchToScene = viewModel::switchToScene,
                    )
                }
            }
        }
    }

    // Nota: ya no se desconecta en onDestroy. Ante un cambio de configuración (tema, idioma,
    // multiventana...) la Activity se destruye pero el ViewModel sobrevive y quedaba desconectado.
    // La desconexión real ocurre en StreamDockViewModel.onCleared().
}
