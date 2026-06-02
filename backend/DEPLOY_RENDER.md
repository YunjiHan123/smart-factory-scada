# Render Backend Deployment

이 문서는 2일 정도의 단기 시연 배포를 기준으로 백엔드(Render) + MySQL(Aiven) + Redis(Aiven) + OpenAI 설정을 빠르게 점검하기 위한 가이드입니다.

## Render Service Settings

- Service type: Web Service
- Root Directory: `backend`
- Build Command: `./gradlew clean bootJar`
- Start Command: `java -jar build/libs/*.jar`
- Health Check Path: `/actuator/health`
- Runtime: Java 17

Render는 `PORT` 환경변수를 자동으로 주입합니다. 애플리케이션은 `server.port=${PORT:8080}` 설정으로 Render 포트를 사용합니다.

## Environment Variables

### Required

```properties
JWT_SECRET=<32-bytes-or-longer-secret>
JWT_ACCESS_TOKEN_EXPIRATION_MS=300000
JWT_REFRESH_TOKEN_EXPIRATION_MS=3600000

DB_URL=jdbc:mysql://<aiven-mysql-host>:<port>/scada?sslMode=REQUIRED&serverTimezone=Asia/Seoul&characterEncoding=utf8
DB_USERNAME=avnadmin
DB_PASSWORD=<aiven-mysql-password>

REDIS_HOST=<aiven-redis-host>
REDIS_PORT=<aiven-redis-port>
REDIS_PASSWORD=<aiven-redis-password>
REDIS_SSL_ENABLED=true
```

### Optional

```properties
OPENAI_API_KEY=<openai-api-key>
OPENAI_MODEL=gpt-5.4-mini
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_TIMEOUT_SECONDS=10
OPENAI_MAX_OUTPUT_TOKENS=600

MQTT_ENABLED=false
MQTT_BROKER_URL=tcp://localhost:1883
MQTT_CLIENT_ID=scada-backend
MQTT_TOPIC=scada/team5/plant/+/facility/+/energy
```

`OPENAI_API_KEY`가 비어 있어도 챗봇 API는 fallback 답변을 저장하고 정상 응답합니다. Node-RED/MQTT 시연이 필요한 경우에만 `MQTT_ENABLED=true`로 켭니다.

## Pre-demo Checks

1. Aiven MySQL `scada` 데이터베이스에 `database/init.sql`과 `database/seed-demo-data.sql`을 반영합니다.
2. Render 환경변수에 `DB_*`, `REDIS_*`, `JWT_*`, 필요 시 `OPENAI_*` 값을 설정합니다.
3. Render 배포 완료 후 `https://<render-service>.onrender.com/actuator/health`가 `UP`인지 확인합니다.
4. 로그인 API가 정상인지 확인합니다.
5. `/detail/chatbot`에서 질문을 보내 OpenAI 또는 fallback 답변이 표시되는지 확인합니다.

## Troubleshooting

- Health가 `DOWN`이면 MySQL JDBC URL, Redis host/port/password/SSL 값을 먼저 확인합니다.
- Aiven MySQL은 `sslMode=REQUIRED`를 JDBC URL에 포함해야 합니다.
- Aiven Redis는 일반적으로 SSL이 필요하므로 `REDIS_SSL_ENABLED=true`로 설정합니다.
- MQTT broker를 배포하지 않는다면 `MQTT_ENABLED=false`를 유지합니다.
