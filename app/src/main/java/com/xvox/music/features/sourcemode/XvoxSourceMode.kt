package com.xvox.music.features.sourcemode

/**
 * Deliberately narrow source selection contract.
 *
 * Offline always means the device-indexed music library. Online is only a request to use an
 * approved provider integration; it is never a signal to scrape web players, extract streams, or
 * bypass advertising/entitlements.
 */
enum class XvoxSourceMode(
    val storageValue: String,
    val title: String,
    val summary: String
) {
    OFFLINE(
        storageValue = "offline",
        title = "Offline",
        summary = "Play and search your device library"
    ),
    ONLINE(
        storageValue = "online",
        title = "Online",
        summary = "Use an approved provider when one is configured"
    );

    companion object {
        fun fromStorage(value: String?): XvoxSourceMode =
            entries.firstOrNull { it.storageValue == value?.lowercase() } ?: OFFLINE
    }
}

/** Capability information exposed by compliant provider integrations. */
data class XvoxOfficialProviderReadiness(
    val discoveryAvailable: Boolean,
    val playbackAvailable: Boolean,
    val message: String
)

/**
 * Boundary for a future first-party/partner SDK or approved backend. Implementations must use the
 * provider's supported discovery and playback APIs and must honour the provider's authentication,
 * licensing, ads, and playback policy. No unofficial stream extraction belongs behind this API.
 */
interface XvoxOfficialProviderGateway {
    val readiness: XvoxOfficialProviderReadiness
}

/**
 * Safe product default. The UI can remember an Online choice, but no online discovery/playback is
 * exposed until an approved provider backend/SDK is intentionally supplied by the product.
 */
object XvoxUnconfiguredOfficialProviderGateway : XvoxOfficialProviderGateway {
    override val readiness = XvoxOfficialProviderReadiness(
        discoveryAvailable = false,
        playbackAvailable = false,
        message = "Online discovery and playback need an approved provider API or backend configuration."
    )
}
