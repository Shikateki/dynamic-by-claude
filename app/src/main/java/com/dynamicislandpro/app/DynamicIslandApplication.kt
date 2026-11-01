package com.dynamicislandpro.app

import android.app.Application
import android.util.Log

class DynamicIslandApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Application started")
    }

    companion object {
        const val TAG = "DynamicIslandPro"
    }
}
