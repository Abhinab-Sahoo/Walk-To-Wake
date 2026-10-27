package com.example.stepcounter.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.stepcounter.data.local.Alarm
import com.example.stepcounter.data.local.AlarmDao
import com.example.stepcounter.data.local.Converters

@TypeConverters(Converters::class)
@Database(entities = [Alarm::class], version = 5, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao
}