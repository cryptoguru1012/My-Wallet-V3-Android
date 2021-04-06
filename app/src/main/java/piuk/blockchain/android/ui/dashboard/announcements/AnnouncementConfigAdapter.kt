package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.remoteconfig.RemoteConfig
import com.google.gson.Gson
import io.reactivex.Single

data class AnnounceConfig(
    val order: List<String> = emptyList(), // Announcement card display order
    val interval: Long = 7 // Period, in days, between re-displaying dismissed periodic cards
)

interface AnnouncementConfigAdapter {
    val announcementConfig: Single<AnnounceConfig> // Priority display order of announcements
}

class AnnouncementConfigAdapterImpl(private val config: RemoteConfig) : AnnouncementConfigAdapter {

    private val gson = Gson()

    override val announcementConfig: Single<AnnounceConfig>
        get() {
            return config.getRawJson(ANNOUNCE_KEY)
                .map { gson.fromJson(it, AnnounceConfig::class.java) }
        }

    companion object {
        private const val ANNOUNCE_KEY = "announcements"
    }
}