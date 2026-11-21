package com.streamdock.pro.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.streamdock.pro.data.local.UUIDManager
import com.streamdock.pro.data.local.db.AppDatabase
import com.streamdock.pro.data.repository.StreamDockRepository

/**
 * El repositorio se crea aquí (sólo cuando realmente se instancia el ViewModel) en lugar de en
 * la Activity: antes cada recreación de la Activity creaba un repositorio nuevo que se descartaba.
 */
class StreamDockViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StreamDockViewModel::class.java)) {
            val clientUuid = UUIDManager(application).getOrGenerateUUID()
            val repository = StreamDockRepository(clientUuid, AppDatabase.getDatabase(application))
            @Suppress("UNCHECKED_CAST")
            return StreamDockViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
