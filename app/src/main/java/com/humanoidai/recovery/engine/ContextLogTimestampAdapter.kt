package com.humanoidai.recovery.engine

import com.humanoidai.memory.dao.ContextLogDao

class ContextLogTimestampAdapter(
    private val contextLogDao: ContextLogDao
) : LastContextTimestampProvider {

    override suspend fun getLastContextTimestamp(): Long? {
        return contextLogDao.getMostRecent()?.timestamp
    }

    override suspend fun getLastContextEventType(): String? {
        return contextLogDao.getMostRecent()?.eventType
    }
}
