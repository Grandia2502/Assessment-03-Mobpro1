package com.grandiamuhammad3096.assessment03.database.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PhotoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PhotoDb : RoomDatabase() {
    abstract fun photoDao(): PhotoDao

    companion object {
        @Volatile private var INSTANCE: PhotoDb? = null

        fun get(context: Context): PhotoDb =
            INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                PhotoDb::class.java,
                "gallery.db"
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}