package com.onseju.orderservice.producer;

import java.util.concurrent.CompletableFuture;

public interface EventProducer<T> {
    CompletableFuture<Void> publishEvent(T event);
}