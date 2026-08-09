package com.humanoidai.memory.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import com.humanoidai.memory.dao.*
import com.humanoidai.memory.entities.*
import com.humanoidai.memory.security.PrivacyVault
import com.humanoidai.recovery.model.GapEventDao
import com.humanoidai.recovery.model.GapEventEntity

@Database(
    entities = [
        UserEntity::class,
        InteractionEntity::class,
        AlertHistoryEntity::class,
        ContextLogEntity::class,
        GapEventEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HumanoidDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun interactionDao(): InteractionDao
    abstract fun alertHistoryDao(): AlertHistoryDao
    abstract fun contextLogDao(): ContextLogDao
    abstract fun gapEventDao(): GapEventDao

    companion object {
        @Volatile
        private var INSTANCE: HumanoidDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add eventType to context_logs
                db.execSQL("ALTER TABLE context_logs ADD COLUMN eventType TEXT")
                
                // 2. Create gap_events table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS gap_events (
                        gapId TEXT NOT NULL PRIMARY KEY,
                        gapStart INTEGER NOT NULL,
                        gapEnd INTEGER NOT NULL,
                        cause TEXT NOT NULL,
                        lastKnownContextJson TEXT,
                        eventsDuringGapJson TEXT NOT NULL,
                        reconstructedSummary TEXT,
                        tier TEXT NOT NULL,
                        acknowledged INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): HumanoidDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): HumanoidDatabase {
            val vault = PrivacyVault(context)
            val passphrase = vault.getDatabasePassphrase()

            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                HumanoidDatabase::class.java,
                "humanoid_memory_v2.db"
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
