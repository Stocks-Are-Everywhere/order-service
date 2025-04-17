# 📦 Order-service

### 🔖 서비스 개요

- **주제**: 주문 처리 및 주문 이벤트 발행을 담당하는 서비스  
- **주요 역할**:
  - 사용자 주문 요청 수신 및 처리
  - 주문 검증 및 상태 저장
  - 유저 서비스와 연동하여 잔액 및 보유 종목 검증
  - RabbitMQ를 통한 주문 이벤트 발행 (`order.created`)
  - 체결 결과 이벤트 수신 (`order.decreased`, `account.decreased`)
  - 회사 정보(CompanyCode) 제공 및 연동
  - 체결 결과 기반 거래 내역(TradeHistory) 저장 및 관리

### 🔗 주요 기능

#### 1️⃣ 주문 생성 및 처리
- 사용자로부터 주문 요청을 수신
- 가격 유효성, 보유 자산 검증을 위한 이벤트 발행
- 주문 데이터를 생성하고 상태 저장

#### 2️⃣ 주문 데이터 관리
- 주문 정보는 DB에 저장 및 관리됨
- 체결 결과 수신 시 주문 수량 감소 및 상태 업데이트

#### 3️⃣ 주문 검증 및 이벤트 발행
- 유저 서비스와의 통신을 통해 잔액 및 보유 주식 검증 요청
- 검증 결과에 따라 `order.created` 이벤트 발행

#### 4️⃣ 체결 결과 수신 및 반영
- Matching 서비스로부터 체결 결과 이벤트 수신
- 체결 수량에 따라 주문 상태 변경
- 거래 내역 데이터 생성 및 저장

#### 5️⃣ 회사 정보 조회
- CompanyCode 기준으로 종목 관련 정보 제공
- 주문 처리 시 종목 정보 검증 및 활용

#### 6️⃣ 거래 내역 관리
- 체결 완료 시 `TradeHistory` 기록 생성
- 사용자별, 종목별, 기간별 체결 내역 조회 기능 제공

---

### 🌏 주문 시퀀스

<img width="943" alt="주문 처리 흐름도" src="https://github.com/user-attachments/assets/0edb90be-ebb0-4b72-a83b-9baf6ed56ba8" />

---

### 🧱 시스템 전환 요약

| 항목             | 변경 전                      | 변경 후                          |
|------------------|-------------------------------|-----------------------------------|
| 구조             | 단일 서버                     | 분산 서버 (User / Order / Matching) |
| 통신 방식        | 메서드 호출 기반              | gRPC + 비동기 메시지 큐 (RabbitMQ) |
| 동기 방식        | 전체 동기 처리                 | 주문 생성은 동기, 체결은 비동기     |
| ID 생성 방식     | AUTO_INCREMENT                | **TSID (분산 고유 ID)**           |
| 종가 참조 방식   | 매 요청마다 DB 조회           | **인메모리 캐시 적용**             |

---

### 🌈 개선 사항

#### 1️⃣ 주문 ID 생성 방식 개선 - <ins>저장 공간 절약 및 고속 삽입 성능 확보</ins>

**📌 문제 상황**  
- 기존에는 `AUTO_INCREMENT`를 통해 ID를 생성했음  
- 분산 환경에서 충돌 가능성과 성능 병목 발생  
- ID 예측 가능성으로 인한 보안 이슈 존재

**✅ 개선 방향**  
- **TSID (Time-Sorted Unique ID)** 도입  
  - 분산 환경에서도 고유성 보장  
  - 8바이트 Long 타입으로 UUID 대비 공간 50% 절약

---

#### 2️⃣ 종가 데이터 인메모리 캐싱 적용 - <ins>DB 부하 약 24% 감소</ins>

**📌 문제 상황**  
- 주문 시 전일 종가 확인을 위해 **매번 DB 조회** 발생  
- 종목 수가 900개 이상으로 증가하며 **성능 병목** 발생

**✅ 개선 방향**  
- 서버 구동 시 종가 데이터를 **인메모리 Map**에 캐싱  
- 주문 검증 시 DB가 아닌 메모리에서 직접 조회  
- 종가 데이터는 고정값이므로 재시작 전까지 안전하게 사용 가능

**📉 개선 전**
<img width="515" alt="DB findAll 기반 종가 조회" src="https://github.com/user-attachments/assets/68d4f328-eaaf-4c13-9689-9896ef3ca129" />

**📈 개선 후**
<img width="517" alt="인메모리 캐싱 적용" src="https://github.com/user-attachments/assets/4032895f-9b6f-42f0-a494-44b737582506" />

---

#### 3️⃣ 서버 간 통신 속도 개선 - <ins>gRPC 적용</ins>

**📌 문제 상황**  
- 기존 RESTTemplate (HTTP/1.1) 기반의 동기 통신 구조  
  - TCP 3-way handshake  
  - JSON 직렬화 / 역직렬화 비용  
  - 과도한 헤더 크기로 인한 지연

**✅ 개선 방향**  
- **gRPC (HTTP/2 기반)** 적용  
  - 직렬화 비용 절감 (바이너리 기반)  
  - TCP 연결 재사용으로 성능 향상  

**⏱ 성능 비교**

- **RestTemplate (HTTP / 1.1)**  
  <img width="615" alt="RestTemplate(http / 1.1)" src="https://github.com/user-attachments/assets/90154579-82a6-4247-9be5-b316aed5a548" />

- **RestTemplate (HTTP / 2)**  
  <img width="625" alt="RestTemplate(http / 2)" src="https://github.com/user-attachments/assets/4fff0191-325f-4871-bf5c-4c3d19222c42" />

- **gRPC**  
  <img width="669" alt="gRPC" src="https://github.com/user-attachments/assets/7e24f753-398a-455a-98d4-34adda48ffea" />
