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

    init {
        // Setup uses the same persisted gallery as the profile editor.
        viewModelScope.launch {
            preferencesRepository.customPfpUris.collect { uris ->
                _state.update { current ->
                    val stillThere = current.customPfpUri?.toString()?.takeIf { it in uris }
                    current.copy(
                        customPfpUris = uris,
                        customPfpUri = stillThere?.let(Uri::parse),
                        selectedPfp = if (current.selectedPfp == PfpType.CUSTOM && stillThere == null) PfpType.DEFAULT else current.selectedPfp
                    )
                }
            }
        }
    }

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

    /** Copies the picked image into app storage and stacks it; it stays until deleted. */
    fun addCustomPfp(uri: Uri) {
        viewModelScope.launch {
            val stored = preferencesRepository.addCustomPfp(uri.toString()) ?: return@launch
            _state.update { it.copy(selectedPfp = PfpType.CUSTOM, customPfpUri = Uri.parse(stored)) }
        }
    }

    fun setCustomPfp(uri: Uri) = addCustomPfp(uri)

    /** Picks an already-kept picture out of the stack. */
    fun selectCustomPfp(uri: String) {
        _state.update { it.copy(selectedPfp = PfpType.CUSTOM, customPfpUri = Uri.parse(uri)) }
    }

    fun deleteCustomPfp(uri: String) {
        viewModelScope.launch { preferencesRepository.removeCustomPfp(uri) }
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
