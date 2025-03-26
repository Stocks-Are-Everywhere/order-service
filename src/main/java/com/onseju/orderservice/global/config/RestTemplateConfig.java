package com.onseju.orderservice.global.config;


import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Configuration
public class RestTemplateConfig {

	@Bean
	public RestTemplate restTemplate() {
		// HTTP/2 지원 연결 관리자 생성
		PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
			.setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
				.setSslContext(SSLContexts.createDefault())
				.setTlsVersions(TLS.V_1_3, TLS.V_1_2)
				.build())
			.setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
			.setConnPoolPolicy(PoolReusePolicy.LIFO)
			.setDefaultSocketConfig(SocketConfig.custom()
				.setSoTimeout(Timeout.of(30, TimeUnit.SECONDS))
				.build())
			.build();

		// HTTP 클라이언트 생성
		CloseableHttpClient httpClient = HttpClients.custom()
			.setConnectionManager(connectionManager)
			.build();

		// RestTemplate 팩토리 생성
		HttpComponentsClientHttpRequestFactory requestFactory =
			new HttpComponentsClientHttpRequestFactory(httpClient);
		requestFactory.setConnectTimeout(30000); // 30초
		requestFactory.setConnectionRequestTimeout(30000);

		return new RestTemplate(requestFactory);
	}
}
