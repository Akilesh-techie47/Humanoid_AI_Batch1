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
        GapEventEntity::class,
        AuditLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class Aura360Database : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun interactionDao(): InteractionDao
    abstract fun alertHistoryDao(): AlertHistoryDao
    abstract fun contextLogDao(): ContextLogDao
    abstract fun gapEventDao(): GapEventDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: Aura360Database? = null

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN label TEXT NOT NULL DEFAULT 'Unknown'")
                db.execSQL("ALTER TABLE users ADD COLUMN viewpointsJson TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add password fields to users
                db.execSQL("ALTER TABLE users ADD COLUMN passwordHash TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN passwordSalt TEXT")

                // 2. Create audit_logs table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS audit_logs (
                        logId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        action TEXT NOT NULL,
                        status TEXT NOT NULL,
                        details TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): Aura360Database {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): Aura360Database {
            val vault = PrivacyVault(context)
            val passphrase = vault.getDatabasePassphrase()

            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                Aura360Database::class.java,
                "aura360_memory_v2.db"
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
