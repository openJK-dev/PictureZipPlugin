package com.github.openjkdev.picturezipplugin.thread

import java.util.concurrent.BlockingQueue

import java.util.concurrent.LinkedBlockingQueue

import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class DefaultWorkExecutor private constructor(corePoolSize: Int,
                                              maximumPoolSize: Int,
                                              keepAliveTime: Long,
                                              unit: TimeUnit,
                                              workQueue: BlockingQueue<Runnable>) : ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue) {
    constructor() : this(CORE_SIZE, MAX_SIZE, KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>()) {}

    companion object {
        private val CPU_SIZE = Runtime.getRuntime().availableProcessors()
        private var CORE_SIZE = 0
        private var MAX_SIZE = 0
        private const val KEEP_ALIVE_TIME = 1L

        init {
            // 线程池核心线程数为 cpu 核心数加一
            CORE_SIZE = CPU_SIZE + 1
            // 线程池最大线程数为 cpu 核心数2被加一
            MAX_SIZE = CPU_SIZE * 2 + 1
        }
    }
}
