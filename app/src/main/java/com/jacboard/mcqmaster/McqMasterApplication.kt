package com.jacboard.mcqmaster

import android.app.Application
import com.jacboard.mcqmaster.util.UserPreferences

class McqMasterApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        UserPreferences.init(this)
    }
}
