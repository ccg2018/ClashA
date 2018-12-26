package com.github.cgg.clasha.net

import android.net.LocalSocket
import com.github.cgg.clasha.bg.LocalSocketListener
import com.github.cgg.clasha.utils.printLog
import kotlinx.coroutines.*
import java.io.File

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-03-01
 * @describe
 */

abstract class ConcurrentLocalSocketListener(name: String, socketFile: File) : LocalSocketListener(name, socketFile),
    CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext get() = Dispatchers.IO + job + CoroutineExceptionHandler { _, t -> printLog(t) }

    override fun accept(socket: LocalSocket) {
        launch { super.accept(socket) }
    }

    override fun shutdown(scope: CoroutineScope) {
        running = false
        job.cancel()
        super.shutdown(scope)
        scope.launch { job.join() }
    }
}