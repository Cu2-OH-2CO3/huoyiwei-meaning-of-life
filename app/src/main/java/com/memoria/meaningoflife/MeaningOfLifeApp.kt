package com.memoria.meaningoflife

import android.app.Application
import com.memoria.meaningoflife.data.database.AppDatabase

class MeaningOfLifeApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
    }

    companion object {
        lateinit var instance: MeaningOfLifeApp
            private set
    }
}