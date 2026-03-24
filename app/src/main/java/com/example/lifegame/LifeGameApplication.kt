package com.example.lifegame

import android.app.Application
import com.example.lifegame.service.PeriodicEffectManager
import com.example.lifegame.service.PeriodicStatusWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LifeGameApplication : Application() {

    @Inject
    lateinit var periodicEffectManager: PeriodicEffectManager

    override fun onCreate() {
        super.onCreate()
        PeriodicStatusWorker.schedule(this)
        periodicEffectManager.start()
    }
}
