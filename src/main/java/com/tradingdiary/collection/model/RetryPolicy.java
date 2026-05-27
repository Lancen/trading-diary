package com.tradingdiary.collection.model;

/**
 * 重试策略配置，封装最大重试次数和退避参数
 */
public class RetryPolicy {

    private final int maxRetries;
    private final long initialBackoffMs;
    private final double backoffMultiplier;
    private final long maxBackoffMs;

    /** 默认策略：3 次重试，2 秒初始退避，2 倍指数增长，最大 30 秒 */
    public static final RetryPolicy DEFAULT = new RetryPolicy(3, 2000, 2.0, 30000);

    public RetryPolicy(int maxRetries, long initialBackoffMs, double backoffMultiplier, long maxBackoffMs) {
        this.maxRetries = maxRetries;
        this.initialBackoffMs = initialBackoffMs;
        this.backoffMultiplier = backoffMultiplier;
        this.maxBackoffMs = maxBackoffMs;
    }

    public int getMaxRetries() { return maxRetries; }
    public long getInitialBackoffMs() { return initialBackoffMs; }
    public double getBackoffMultiplier() { return backoffMultiplier; }
    public long getMaxBackoffMs() { return maxBackoffMs; }

    /** 计算第 N 次重试的退避时间（毫秒），受 maxBackoffMs 上限约束 */
    public long getBackoffMs(int attempt) {
        long backoff = (long) (initialBackoffMs * Math.pow(backoffMultiplier, attempt));
        return Math.min(backoff, maxBackoffMs);
    }
}