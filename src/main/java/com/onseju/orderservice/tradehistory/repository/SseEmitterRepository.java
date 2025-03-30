package com.onseju.orderservice.tradehistory.repository;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterRepository {

    private final Map<Long, SseEmitter> elements = new ConcurrentHashMap<>();

    public SseEmitter save(Long memberId, SseEmitter sseEmitter) {
        elements.put(memberId, sseEmitter);

        return elements.get(memberId);
    }

    public SseEmitter findByMemberId(Long memberId) {
        return elements.get(memberId);
    }
}
