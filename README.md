# Smart Factory SCADA

> 에너지 사용량 기반 스마트팩토리 통합 관제 시스템  
> 전기, 가스, 용수, 태양광 데이터를 수집하고 분석하여 사업장별 에너지 사용 현황과 ESG 경영 지표를 제공하는 Web SCADA 기반 대시보드 프로젝트입니다.

<br/>

## 1. 프로젝트 소개

Smart Factory SCADA는 제조 사업장의 에너지 데이터를 통합 관리하기 위한 스마트팩토리 관제 시스템입니다.

Node-RED 기반 DAS 서버에서 생성한 더미 계측 데이터를 MQTT로 전달하고, Spring Boot 백엔드가 데이터를 수신·저장·집계합니다.  
프론트엔드는 Vue 기반 대시보드로 구성되어 사업장별 에너지 사용량, 설비별 조회, 피크 전력, 가스/용수 사용량, ESG 평가 지표, 알림, AI 상담 기능을 제공합니다.

<br/>

## 2. 주요 기능

- 사업장별 에너지 종합 현황 조회
- 설비/라인별 에너지 사용량 분석
- 전기, 가스, 용수, 태양광 데이터 관리
- MQTT 기반 실시간 에너지 데이터 수집
- WebSocket 기반 실시간 대시보드 업데이트
- 피크 전력 모니터링 및 전기요금 추정
- ESG 평가 지표 및 사업장 등급 화면 제공
- 알림 발생/처리/삭제 기능
- AI 상담 챗봇 기능
- Web SCADA 화면 연동
- 사용자 관리 및 인증/인가 기능

<br/>

## 3. 기술 스택

| 영역 | 기술 |
|---|---|
| Frontend | Vue, JavaScript, CSS |
| Backend | Java, Spring Boot |
| Database | MySQL |
| Realtime | MQTT, WebSocket |
| Cache/Auth | Redis |
| Data Source | Node-RED DAS Server |
| External UI | Web SCADA / SMWP |
| Deploy | Render, Vercel |
| AI | OpenAI API 기반 상담 기능 |

<br/>

## 4. 프로젝트 구조

```text
smart-factory-scada
├── backend      # Spring Boot 백엔드 서버
├── frontend     # Vue 기반 대시보드 화면
├── database     # DB 초기화 및 SQL 관련 파일
├── smwp         # Web SCADA 연동 관련 코드
├── .github      # GitHub 설정
├── README.md
├── final-report-draft.md
└── real node-red.json
````

<br/>

## 5. 팀원 역할

| 이름 | 담당 영역                                                        | 주요 기여                                                                                                                                                                                                                                                               |
| ------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 한윤지              | Backend / Dashboard API / Realtime / Deploy                  | Spring Boot 백엔드 초기 구조를 구성하고, 사업장·설비·에너지·ESG·알림 API를 구현했습니다. MQTT 구독, Redis 최신 에너지 캐시, WebSocket 실시간 에너지 전송, 에너지 집계 배치, 설비 상세 조회, 피크 전력 모니터링, ESG 환경 대시보드 API 등을 담당했습니다. 또한 Vue 대시보드와 백엔드 API 연결, 실시간 에너지 화면, Google Map 기반 사업장 지도, Render/Vercel 배포 준비 작업에도 기여했습니다. |
| 강성재                  | Frontend / Web SCADA / Realtime Dashboard / Alarm·Chatbot UI | Vue 기반 프론트엔드 기본 UI와 Web SCADA 라우팅 및 연동을 담당했습니다. Web SCADA 화면 오류 수정, 차트 수정, 백엔드 주소 수정, 팝업창 및 로그인 화면 개선, 대시보드 실시간 화면 수정, 알람 처리/삭제, 챗봇 UI 개선, 피크 전력 및 챗봇 화면 기능 보완에 기여했습니다.                                                                                             |
| 조혜원             | Auth / User Management / AI Chat / Peak·Utility Dashboard    | 인증/인가와 사용자 관리 기능을 중심으로 백엔드 API를 구현했습니다. 회원가입, 로그인, AccessToken 인증 필터, RefreshToken 재발급, 로그아웃, 사용자 목록/상세/수정 API, Swagger 설정을 담당했습니다. 또한 AI 상담 기능, OpenAI Responses 호출, 설비 운영 문맥 기반 답변, 가스/용수 단위 기간 탭, 피크 전력 비교, 전기요금 계산 API 및 요금제 비교 기능을 구현했습니다.                     |
| 홍원희              | Alarm Feature / Sorting / Keyword Processing                 | 알람 기능 고도화를 담당했습니다. 알람을 키워드화하고, 최근순/오래된순 정렬 기능을 추가하여 관리자가 발생 알림을 더 쉽게 확인하고 처리할 수 있도록 개선했습니다.                                                                                                                                                                         |

<br/>

## 6. 시스템 흐름

```text
[Node-RED DAS Server]
- 사업장/라인/설비별 더미 계측 데이터 생성
        |
        v
[MQTT Broker]
- 전력, 가스, 용수, 태양광 데이터 발행/구독
        |
        v
[Spring Boot Backend]
- MQTT 데이터 수신
- DB 저장 및 집계
- 알림/피크/ESG 분석
- WebSocket 실시간 전송
        |
        v
[Vue Dashboard]
- 에너지 종합 화면
- 설비별 조회
- 피크 전력 화면
- 가스/용수 화면
- ESG 평가 지표
- AI 상담 및 알림 관리
        |
        v
[Web SCADA / SMWP]
- 외부 관제 화면 연동
```

<br/>

## 7. 프로젝트를 통해 배운 점

이 프로젝트를 통해 스마트팩토리 시스템은 단순히 데이터를 보여주는 화면이 아니라, 데이터 생성·수집·저장·분석·시각화가 하나의 흐름으로 연결되어야 한다는 점을 배웠습니다.

특히 MQTT를 통해 실시간 계측 데이터를 수신하고, WebSocket으로 대시보드에 전달하면서 실시간 관제 시스템에서 데이터 처리 속도와 화면 반영 구조가 중요하다는 것을 경험했습니다.

또한 전력, 가스, 용수, 태양광 데이터를 단순 조회하는 데 그치지 않고, 피크 전력, 전기요금, ESG 지표, 알림, AI 상담으로 확장하면서 운영자가 의사결정에 활용할 수 있는 형태로 데이터를 가공하는 경험을 할 수 있었습니다.

<br/>

## 8. 한 줄 회고

Smart Factory SCADA는 에너지 데이터를 실시간으로 수집·분석하고, Web SCADA와 Vue 대시보드를 통해 스마트팩토리 운영과 ESG 관리를 지원하는 통합 관제 시스템입니다.


