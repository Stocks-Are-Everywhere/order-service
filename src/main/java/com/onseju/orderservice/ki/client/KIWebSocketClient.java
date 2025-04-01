package com.onseju.orderservice.ki.client;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import com.onseju.orderservice.chart.service.ChartService;
import com.onseju.orderservice.global.utils.TsidGenerator;
import com.onseju.orderservice.ki.dto.KIStockDto;
import com.onseju.orderservice.ki.dto.KIStockHogaDto;
import com.onseju.orderservice.order.domain.Type;
import com.onseju.orderservice.order.dto.BeforeTradeOrderDto;
import com.onseju.orderservice.order.service.OrderService;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 한국투자증권 WebSocket 클라이언트
 * 주식 데이터 및 호가 데이터를 실시간으로 수신하기 위한 WebSocket 연결을 관리
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KIWebSocketClient {
	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

	private String stockApprovalKey;  // 주식 데이터용 approval key
	private String hogaApprovalKey;   // 호가 데이터용 approval key
	private Long tokenExpireTime;     // 토큰 만료 시간 (밀리초 타임스탬프)

	private final OrderService orderService;
	private final ChartService chartService;
	private final TsidGenerator tsidGenerator;
	private final RestTemplate restTemplate;

	@Value("${ki.approvalUrl}")
	private String approvalUrl;

	@Value("${ki.grantType}")
	private String grantKey;

	// 체결 WebSocket 설정
	@Value("${ki.stockWsUrl}")
	private String stockWsUrl;
	@Value("${ki.appKey_1}")
	private String appKey_1;
	@Value("${ki.secretKey_1}")
	private String secretKey_1;

	// 호가 WebSocket 설정
	@Value("${ki.hogaWsUrl}")
	private String hogaWsUrl;
	@Value("${ki.appKey_2}")
	private String appKey_2;
	@Value("${ki.secretKey_2}")
	private String secretKey_2;

	/**
	 * 앱 시작 시 토큰 발급
	 */
	@PostConstruct
	public void init() {
		generateApprovalKeys();
	}

	/**
	 * 한국투자증권 API 승인키 발급
	 * 주식 데이터와 호가 데이터 각각에 대한 승인키를 발급하고 만료 시간 설정
	 */
	public void generateApprovalKeys() {
		try {
			// 주식 데이터용 승인키 발급
			stockApprovalKey = generateApprovalKey(appKey_1, secretKey_1);

			// 호가 데이터용 승인키 발급
			hogaApprovalKey = generateApprovalKey(appKey_2, secretKey_2);

			// 토큰 만료 시간 설정 (1일)
			tokenExpireTime = Instant.now().plusSeconds(24 * 60 * 60).toEpochMilli();

			log.info("한국투자증권 API 승인키 발급 완료");
		} catch (Exception e) {
			log.error("승인키 발급 실패", e);
			throw new RuntimeException("한국투자증권 API 승인키 발급 실패", e);
		}
	}

	/**
	 * 개별 승인키 발급 메소드
	 * 지정된 appKey와 secretKey를 사용하여 한투 API에 승인키 요청
	 */
	private String generateApprovalKey(final String appKey, final String secretKey) {
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		final Map<String, String> body = new HashMap<>();
		body.put("grant_type", grantKey);
		body.put("appkey", appKey);
		body.put("secretkey", secretKey);

		final HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
		final ResponseEntity<Map> response = restTemplate.postForEntity(approvalUrl, entity, Map.class);

		if (response.getStatusCode().is2xxSuccessful()) {
			final Map<String, String> responseBody = response.getBody();
			return responseBody.get("approval_key");
		} else {
			throw new RuntimeException("승인키 발급 실패: " + response.getStatusCode());
		}
	}

	/**
	 * 토큰 유효성 검사 및 필요시 재발급
	 * 토큰이 만료되었거나 없는 경우 새로 발급
	 */
	private void checkAndRefreshToken() {
		final long currentTime = Instant.now().toEpochMilli();
		if (tokenExpireTime == null || tokenExpireTime == 0 || currentTime > tokenExpireTime) {
			log.info("승인키 만료, 재발급 진행");
			generateApprovalKeys();
		}
	}

	/**
	 * 체결 데이터 / 호가 데이터 연결
	 */
	public void connectAll(final String companyCode) {
		checkAndRefreshToken();  // 연결 전 토큰 유효성 검사

		connectStockData(companyCode);
		connectHogaData(companyCode);
	}

	/**
	 * 체결 데이터 WebSocket 연결
	 */
	private void connectStockData(final String stockCode) {
		connect(stockApprovalKey, stockCode, stockWsUrl, "H0STCNT0", this::handleStockDataMessage);
	}

	/**
	 * 호가 데이터 WebSocket 연결
	 */
	private void connectHogaData(final String stockCode) {
		connect(hogaApprovalKey, stockCode, hogaWsUrl, "H0STASP0", this::handleHogaDataMessage);
	}

	/**
	 * WebSocket 연결 공통 메서드
	 */
	private void connect(final String key, final String stockCode, final String url, final String trId,
			final MessageHandler messageHandler) {
		final WebSocketClient client = new StandardWebSocketClient();
		final String sessionKey = generateSessionKey(trId, stockCode);

		final WebSocketHandler handler = new WebSocketHandler() {
			@Override
			public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
				log.info("Connected to KIS WebSocket server: {} for stock {}", url, stockCode);
				sessions.put(sessionKey, session);
				sendSubscribeMessage(key, session, stockCode, trId);
			}

			@Override
			public void handleMessage(final WebSocketSession session, final WebSocketMessage<?> message) {
				try {
					final String payload = (String)message.getPayload();
					log.info(payload);

					// 연결 확인 응답 메시지인지 확인
					if (payload.startsWith("{")) {
						log.info("Received connection response: {}", payload);
						return;
					}

					// 메시지 핸들러에 위임
					messageHandler.handle(stockCode, payload);
				} catch (Exception e) {
					log.error("Error handling message: {}", e.getMessage(), e);
				}
			}

			@Override
			public void handleTransportError(final WebSocketSession session, final Throwable exception) {
				log.error("Transport error: ", exception);
			}

			@Override
			public void afterConnectionClosed(final WebSocketSession session, final CloseStatus closeStatus) {
				log.info("Connection closed: {}", closeStatus);
				sessions.remove(sessionKey);

				// 1009 에러(메시지 크기 초과) 또는 다른 연결 문제 발생 시 재연결
				if (closeStatus.getCode() == 1009 || closeStatus.getCode() != 1000) {
					log.info("Connection closed with code {}, scheduling reconnect", closeStatus.getCode());
					scheduleReconnect(key, stockCode, url, trId, messageHandler);
				}
			}

			@Override
			public boolean supportsPartialMessages() {
				return false;
			}
		};

		try {
			client.execute(handler, new WebSocketHttpHeaders(), URI.create(url));
		} catch (Exception e) {
			log.error("Failed to connect to KIS WebSocket server: {}", url, e);
			throw new RuntimeException("WebSocket connection failed", e);
		}
	}

	/**
	 * 세션 키 생성
	 */
	private String generateSessionKey(final String trId, final String stockCode) {
		return trId + "_" + stockCode;
	}

	/**
	 * 재연결 스케줄링
	 * 연결이 끊어진 경우 5초 후 재연결 시도
	 */
	private void scheduleReconnect(final String key, final String stockCode, final String url, final String trId,
			final MessageHandler messageHandler) {
		log.info("재연결 진행, 5초 후 시도");
		try {
			Thread.sleep(5000); // 5초 대기
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		connect(key, stockCode, url, trId, messageHandler);
	}

	/**
	 * 구독 메시지 전송
	 */
	private void sendSubscribeMessage(final String key, final WebSocketSession session, final String stockCode,
			final String trId) throws IOException {
		final JSONObject request = createSubscribeRequest(key, stockCode, trId);
		session.sendMessage(new TextMessage(request.toString()));
	}

	/**
	 * 구독 요청 생성
	 */
	private JSONObject createSubscribeRequest(final String key, final String stockCode, final String trId) {
		final JSONObject header = new JSONObject();
		header.put("approval_key", key);
		header.put("custtype", "P");
		header.put("tr_type", "1");
		header.put("content-type", "utf-8");

		final JSONObject input = new JSONObject();
		input.put("tr_id", trId);
		input.put("tr_key", stockCode);

		final JSONObject body = new JSONObject();
		body.put("input", input);

		final JSONObject request = new JSONObject();
		request.put("header", header);
		request.put("body", body);

		return request;
	}

	/**
	 * 특정 연결 해제
	 */
	public void disconnect(final String trId, final String stockCode) {
		final String sessionKey = generateSessionKey(trId, stockCode);
		final WebSocketSession session = sessions.get(sessionKey);

		if (session != null && session.isOpen()) {
			try {
				session.close();
				sessions.remove(sessionKey);
				log.info("WebSocket connection closed for {} {}", trId, stockCode);
			} catch (IOException e) {
				log.error("Error closing WebSocket connection", e);
			}
		}
	}

	/**
	 * 모든 연결 해제
	 */
	public void disconnectAll() {
		for (final WebSocketSession session : sessions.values()) {
			if (session.isOpen()) {
				try {
					session.close();
					log.info("WebSocket connection closed");
				} catch (IOException e) {
					log.error("Error closing WebSocket connection", e);
				}
			}
		}
		sessions.clear();
	}

	/**
	 * 주식 데이터 메시지 처리
	 */
	private void handleStockDataMessage(final String stockCode, final String payload) {
		try {
			final KIStockDto stockData = parseKisData(payload);
			final Long buyOrderId = tsidGenerator.nextId();
			final Long sellOrderId = tsidGenerator.nextId();
			final Long now = Instant.now().toEpochMilli();

			final TradeHistory tradeHistory = TradeHistory.builder()
					.companyCode(stockCode)
					.sellOrderId(sellOrderId)
					.buyOrderId(buyOrderId)
					.price(BigDecimal.valueOf(stockData.getCurrentPrice()))
					.quantity(BigDecimal.valueOf(stockData.getAccVolume()))
					.tradeTime(now)
					.build();

			chartService.processNewTrade(tradeHistory);
		} catch (Exception e) {
			log.error("Error handling stock data message: {}", e.getMessage());
		}
	}

	/**
	 * 호가 데이터 메시지 처리
	 */
	private void handleHogaDataMessage(final String stockCode, final String payload) {
		try {
			final KIStockHogaDto stockData = parseKisHogaData(payload);

			// 매도 호가
			for (int i = 0; i < 10; i++) {
				final BigDecimal price = stockData.askPrices().get(i);
				final BigDecimal quantity = stockData.askRemains().get(i);

				final BeforeTradeOrderDto dto = BeforeTradeOrderDto.builder()
						.companyCode(stockData.stockCode())
						.type(Type.LIMIT_SELL.name())
						.totalQuantity(quantity)
						.price(price)
						.memberId(1L)
						.build();

				orderService.placeOrder(dto);
			}

			// 매수 호가
			for (int i = 0; i < 10; i++) {
				final BigDecimal price = stockData.bidPrices().get(i);
				final BigDecimal quantity = stockData.bidRemains().get(i);

				final BeforeTradeOrderDto dto = BeforeTradeOrderDto.builder()
						.companyCode(stockData.stockCode())
						.type(Type.LIMIT_BUY.name())
						.totalQuantity(quantity)
						.price(price)
						.memberId(1L)
						.build();

				orderService.placeOrder(dto);
			}
		} catch (Exception e) {
			log.error("Error handling hoga data message: {}", e.getMessage());
		}
	}

	/**
	 * 주식 데이터 파싱
	 */
	private KIStockDto parseKisData(final String rawData) {
		try {
			final String[] sections = rawData.split("\\|");
			if (sections.length < 4) {
				log.error("잘못된 데이터 형식: {}", rawData);
				throw new IllegalArgumentException("잘못된 데이터 형식");
			}

			final String[] fields = sections[3].split("\\^");
			final KIStockDto data = new KIStockDto();

			// 기본 정보 설정
			try {
				// 날짜 정보 파싱
				// 현재 날짜 가져오기 (체결 시간은 당일 데이터만 제공)
				final LocalDate today = LocalDate.now();

				// 시간 파싱
				final String hour = fields[1].substring(0, 2);
				final String minute = fields[1].substring(2, 4);
				final String second = fields[1].substring(4, 6);

				// LocalDateTime 생성
				final LocalDateTime time = LocalDateTime.of(
						today.getYear(),
						today.getMonth(),
						today.getDayOfMonth(),
						Integer.parseInt(hour),
						Integer.parseInt(minute),
						Integer.parseInt(second)
				);

				data.setTime(time.toEpochSecond(ZoneOffset.UTC));

				// 숫자 데이터 파싱 시 DecimalFormat 사용
				final DecimalFormat df = new DecimalFormat("#.##");
				df.setParseBigDecimal(true);

				// 가격 정보 설정
				data.setCurrentPrice(df.parse(fields[2]).doubleValue());
				data.setChangePrice(df.parse(fields[4]).doubleValue());
				data.setChangeRate(df.parse(fields[5]).doubleValue());
				data.setOpenPrice(df.parse(fields[7]).doubleValue());
				data.setHighPrice(df.parse(fields[8]).doubleValue());
				data.setLowPrice(df.parse(fields[9]).doubleValue());

				// 거래량 정보 설정 (정수 값)
				data.setVolume(Long.parseLong(fields[12]));
				data.setAccVolume(Long.parseLong(fields[13]));

				return data;
			} catch (ParseException | NumberFormatException e) {
				log.error("숫자 파싱 실패: {} - {}", e.getMessage(), rawData);
				throw new RuntimeException("데이터 파싱 실패", e);
			}
		} catch (Exception e) {
			log.error("데이터 처리 실패: {} - {}", e.getMessage(), rawData);
			throw e;
		}
	}

	/**
	 * 호가 데이터 파싱
	 */
	private KIStockHogaDto parseKisHogaData(final String rawData) {
		try {
			final String[] sections = rawData.split("\\|");
			if (sections.length < 4) {
				log.error("잘못된 데이터 형식: {}", rawData);
				throw new IllegalArgumentException("잘못된 데이터 형식");
			}

			final String[] fields = sections[3].split("\\^");

			// 매도호가(ASKP) 설정 (1-10)
			final List<BigDecimal> askPrices = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				askPrices.add(new BigDecimal(fields[3 + i]));
			}

			// 매수호가(BIDP) 설정 (1-10)
			final List<BigDecimal> bidPrices = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				bidPrices.add(new BigDecimal(fields[13 + i]));
			}

			// 매도호가 잔량(ASKP_RSQN) 설정 (1-10)
			final List<BigDecimal> askRemains = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				askRemains.add(new BigDecimal(fields[23 + i]));
			}

			// 매수호가 잔량(BIDP_RSQN) 설정 (1-10)
			final List<BigDecimal> bidRemains = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				bidRemains.add(new BigDecimal(fields[33 + i]));
			}

			return KIStockHogaDto.builder()
					.stockCode(fields[0])
					.businessHour(fields[1])
					.hourClassCode(fields[2])
					.askPrices(askPrices)
					.bidPrices(bidPrices)
					.askRemains(askRemains)
					.bidRemains(bidRemains)
					.totalAskRemain(parseLong(fields[43]))
					.totalBidRemain(parseLong(fields[44]))
					.overtimeTotalAskRemain(parseLong(fields[45]))
					.overtimeTotalBidRemain(parseLong(fields[46]))
					.anticipatedPrice(parseDouble(fields[47]))
					.anticipatedQuantity(parseLong(fields[48]))
					.anticipatedVolume(parseLong(fields[49]))
					.anticipatedCompared(parseDouble(fields[50]))
					.anticipatedComparedSign(fields[51])
					.anticipatedComparedRate(parseDouble(fields[52]))
					.accumulatedVolume(parseLong(fields[53]))
					.totalAskRemainChange(parseLong(fields[54]))
					.totalBidRemainChange(parseLong(fields[55]))
					.overtimeTotalAskRemainChange(parseLong(fields[56]))
					.overtimeTotalBidRemainChange(parseLong(fields[57]))
					.build();

		} catch (Exception e) {
			log.error("호가 데이터 처리 실패: {} - {}", e.getMessage(), rawData);
			throw new RuntimeException("호가 데이터 파싱 실패", e);
		}
	}

	/**
	 * Double 파싱 유틸리티 함수 - 빈 문자열이나 null 처리
	 */
	private Double parseDouble(final String value) {
		if (value == null || value.trim().isEmpty()) {
			return 0.0;
		}
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			log.warn("숫자 변환 실패: {}", value);
			return 0.0;
		}
	}

	/**
	 * Long 파싱 유틸리티 함수 - 빈 문자열이나 null 처리
	 */
	private Long parseLong(final String value) {
		if (value == null || value.trim().isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			log.warn("숫자 변환 실패: {}", value);
			return 0L;
		}
	}

	/**
	 * 메시지 핸들러 인터페이스
	 * WebSocket으로부터 수신된 메시지를 처리하기 위한 함수형 인터페이스
	 */
	@FunctionalInterface
	private interface MessageHandler {
		void handle(String stockCode, String payload);
	}
}
