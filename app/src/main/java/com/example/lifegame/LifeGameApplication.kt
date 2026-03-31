package com.example.lifegame

import android.app.Application
import com.example.lifegame.service.PeriodicEffectManager
import com.example.lifegame.service.PeriodicQuestResetWorker
import com.example.lifegame.service.PeriodicStatusWorker
import com.example.lifegame.service.QuestCompletionManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LifeGameApplication : Application() {

    @Inject
    lateinit var periodicEffectManager: PeriodicEffectManager

    @Inject
    lateinit var questCompletionManager: QuestCompletionManager

    override fun onCreate() {
        super.onCreate()
        PeriodicStatusWorker.schedule(this)
        PeriodicQuestResetWorker.schedule(this)
        periodicEffectManager.start()
        questCompletionManager.start()
    }
}
