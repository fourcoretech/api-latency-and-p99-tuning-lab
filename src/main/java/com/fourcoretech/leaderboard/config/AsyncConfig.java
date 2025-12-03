package com.fourcoretech.leaderboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration for handling asynchronous operations
 *
 * PROBLEM: This async configuration has INTENTIONAL ISSUES:
 * 1. Thread pool is TOO SMALL (only 2 core threads) - causes thread starvation
 * 2. Queue capacity is TOO LARGE (500) - hides the problem by queuing forever
 * 3. No proper rejection policy - tasks just pile up in the queue
 * 4. No metrics or monitoring on thread pool usage
 *
 * REAL-WORLD IMPACT:
 * - Under load, only 2 threads handle all async work
 * - Requests queue up, causing latency spikes (high P99)
 * - No visibility into how many tasks are waiting
 * - System appears to work but performance degrades badly
 *
 * WHAT YOU'LL FIX:
 * - Increase core pool size to match expected load
 * - Add proper max pool size and reasonable queue capacity
 * - Add task rejection handling
 * - Expose thread pool metrics
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // PROBLEM 1: Way too few threads for handling concurrent requests
        // Only 2 threads will process all async tasks, causing bottleneck
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);

        // PROBLEM 2: Huge queue masks the problem by allowing unlimited queuing
        // This causes requests to wait in queue, spiking P99 latency
        executor.setQueueCapacity(500);

        // PROBLEM 3: Generic thread names make debugging difficult
        executor.setThreadNamePrefix("async-");

        // PROBLEM 4: No timeout on waiting for task completion
        // Tasks can wait in queue indefinitely
        executor.setWaitForTasksToCompleteOnShutdown(true);

        executor.initialize();
        return executor;
    }
}
