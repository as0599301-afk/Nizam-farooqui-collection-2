package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.DuckingSettings
import com.example.data.model.Playlist
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.SongItem
import com.example.data.model.VoiceRecording
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SongItem::class,
        VoiceRecording::class,
        Playlist::class,
        PlaylistSongCrossRef::class,
        DuckingSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NfcDatabase : RoomDatabase() {
    abstract fun nfcDao(): NfcDao

    companion object {
        @Volatile
        private var INSTANCE: NfcDatabase? = null

        fun getInstance(context: Context): NfcDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NfcDatabase::class.java,
                    "nfc_collection_database"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        try {
                            db.execSQL(
                                "INSERT OR IGNORE INTO ducking_settings (id, isDuckingEnabled, duckingLevelPercent, fadeDownSpeedMs, fadeUpSpeedMs, musicVolumePercent, voiceMicVolumePercent) VALUES (1, 1, 30, 300, 600, 100, 100)"
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
