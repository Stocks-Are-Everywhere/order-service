package com.onseju.orderservice.global.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	@Bean
	public MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(jsonMessageConverter());
		return template;
	}

	// 교환기(Exchange) 설정
	@Bean
	public DirectExchange matchingExchange() {
		return new DirectExchange("matching.exchange");
	}

	@Bean
	public DirectExchange matchedExchange() {
		return new DirectExchange("matched.exchange");
	}

	// 큐(Queue) 설정
	@Bean
	public Queue matchingEventQueue() {
		return new Queue("matching.event.queue", true);
	}

	@Bean
	public Queue matchedEventQueue() {
		return new Queue("matched.event.queue", true);
	}


	// 바인딩(Binding) 설정
	@Bean
	public Binding matchingEventBinding(Queue matchingEventQueue, DirectExchange matchingExchange) {
		return BindingBuilder.bind(matchingEventQueue)
			.to(matchingExchange)
			.with("matching.event");
	}

	@Bean
	public Binding matchedEventBinding(Queue matchedEventQueue, DirectExchange matchedExchange) {
		return BindingBuilder.bind(matchedEventQueue)
			.to(matchedExchange)
			.with("matched.event");
	}
}

