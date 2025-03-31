package com.onseju.orderservice.global.utils;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.f4b6a3.tsid.TsidFactory;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TsidGenerator {

    private TsidFactory tsidFactory;
    private final Queue<Long> idPool = new ConcurrentLinkedQueue<>();

    @Value("${tsid.node-id:1}")
    private int nodeId;

    @Value("${tsid.use-secure-random:false}")
    private boolean useSecureRandom;

    @Value("${tsid.batch-size:1000}")
    private int batchSize;

    @PostConstruct
    public void init() {
        validateNodeId();
        initializeTsidFactory();
        log.info("Initialized TsidGenerator with nodeId: {}, useSecureRandom: {}", nodeId, useSecureRandom);
    }

    private void validateNodeId() {
        if (nodeId < 0 || nodeId > 1023) {
            throw new IllegalArgumentException("Node ID must be between 0 and 1023");
        }
    }

    private void initializeTsidFactory() {
        TsidFactory.Builder builder = TsidFactory.builder()
            .withNode(nodeId)
            .withClock(Clock.systemUTC());

        if (useSecureRandom) {
            builder.withRandom(new SecureRandom());
        }

        this.tsidFactory = builder.build();
    }

    /**
     * 새로운 ID를 생성합니다.
     * ID 풀이 비어있으면 자동으로 채워집니다.
     */
    public Long nextId() {
        Long id = idPool.poll();
        if (id == null) {
            refillPool();
            id = idPool.poll();
        }
        return id;
    }

    /**
     * 지정된 수만큼의 ID를 생성합니다.
     */
    public List<Long> nextIds(int size) {
        List<Long> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(nextId());
        }
        return ids;
    }

    /**
     * ID 풀을 리필합니다.
     * 스레드 안전을 위해 synchronized 사용
     */
    private synchronized void refillPool() {
        if (idPool.isEmpty()) {
            for (int i = 0; i < batchSize; i++) {
                idPool.offer(tsidFactory.create().toLong());
            }
            log.debug("Refilled ID pool with {} new IDs", batchSize);
        }
    }
}