package com.onseju.orderservice.ki.service;

import org.springframework.stereotype.Service;

import com.onseju.orderservice.ki.client.KIWebSocketClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class KIStockDataService {
	private final KIWebSocketClient kisWebSocketClient;

	public void startAllStream(String stockCode) {
		try {
			kisWebSocketClient.connectAll(stockCode);
		} catch (Exception e) {
			throw new RuntimeException("Failed to start stock data stream", e);
		}
	}

	public void stopAllStream(String stockCode) {
		try {
			kisWebSocketClient.disconnectAll();
		} catch (Exception e) {
			log.error("Failed to stop stock data stream for code {}: {}", stockCode, e.getMessage());
		}
	}

}
