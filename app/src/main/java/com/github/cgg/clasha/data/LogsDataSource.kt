package com.github.cgg.clasha.data

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-06-15
 * @describe
 */

interface LogsDataSource {
    interface LoadLogsCallback {
        fun onLogsLoaded(logs: List<LogMessage>)
        fun onDataNotAvailable()
    }

    fun getLogsByPage(profileId: Long, page: Int = 0, callback: LoadLogsCallback)
}