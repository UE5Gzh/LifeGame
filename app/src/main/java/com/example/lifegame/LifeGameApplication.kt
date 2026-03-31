package com.example.lifegame

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.lifegame.service.PeriodicEffectManager
import com.example.lifegame.service.PeriodicQuestResetWorker
import com.example.lifegame.service.PeriodicStatusWorker
import com.example.lifegame.service.QuestCompletionManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LifeGameApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    @Inject
    lateinit var periodicEffectManager: PeriodicEffectManager

    @Inject
    lateinit var questCompletionManager: QuestCompletionManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        WorkManager.initialize(this, workManagerConfiguration)
        PeriodicStatusWorker.schedule(this)
        PeriodicQuestResetWorker.schedule(this)
        periodicEffectManager.start()
        questCompletionManager.start()
    }
}
