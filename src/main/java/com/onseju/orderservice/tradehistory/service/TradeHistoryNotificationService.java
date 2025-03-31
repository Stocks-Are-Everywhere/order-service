package com.onseju.orderservice.tradehistory.service;

import com.onseju.orderservice.events.MatchedEvent;
import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.repository.OrderRepositoryImpl;
import com.onseju.orderservice.tradehistory.dto.MatchingNotificationDto;
import com.onseju.orderservice.tradehistory.repository.SseEmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeHistoryNotificationService {

    private static final Long NOTIFICATION_TIME_OUT = 60L * 60 * 60 * 60;

    private final SseEmitterRepository orderNotificationRepository;
    private final OrderRepositoryImpl orderRepository;

    public SseEmitter subscribe(Long memberId) {
        SseEmitter emitter = new SseEmitter(NOTIFICATION_TIME_OUT);
        return orderNotificationRepository.save(memberId, emitter);
    }

    public void sendNotification(final MatchedEvent event) {
        log.info("Sending matched event {}", event);
        try {
            log.info("Sending matched event {}", event);
            sendNotificationToSellOrder(event);
            sendNotificationToBuyOrder(event);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendNotificationToSellOrder(final MatchedEvent event) throws IOException {
        Order sellOrder = orderRepository.getById(event.sellOrderId());
        Optional<SseEmitter> sellOrderEmitter = orderNotificationRepository.findByMemberId(sellOrder.getAccountId());

        if (sellOrderEmitter.isPresent()) {
            sellOrderEmitter.get().send(toMatchingNotificationDto(sellOrder, event));
        } else {
            System.err.println("SellOrderEmitter is null for memberId: "
                    + sellOrder.getAccountId());
        }
    }

    private void sendNotificationToBuyOrder(final MatchedEvent event) throws IOException {
        Order buyOrder = orderRepository.getById(event.buyOrderId());
        Optional<SseEmitter> buyOrderEmitter = orderNotificationRepository.findByMemberId(buyOrder.getAccountId());

        if (buyOrderEmitter.isPresent()) {
            buyOrderEmitter.get().send(toMatchingNotificationDto(buyOrder, event));
        } else {
            System.err.println("BuyOrderEmitter is null for memberId: "
                    + buyOrder.getAccountId());
        }
    }

    private MatchingNotificationDto toMatchingNotificationDto(final Order order, final MatchedEvent event) {
        return MatchingNotificationDto.builder()
                .orderId(order.getId())
                .companyCode(order.getCompanyCode())
                .type(order.getType())
                .price(event.price())
                .quantity(event.quantity())
                .createdAt(event.tradeAt())
                .build();
    }
}