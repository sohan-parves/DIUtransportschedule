package com.sohan.diutransportschedule

import android.app.Application
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import com.sohan.diutransportschedule.db.AppDatabase
import com.sohan.diutransportschedule.prefs.UserPrefs
import com.sohan.diutransportschedule.sync.ScheduleRepository
import com.sohan.diutransportschedule.sync.VersionStore

class App : Application() {
    lateinit var repo: ScheduleRepository

    override fun onCreate() {
        super.onCreate()

        val db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "diu.db"
        ).build()

        repo = ScheduleRepository(
            dao = db.scheduleDao(),
            fs = FirebaseFirestore.getInstance(),
            store = VersionStore(this),
            prefs = UserPrefs(this)   // ✅ prefs package এরটা
        )
    }
}