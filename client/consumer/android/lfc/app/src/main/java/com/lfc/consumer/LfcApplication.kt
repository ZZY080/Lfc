package com.lfc.consumer

import android.app.Application
import com.lfc.consumer.location.AmapLocationHelper

class LfcApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AmapLocationHelper.ensurePrivacy(this)
    }
}
