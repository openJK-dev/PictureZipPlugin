package com.github.openjkdev.picturezipplugin.thread

import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor


class ThreadPools() {
    private var mAsyncExecutor: ThreadPoolExecutor = DefaultWorkExecutor()

    companion object {
        @Volatile
        private var sInstance: ThreadPools? = null

        fun instance(): ThreadPools {
            if (sInstance == null) {
                synchronized(ThreadPools::class.java) {
                    if (sInstance == null) {
                        sInstance = ThreadPools()
                    }
                }
            }
            return sInstance!!
        }
    }


    fun asyncExecutor(): ExecutorService {
        return instance().mAsyncExecutor
    }

}