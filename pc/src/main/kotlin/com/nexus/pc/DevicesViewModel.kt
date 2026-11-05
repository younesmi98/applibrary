package com.nexus.pc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class DevicesUiState(
    val devices: List<PairedDeviceEntity> = emptyList(),
    val offer: PairingOffer? = null,
    val relayError: String? = null,
)

class DevicesViewModel(private val dao: PcDao, private val pairing: PairingManager, private val relay: PcRelay?) : ViewModel() {
    private val extra = MutableStateFlow(DevicesUiState())
    val state: StateFlow<DevicesUiState> = combine(dao.devices(), extra) { d, e -> e.copy(devices = d) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DevicesUiState())

    fun startPairing() = viewModelScope.launch {
        val offer = pairing.createOffer()
        val r = relay?.publishOffer(offer)
        // Without a relay the code is still shown, but a companion cannot reach it. Say so instead of faking success.
        extra.update { it.copy(offer = offer, relayError = when { relay == null -> "No relay configured yet"; r?.isFailure == true -> "Could not reach the relay"; else -> null }) }
    }
    fun cancelPairing() = extra.update { it.copy(offer = null, relayError = null) }
    fun revoke(id: String) = viewModelScope.launch { pairing.revoke(id) }

    fun saveRule(gameId: String, exe: String, mode: TrackingMode, idleMinutes: Int?, deviceId: String? = null) = viewModelScope.launch {
        val name = exe.trim()
        if (name.isEmpty() || '/' in name || '\\' in name) return@launch   // executable name only, never a path
        dao.upsertRule(ProcessRuleEntity(UUID.randomUUID().toString(), gameId, deviceId, name, mode, idleMinutes, true))
    }
}
