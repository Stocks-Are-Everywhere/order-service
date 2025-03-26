package com.onseju.orderservice.global.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    // Queue
    public static final String ORDER_QUEUE = "order.queue";
    public static final String MATCHING_REQUEST_QUEUE = "matching.request.queue";
    public static final String MATCHING_RESULT_QUEUE = "matching.result.queue";
    public static final String USER_UPDATE_QUEUE = "user.update.queue";
    public static final String DLX_QUEUE = "dlx.queue";

    // Exchange
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String MATCHING_EXCHANGE = "matching.exchange";
    public static final String USER_EXCHANGE = "user.exchange";
    public static final String DLX_EXCHANGE = "dlx.exchange";
    
    // Routing Key
    public static final String ORDER_CREATE_KEY = "order.create";
    public static final String MATCHING_REQUEST_KEY = "matching.request";
    public static final String MATCHING_RESULT_KEY = "matching.result";
    public static final String USER_UPDATE_KEY = "user.update";
    public static final String DLX_KEY = "dlx.key";

    private static final long MESSAGE_TTL = 10000; // 10초

    // 큐와 교환기 연결 설정
    private static final Map<String, String> QUEUE_CONFIG = Map.ofEntries(
        Map.entry(ORDER_QUEUE, ORDER_EXCHANGE + ":" + ORDER_CREATE_KEY),
        Map.entry(MATCHING_REQUEST_QUEUE, MATCHING_EXCHANGE + ":" + MATCHING_REQUEST_KEY),
        Map.entry(MATCHING_RESULT_QUEUE, MATCHING_EXCHANGE + ":" + MATCHING_RESULT_KEY),
        Map.entry(USER_UPDATE_QUEUE, USER_EXCHANGE + ":" + USER_UPDATE_KEY)
    );

    @Value("${spring.rabbitmq.host:localhost}")
    private String host;
    @Value("${spring.rabbitmq.port:5672}")
    private int port;
    @Value("${spring.rabbitmq.username:admin}")
    private String username;
    @Value("${spring.rabbitmq.password:admin123}")
    private String password;

    @Bean
    public Declarables rabbitMQBindings() {
        List<Declarable> declarablesList = new ArrayList<>();

        // 1. DLX 설정을 먼저 생성 (다른 큐들이 참조할 수 있도록)
        Queue dlxQueue = QueueBuilder
            .durable(DLX_QUEUE)
            .withArgument("x-message-ttl", MESSAGE_TTL)
            .build();
        DirectExchange dlxExchange = new DirectExchange(DLX_EXCHANGE);
        Binding dlxBinding = BindingBuilder.bind(dlxQueue).to(dlxExchange).with(DLX_KEY);
        
        declarablesList.add(dlxQueue);
        declarablesList.add(dlxExchange);
        declarablesList.add(dlxBinding);

        // 2. 일반 큐 설정 (DLX 참조)
        QUEUE_CONFIG.forEach((queueName, bindingInfo) -> {
            String[] parts = bindingInfo.split(":");
            String exchangeName = parts[0];
            String routingKey = parts[1];

            // Dead Letter Exchange 설정 추가
            Map<String, Object> args = new HashMap<>();
            args.put("x-dead-letter-exchange", DLX_EXCHANGE); // 실패 시 메시지가 전달될 DLX
            args.put("x-dead-letter-routing-key", DLX_KEY);   // DLX 내에서 사용할 라우팅 키
            args.put("x-message-ttl", MESSAGE_TTL); // 메시지 타임아웃 설정 (24시간)

            Queue queue = QueueBuilder.durable(queueName).withArguments(args).build();
            TopicExchange exchange = new TopicExchange(exchangeName);
            Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
    
            declarablesList.add(queue);
            declarablesList.add(exchange);
            declarablesList.add(binding);
        });
    
        return new Declarables(declarablesList);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());

        // 메시지 전송 확인 콜백 추가
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("메시지 전송 성공: {}", correlationData != null ? correlationData.getId() : "null");
            } else {
                log.error("메시지 전송 실패. 원인: {}, 데이터: {}", cause, correlationData);
            }
        });
        
        // 라우팅할 수 없는 메시지에 대한 콜백 추가
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("메시지 라우팅 실패: {}, exchange: {}, routingKey: {}, replyCode: {}, replyText: {}",
                    returned.getMessage(), returned.getExchange(), 
                    returned.getRoutingKey(), returned.getReplyCode(), 
                    returned.getReplyText());
        });
        
        // 필요한 추가 설정
        rabbitTemplate.setMandatory(true); // 라우팅 실패한 메시지 반환 활성화
        
        return rabbitTemplate;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);
        return connectionFactory;
    }

}
