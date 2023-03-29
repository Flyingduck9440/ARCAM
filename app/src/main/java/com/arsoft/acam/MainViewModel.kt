package com.arsoft.acam

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

sealed class MainEvents {
    object OpenCamera: MainEvents()
    object CloseCamera: MainEvents()
}

data class MainStateHolder(
    val isCameraOpen: Boolean = false
)

class MainViewModel: ViewModel() {
    var states by mutableStateOf(MainStateHolder())
        private set

    fun onEvent(event: MainEvents) {
        when (event) {
            MainEvents.OpenCamera -> {
                states = states.copy(
                    isCameraOpen = true
                )
            }
            MainEvents.CloseCamera -> {
                states = states.copy(
                    isCameraOpen = false
                )
            }
        }
    }
}