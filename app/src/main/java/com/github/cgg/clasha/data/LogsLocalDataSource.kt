package com.github.cgg.clasha.data

import com.github.cgg.clasha.utils.AppExecutors
import com.github.cgg.clasha.utils.Key.LOG_PAGESIZE

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-06-15
 * @describe
 */
class LogsLocalDataSource
private constructor(
    private val mAppExecutors: AppExecutors,
    private val logsDao: LogMessage.Dao
) : LogsDataSource {

    companion object {
        @Volatile
        private var instance: LogsLocalDataSource? = null

        fun getInstance(mAppExecutors: AppExecutors, logsDao: LogMessage.Dao) = instance ?: synchronized(this) {
            instance ?: LogsLocalDataSource(mAppExecutors, logsDao).also { instance = it }
        }
    }

    override fun getLogsByPage(profileId: Long, page: Int, callback: LogsDataSource.LoadLogsCallback) {
        mAppExecutors.diskIO.execute {
            val logs = logsDao.getByPage(profileId, page * LOG_PAGESIZE)
            mAppExecutors.mainThread.execute {
                if (logs.isEmpty()) {
                    callback.onDataNotAvailable()
                } else {
                    callback.onLogsLoaded(logs)
                }
            }
        }
    }
}