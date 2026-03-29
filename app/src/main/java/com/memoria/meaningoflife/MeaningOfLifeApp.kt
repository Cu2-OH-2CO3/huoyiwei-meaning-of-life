package com.memoria.meaningoflife

import android.app.Application
import com.memoria.meaningoflife.data.database.AppDatabase
import com.memoria.meaningoflife.utils.LogManager

class MeaningOfLifeApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)

        // 初始化日志管理器
        LogManager.init(this)
        LogManager.i("MeaningOfLifeApp", "应用启动")
    }

    companion object {
        lateinit var instance: MeaningOfLifeApp
            private set
    }
}