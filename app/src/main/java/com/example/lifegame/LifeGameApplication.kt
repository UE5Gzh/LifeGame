package com.example.lifegame

import android.app.Application
import com.example.lifegame.service.PeriodicStatusWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LifeGameApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PeriodicStatusWorker.schedule(this)
    }
}
