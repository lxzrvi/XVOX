package com.xvox.music.features.setup

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SetupViewModel : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    fun setName(name: String) {
        _state.update {
            it.copy(name = name.take(32))
        }
    }

    fun selectPfp(type: PfpType) {
        _state.update {
            it.copy(selectedPfp = type)
        }
    }

    fun setCustomPfp(uri: Uri) {
        _state.update {
            it.copy(
                selectedPfp = PfpType.CUSTOM,
                customPfpUri = uri
            )
        }
    }

    fun updatePermissions(
        audioGranted: Boolean,
        notificationGranted: Boolean
    ) {
        _state.update {
            it.copy(
                audioGranted = audioGranted,
                notificationGranted = notificationGranted
            )
        }
    }
}
