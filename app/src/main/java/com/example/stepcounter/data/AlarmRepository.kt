package com.example.stepcounter.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AlarmRepository @Inject constructor(
    private val alarmDao: AlarmDao) {

    suspend fun insertAlarm(alarm: Alarm): Long {
        return alarmDao.insert(alarm)
    }

    suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.update(alarm)
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        alarmDao.delete(alarm)
    }

    fun getAllAlarms(): Flow<List<Alarm>> {
        return alarmDao.getAllAlarms()
    }

    suspend fun getAlarmById(creationTime: Int): Alarm? {
        return alarmDao.getAlarmById(creationTime)
    }
}