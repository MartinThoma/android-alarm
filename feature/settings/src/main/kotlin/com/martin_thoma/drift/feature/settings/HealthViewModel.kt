package com.martin_thoma.drift.feature.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
internal class HealthViewModel
    @Inject
    constructor(
        private val phone: PhoneSettings,
    ) : ViewModel() {
        private val mutableHealth = MutableStateFlow(Health.of(phone))
        val health: StateFlow<Health> = mutableHealth.asStateFlow()

        /** Reads the settings again; the user may just have changed one in the system settings. */
        fun refresh() {
            mutableHealth.value = Health.of(phone)
        }
    }
