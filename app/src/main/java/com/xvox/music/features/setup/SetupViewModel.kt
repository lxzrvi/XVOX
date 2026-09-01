package com.xvox.music.features.setup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SetupViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val preferencesRepository =
        UserPreferencesRepository(application)

    private val _state =
        MutableStateFlow(SetupUiState())

    val state: StateFlow<SetupUiState> =
        _state.asStateFlow()

    fun setName(name: String) {
        if (name.length <= 12) {
            _state.update {
                it.copy(name = name)
            }
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

    fun completeSetup(
        onComplete: () -> Unit
    ) {
        val current = _state.value

        if (!current.setupComplete) {
            return
        }

        viewModelScope.launch {
            preferencesRepository.completeSetup(
                username = current.name.trim(),
                selectedPfp =
                    current.selectedPfp.name,
                customPfpUri =
                    current.customPfpUri?.toString()
            )

            onComplete()
        }
    }
}
