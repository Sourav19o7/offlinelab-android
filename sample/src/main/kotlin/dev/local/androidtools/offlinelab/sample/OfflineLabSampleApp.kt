package dev.local.androidtools.offlinelab.sample

import android.app.Application
import dev.local.androidtools.offlinelab.OfflineLab
import dev.local.androidtools.offlinelab.OfflineLabConfig
import dev.local.androidtools.offlinelab.model.NetworkProfile

class OfflineLabSampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        OfflineLab.initialize(
            context = this,
            config = OfflineLabConfig(
                enabled = BuildConfig.DEBUG,
                defaultProfile = NetworkProfile.Normal,
            ),
        )
    }
}
