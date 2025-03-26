// package com.onseju.orderservice.listener;
//
// import org.springframework.amqp.core.Message;
// import org.springframework.amqp.rabbit.annotation.RabbitListener;
// import org.springframework.stereotype.Component;
//
// import com.onseju.orderservice.global.config.RabbitMQConfig;
//
// import lombok.extern.slf4j.Slf4j;
//
// @Slf4j
// @Component
// public class DeadLetterListener {
//
//     @RabbitListener(queues = RabbitMQConfig.DLX_QUEUE)
//     public void processFailedMessages(Message message) {
//         try {
//             log.warn("Dead Letter 큐에서 실패한 메시지 발견: {}", message);
//
//             // 메시지 헤더에서 원래 큐와 라우팅 키 정보 추출
//             String xDeathHeader = message.getMessageProperties().getHeader("x-death");
//             log.info("실패 원인 정보: {}", xDeathHeader);
//
//             String messageBody = new String(message.getBody());
//             log.info("실패한 메시지 내용: {}", messageBody);
//
//             // 필요 시 실패 원인에 따른, 재처리 또는 알림 로직 추가
//         } catch (Exception e) {
//             log.error("Dead Letter 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
//         }
//     }
// }