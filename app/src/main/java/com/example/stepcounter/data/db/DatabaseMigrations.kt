package com.example.stepcounter.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration starts with 4 -> 5 because, before that fall back to destructive migration was being used.
 */

// Migration 4 -> 5: Added ringtoneUri: String? column for handling ringtones.
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN ringtoneUri TEXT DEFAULT NULL")
    }
}