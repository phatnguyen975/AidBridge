# AidBridge - Thong tin can thiet de thuc thi chuong trinh

Tai lieu nay tong hop cac thong tin, tai khoan, va cau hinh bat buoc de chay AidBridge (backend + mobile + AI). Khong commit file chua secrets.

## 1) Thanh phan he thong

- Backend: Spring Boot (spring-backend)
- Frontend: Android app (drc-app)
- Ha tang: Supabase Postgres + PostGIS, Redis, SMTP mail, Firebase, Cloudinary, SMS gateway
- AI: Local Whisper (speech-to-text) + local LLM (Ollama) cho voice aid request

## 2) Phan mem va moi truong

Backend:
- Java 25 (theo spring-backend/build.gradle)
- Gradle Wrapper (da co trong repo)

Frontend:
- Android Studio + Android SDK
- Java 17 (compile/target theo drc-app/app/build.gradle)

AI (neu dung voice/LLM):
- Python 3.x va goi whisper (python -m whisper)
- Ollama CLI (model mac dinh: llama3)

Ha tang:
- Supabase (PostgreSQL + PostGIS)
- Redis (Upstash/Redis server)
- SMTP email (Gmail App Password hoac SMTP provider)
- Firebase (FCM) + file google-services.json
- Cloudinary (upload attachments)
- SMS gateway (token + so dien thoai gateway)

## 3) Cac file cau hinh bat buoc

### Backend (spring-backend)

- File mau: spring-backend/src/main/resources/application-local.example.yaml
- File thuc te (secrets): spring-backend/src/main/resources/application-local.yaml

Cac khoa can dien:
- spring.datasource.url
- spring.datasource.username
- spring.datasource.password
- spring.data.redis.url
- spring.mail.username
- spring.mail.password
- jwt.rsa.public-key (classpath:certs/public.pem)
- jwt.rsa.private-key (classpath:certs/private.pem)
- firebase.enabled
- firebase.project-id
- firebase.service-account.path
- cloudinary.cloud-name
- cloudinary.api-key
- cloudinary.api-secret
- aidbridge.gateway.sms.token

Ghi chu:
- application.yaml da set spring.profiles.active=local, nen application-local.yaml duoc su dung mac dinh.
- Can tao cap khoa RSA (public.pem, private.pem) va dat vao spring-backend/src/main/resources/certs/.

### Frontend (drc-app)

- File mau: drc-app/local.example.properties
- File thuc te (secrets): drc-app/local.properties
- File Firebase: drc-app/app/google-services.json (dung template google-services.example.json)

Cac khoa can dien trong local.properties:
- API_BASE_URL=http://<ip_address>:8080/api/
- MAPS_API_KEY=your_maps_api_key
- SOS_GATEWAY_PHONE_NUMBER=<staff_gateway_sim_phone_number>
- SMS_GATEWAY_TOKEN=dev-gateway-token
- SUPABASE_URL=https://<your-project>.supabase.co
- SUPABASE_ANON_KEY=<your-anon-key>

Ghi chu:
- Emulator dung API_BASE_URL mac dinh http://10.0.2.2:8080/api/
- Thiet bi that dung IPv4 cua may chay backend.

## 4) Co so du lieu va migration

- PostGIS bat buoc cho cac truy van dia ly.
- Chay migration trong docs/database:
  - postgis_migration.sql (them location geography + index)
  - sms_ingest_migration.sql (bo sung cot sms ingest)
  - seed_staff_inventory.sql (seed danh muc va ton kho)

Luu y:
- docs/database/schema.sql chi mang tinh tham khao (co canh bao), khong nhat thiet dung de run.

## 5) AI / LLM

Hien tai flow voice aid request su dung local CLI:
- Speech-to-text: LocalWhisperSpeechToTextService (python -m whisper)
- LLM extract items: LocalCliAidRequestVoiceLlmService (ollama run llama3)

Neu khong can AI/voice:
- Co the khong bat voice endpoint va bo qua cai dat Python/Ollama.

## 6) Tai khoan va dich vu ben ngoai

Bat buoc/khuyen nghi:
- Supabase project: DB + PostGIS, cap quyen user, lay URL/username/password
- Supabase anon key (frontend realtime)
- Redis URL
- SMTP email user + app password
- Firebase project (FCM) + service account json
- Google Maps API key
- Cloudinary credentials
- SMS gateway token + so dien thoai gateway (cho Quick SOS fallback)

## 7) Thong tin run co ban

Backend:
- Port mac dinh: 8080 (theo application.yaml)
- Can co data/vietnam.osm.pbf va thu muc graph-data de GraphHopper hoat dong

Frontend:
- Build + run tren emulator hoac thiet bi that
- Can co google-services.json va local.properties day du

## 8) Bao mat va commit

- Khong commit: application-local.yaml, local.properties, google-services.json, firebase service account, RSA keys.
- Su dung template mau de chia se thong tin cho team.
