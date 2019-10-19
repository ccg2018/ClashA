package com.github.cgg.clasha.data

import kotlin.math.log

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-06-15
 * @describe
 */
class LogsRepository private constructor(private val logsLocalDataSource: LogsDataSource) : LogsDataSource {

    companion object {
        private var instance: LogsRepository? = null

        fun getInstance(logsLocalDataSource: LogsDataSource) = instance ?: synchronized(this) {
            instance ?: LogsRepository(logsLocalDataSource).also { instance = it }
        }
    }

    var cachedLogs: LinkedHashMap<Long, LogMessage> = LinkedHashMap()

    var cachedIsDirty = false

    override fun getLogsByPage(profileId: Long, page: Int, callback: LogsDataSource.LoadLogsCallback) {
        logsLocalDataSource.getLogsByPage(profileId, page, object : LogsDataSource.LoadLogsCallback {
            override fun onLogsLoaded(logs: List<LogMessage>) {
                callback.onLogsLoaded(logs)
            }

            override fun onDataNotAvailable() {
                callback.onDataNotAvailable()
            }
        })
    }

    override fun removeAll() {
        logsLocalDataSource.removeAll()
    }
}