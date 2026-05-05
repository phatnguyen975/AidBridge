# AI USAGE DECLARATION - GROUP 04 (Viper Coder)

**Project:** Disaster Relief Coordinator (DRC)

---

## WEEK 05 (2026-03-02 – 2026-03-08)

### Nguyễn Thành Tiến / Nguyễn Thanh Gia Bảo / Đặng Đăng Khoa / Lê Tuấn Lộc / Nguyễn Tấn Phát

- Stitch AI. Gemini 3 Flash, Google. Accessed 09:34 on March 2, 2026. Prompt: "Bạn hãy thiết kế màn hình SOS khẩn cấp cho ứng dụng di động cứu trợ thiên tai. Màn hình phải có nút SOS lớn nhấp nháy ở giữa và yêu cầu nhập liệu tối thiểu để người dùng có thể nhanh chóng gửi yêu cầu khẩn cấp." Used to assist the UI/UX design of the Victim module; AI generated the wireframe layout and SOS button positioning; student revised the button placement for the "Thumb Zone" and validated against Android Material Design. **Evidence:** [Link to Stitch AI](https://stitch.withgoogle.com/projects/10007178543029810217)

- Stitch AI. Gemini 3 Flash, Google. Accessed 09:40 on March 2, 2026. Prompt: "Bạn hãy chỉnh sửa giao diện SOS khẩn cấp: loại bỏ phần bằng chứng hình ảnh và làm cho nút SOS có hình tròn với hiệu ứng nhấp nháy." Used to simplify the emergency reporting flow; AI generated the refined circular design and pulsing animation cues; student manually added long-press logic to prevent accidental SOS triggers. **Evidence:** [Link to Stitch AI](https://stitch.withgoogle.com/projects/10007178543029810217)

- Stitch AI. Gemini 3 Flash, Google. Accessed 14:12 on March 4, 2026. Prompt: "Thiết kế màn hình đăng nhập bằng email và mật khẩu với bố cục gọn gàng và tông màu xanh dương cho ứng dụng di động cứu trợ thiên tai." Used to assist the authentication interface design; AI generated the color scheme and form layout; student integrated the user role selection (Victim, Volunteer, Sponsor) to match the project architecture. **Evidence:** [Link to Stitch AI](https://stitch.withgoogle.com/projects/10007178543029810217)

- Stitch AI. Gemini 3 Flash, Google. Accessed 22:50 on March 4, 2026. Prompt: "Bạn hãy tạo màn hình thông báo nhiệm vụ tình nguyện với bộ đếm ngược 30 giây và các nút Chấp nhận/Từ chối khi có nhiệm vụ cứu hộ mới xuất hiện." Used to prototype the task notification system; AI generated the countdown UI and button styles; student adjusted the timer to 60 seconds to comply with the project's technical specifications. **Evidence:** [Link to Stitch AI](https://stitch.withgoogle.com/projects/10007178543029810217)

- Stitch AI. Gemini 3 Flash, Google. Accessed 13:45 on March 6, 2026. Prompt: "Thiết kế màn hình bản đồ điều hướng hiển thị tuyến đường từ trung tâm cứu trợ đến vị trí nạn nhân, kèm theo thanh tiến trình thể hiện các bước thực hiện nhiệm vụ." Used to create the navigation interface for volunteers; AI generated the route polyline and progress indicators; student validated the mission step sequence against the backend logistical workflow. **Evidence:** [Link to Stitch AI](https://stitch.withgoogle.com/projects/10007178543029810217)

---

## WEEK 06 (2026-03-09 – 2026-03-15)

### 1. Nguyễn Thành Tiến

- Gemini Pro. Google. Accessed 14:20 on March 11, 2026. Prompt: "Dựa trên các file requirements và structures tôi đã cung cấp trước đó. Bạn hãy thiết kế màn hình đăng nhập cho App như hình." Used to assist the UI/UX design of the Authentication module; AI generated the initial login screen wireframe including input fields for email/username and password, login button placement, and basic layout structure; student refined spacing, alignment, and component hierarchy to better follow Android Material Design guidelines. **Evidence:** [Link to Screenshot](https://drive.google.com/drive/folders/1La354Hv8pEMvy6oXG2sQUFchaMlsAED7?usp=sharing)

### 2. Nguyễn Thanh Gia Bảo

- Gemini Pro. Google. Accessed 9:15 on March 10, 2026. Prompt: "Đóng vai là một Database Architect. Dựa trên các Data Points và Business Rules đã phân tích từ các luồng hệ thống (ví dụ: Người dùng nặc danh, Tình nguyện viên, các hoạt động liên quan), hãy thiết kế cơ sở dữ liệu quan hệ cho các module của hệ thống. Chuẩn hóa dữ liệu để tránh dư thừa, xác định rõ Primary Key (PK) và Foreign Key (FK), và tạo một sơ đồ Entity Relationship Diagram (ERD) ở mức Physical Data Model bằng cú pháp Mermaid (sử dụng erDiagram). Trong sơ đồ cần ghi rõ kiểu dữ liệu của các thuộc tính và thể hiện đầy đủ các mối quan hệ giữa thực thể (1-1, 1-N, N-N)." Used to assist the database design phase; AI suggested normalized entities, primary and foreign key relationships, and produced a sample Mermaid ER diagram; student reviewed the schema, refined entity definitions, adjusted relationships, and modified attribute structures to ensure consistency with the final application modules. **Evidence:** [Link to Screenshot](https://drive.google.com/file/d/11nNcumYNGViy2JyzbwoPcQI-SsuKz1Kc/view?usp=sharing)

- Gemini Pro. Google. Accessed 16:48 on March 13, 2026. Prompt: "Đóng vai là một Database Architect. Dựa trên các Data Points và Business Rules đã được phân tích từ các luồng hệ thống (ví dụ: Người dùng nặc danh, Tình nguyện viên và các hoạt động liên quan), hãy thiết kế cơ sở dữ liệu quan hệ cho các module của hệ thống. Yêu cầu chuẩn hóa dữ liệu (Normalization) để tránh dư thừa, xác định rõ Primary Key (PK) và Foreign Key (FK), và tạo một sơ đồ Entity Relationship Diagram (ERD) ở mức Physical Data Model bằng cú pháp Mermaid (erDiagram). Trong sơ đồ cần ghi rõ kiểu dữ liệu của các thuộc tính và thể hiện đầy đủ các mối quan hệ giữa các thực thể (1–1, 1–N, N–N)." Used to assist the database architecture and schema design; AI generated a normalized data model with defined keys and a Mermaid ER diagram; student reviewed the generated schema, refined entity relationships, and adjusted attributes to ensure consistency with system modules. **Evidence:** [Link to Screenshot](https://drive.google.com/file/d/1IOO6GxiIW6KrUPIa-RQtacxdHsQcqIGF/view?usp=drive_link)

### 3. Nguyễn Tấn Phát

- Gemini Pro. Google. Accessed 21:35 on March 12, 2026. Prompt: "Trên là màn hình đăng ký của app Cứu trợ, bạn hãy hướng dẫn tôi code Android UI cho màn hình. Sử dụng Java + XML để code mobile." Used to assist the implementation of the Registration screen; AI generated a sample Android XML layout and Java activity code for handling user input fields; student adjusted the UI components, validation logic, and layout constraints to match the final design and ensure compatibility with Android Material Design. **Evidence:** [Link to Screenshot](https://drive.google.com/drive/folders/1RVqV8rRVIbbAvPowAsTRRubj-6FdEpST?usp=sharing)

### 4. Lê Tuấn Lộc

- GitHub Copilot, GitHub (Microsoft). Accessed 21:35 on March 19, 2026. Prompt: "Act as an expert API Architect and Technical Writer. I will provide you with a requirements document for a software project. Based on these requirements, I need you to design a clean, scalable RESTful API and document it. CRITICAL INSTRUCTION: Output the documentation strictly in valid OpenAPI 3.0 YAML format. Do not use standard Markdown text formatting for the API endpoints. The output must be pure YAML code. Please follow these strict RESTful best practices within the OpenAPI specification: Resource Naming: Use plural nouns for resource URLs (e.g., /users, not /getUser). HTTP Methods: Use GET for reading, POST for creation, PUT/PATCH for updating, and DELETE for removal. Organization: Group the APIs logically using OpenAPI tags. Status Codes: Include standard HTTP status codes for both success (200, 201, 204) and errors (400, 401, 403, 404, 500). Reusability: Define your JSON request and response bodies in components/schemas and reference them using $ref. Security: Define the authentication method (e.g., Bearer Token) in components/securitySchemes and apply it to the protected endpoints." Used to generate a well-structured, production-ready RESTful API specification in OpenAPI 3.0 YAML format from the software requirements. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1l4Qhoj2W9XI5WtYe1Ps8ulHlTN96RHiS?usp=drive_link)

### 5. Đặng Đăng Khoa

- Gemini Pro. Google. Accessed 19:05 on March 17, 2026. Used to assist the implementation of the Authentication and Authorization module featuring Role-Based Access Control (RBAC). **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1H-Q2BE4y6IuRZCJZtWR8EzIXWFlJwZyh?usp=sharing)

---

## WEEK 07 (2026-03-16 – 2026-03-22)

### 1. Nguyễn Thành Tiến

- Gemini Pro. Google. Accessed 8:45 on March 17, 2026. Prompt: "Create an XML Layout for **fragment_volunteer_dashboard.xml** using MaterialComponents, a VolunteerDashboardFragment.java inheriting BaseFragment with ViewBinding, and a @HiltViewModel. Implement an Online/Offline toggle logic using a mock UseCase, ensuring a 'Dumb Fragment' pattern and following the predefined project structure." Used to develop the core Volunteer module; AI generated the Material UI layout, the Fragment boilerplate with ViewBinding, and the ViewModel architecture including state management for the Online/Offline toggle; student validated the Hilt dependency injection setup, refined the XML constraints for multi-device compatibility, and ensured the business logic was correctly isolated within the ViewModel. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1kBgPRhD7nyF15V8jq-LJNIYGX9Zyp5qK?usp=sharing)

### 2. Nguyễn Thanh Gia Bảo

- GitHub Copilot, GitHub (Microsoft). Accessed 19:35 on March 20, 2026. Prompt provided the full database schema for `sos_requests` and `missions` tables and requested backend API implementation for the SOS submission button. Used to generate backend endpoints integrating tightly with the database schema, enabling efficient SOS handling and mission coordination within the system. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1eVptd2J8FdfefPGx_kD-dyNlWoGzCap4?usp=drive_link)

- Claude Sonnet 4.6, Anthropic. Accessed 20:35 on March 20, 2026. Prompt presented a runtime database error: `ERROR: column "status" is of type sos_status but expression is of type character varying`, encountered during an INSERT operation into the `sos_requests` table. Used to diagnose and fix a type mismatch issue between the application layer and the PostgreSQL database schema related to enum handling. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1eVptd2J8FdfefPGx_kD-dyNlWoGzCap4?usp=drive_link)

### 3. Đặng Đăng Khoa

- Claude Opus 4.5, Anthropic. Accessed 19:05 on March 17, 2026. Used to assist the implementation of the Authentication and Authorization module, covering the full user lifecycle including registration, secure login/logout, JWT refresh tokens, and OTP-verified password recovery for five distinct user roles. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1H-Q2BE4y6IuRZCJZtWR8EzIXWFlJwZyh?usp=sharing)

### 4. Lê Tuấn Lộc

- GitHub Copilot, GitHub (Microsoft). Accessed 21:35 on March 19, 2026. Prompt requested production-ready RESTful API design documented strictly in OpenAPI 3.0 YAML format, following best practices for resource naming, HTTP methods, status codes, and Bearer Token security. Used to design clean, scalable API documentation from the project's requirements. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1l4Qhoj2W9XI5WtYe1Ps8ulHlTN96RHiS?usp=drive_link)

### 5. Nguyễn Tấn Phát

- Gemini Pro. Google. Accessed 17:05 on March 16, 2026. Prompt: "Act as a Principal Android Architect to implement Phase 2 (Core UI & Navigation) for the Victim Profile screen. Generate a Navigation Graph with defined actions, a dark-themed XML layout using Material Design cards, and a 'Dumb Fragment' Java class using ViewBinding and BaseFragment utilities. Adhere to strict directory paths: res-role-victim/layout/ and ui/main/fragment/victim/, following Java 17 standards." Used to architect the navigation and profile interface for the Victim module; AI generated the navigation graph with safe actions, the complex XML layout with custom semantic color references, and the Fragment boilerplate with centralized click handling; student validated the navigation IDs against the global nav_graph, refined the XML constraints for the profile header's avatar badge, and ensured all static strings were correctly externalized to strings.xml. **Evidence:** [Link to Prompt](https://drive.google.com/file/d/1aTJ8r1ao23Px8u0J09BOEQihoaX4ENr2/view?usp=drive_link)

---

## WEEK 08 (2026-03-23 – 2026-03-29)

### 1. Nguyễn Thành Tiến

- Gemini Pro. Google. Accessed 21:30 on March 25, 2026. Prompt: "Create a Material Design 3 task history card XML (item_mission_history.xml) featuring a MaterialCardView with 12dp corners, a 16:9 ShapeableImageView, and specific AidBridge naming conventions; design the fragment_volunteer_history.xml layout with a Toolbar, horizontal Filter Chips, and a RecyclerView; and implement the VolunteerHistoryFragment.java using Java, MVVM + Clean Architecture, and ViewBinding. The Fragment must inherit from BaseFragment, include a static inner MissionHistory POJO, a Glide-integrated RecyclerView Adapter, and Log.d logic for ChipGroup selection while remaining a 'dumb' component with a TODO for ViewModel injection and LiveData observation." Used to develop the task tracking module for the Volunteer app; AI generated the Material3 XML layouts with constraint-based positioning, the Java Fragment boilerplate with internal Adapter logic, and the Glide image loading integration; student validated the XML resource references (@string/ and @color/), refined the Material3 Chip styles for filtering, and ensured architectural separation between UI and business logic layers. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1ODxRZgEIianOVe0Uc8aTUSBPVcDVZlgE?usp=sharing)

### 2. Nguyễn Thanh Gia Bảo

- GitHub Copilot, GitHub (Microsoft). Accessed 17:27 on March 25, 2026. Prompt requested implementation of `POST /aid-requests` endpoint in Java, based on the provided `aid_requests` and `aid_request_items` table schemas. Used to build an endpoint that handles the creation of aid requests along with their associated requested items. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/11KF2mkHH9B06BWuJd4IeYcQdJ2SvxCUy?usp=drive_link)

### 3. Đặng Đăng Khoa

- Claude Opus 4.5, Anthropic. Accessed on March 25, 2026. Used to implement a complete Mission API module covering six core endpoints (List Missions, Get Mission Detail, Confirm Pickup, Complete Mission, Cancel Mission, Get Mission Tracking), with Redis caching (5-minute TTL for active missions, 30-second TTL for tracking data), and Firebase Cloud Messaging (FCM) integration for mission workflow notifications. AI also assisted in building supporting DTOs, repository queries, controller routes, and a Postman collection for testing. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1QlZv7d7RtTyt2d-gAY6_47xpPHXXBhNy?usp=drive_link)

### 4. Lê Tuấn Lộc

- GitHub Copilot, GitHub (Microsoft). Accessed 21:35 on March 27, 2026. Prompt: "Task: Implement a REST API endpoint GET /volunteers/profile for the AidBridge project. Technical Stack: Java, Spring Boot 3, Spring Data JPA, MapStruct (for mapping), and Lombok. Architecture Requirements: Follow a strict Layered Architecture (Controller -> Service -> Repository)..." Used to implement the volunteer profile API following Clean Architecture principles with Layered Architecture, MapStruct mapping, and custom exception handling. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1AAa5mWSTopItYtffCuQAwM6HjTK5ZFz5)

### 5. Nguyễn Tấn Phát

- Copilot, Model GPT-5.3-Codex. Accessed on March 22, 2026. **Prompt 1:** Requested implementation of the Victim Account Settings screen (`VictimPersonalInfoFragment`), including a Profile Info card with avatar, name, phone, email, and address fields, and a Change Password card, using Java 17 + ViewBinding, dark Material theme, and strict resource-set separation under `res-role-victim/`. AI generated the XML layout and Fragment boilerplate; student validated resource references and ensured architectural compliance. **Evidence:** [Link to Evidence](https://drive.google.com/drive/folders/1YI2CrUqtI0p5u2mt6oNUVNhDmppKppL4)

- Copilot, Model GPT-5.3-Codex. Accessed on March 22, 2026. **Prompt 2:** Requested implementation of the Victim SOS History screen (`VictimHistoryFragment`) with a filter dropdown, paginated RecyclerView, dynamic icon adapter for three history types (SOS Self, Supply, SOS Relative), a Bottom Sheet detail view, and a commented-out API integration block following the project's `NetworkResultWrapper` pattern. **Evidence:** [Link to Evidence](https://drive.google.com/drive/folders/1YI2CrUqtI0p5u2mt6oNUVNhDmppKppL4)

- Copilot, Model GPT-5.3-Codex. Accessed on March 22, 2026. **Prompt 3:** Requested implementation of the SOS container and custom Bottom Navigation behavior for the Victim role, including a 3-item bottom nav menu (Profile, SOS, Map), an intercept logic for the SOS tab to display a `PopupMenu` with "Cho bản thân" / "Cho người thân" options, a `ViewPager2` container fragment with `TabLayout`, and a `FragmentStateAdapter` for the Rescue and Supply tabs. **Evidence:** [Link to Evidence](https://drive.google.com/drive/folders/1YI2CrUqtI0p5u2mt6oNUVNhDmppKppL4)

---

## WEEK 09 (2026-03-30 – 2026-04-05)

### 1. Nguyễn Thành Tiến

- Gemini Pro. Google. Accessed 8:35 on April 01, 2026. Prompt requested creation of `VolunteerDeliveryMissionFragment.java` managing a 4-phase delivery logic with state management (currentStep 1–4), `updateUIByStep()` to synchronize ProgressBar and Timeline views, click handlers for `btnAction` and `btnSeeDetails`, navigation logic using SharedViewModel (`VolunteerTaskViewModel`), and mock data for volunteer and supply items. Used to develop the delivery mission flow for the Volunteer module. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1ndWhmxF6KFpFB78xhG-K1tEqSepdhyHN?usp=sharing)

- Gemini Pro. Google. Accessed 10:00 on April 01, 2026. Prompt requested creation of an XML layout for the "Vật phẩm chi tiết" (Supply Detail) screen for Volunteer, including a MaterialToolbar header, a large QR code card, and a hierarchical MaterialCardView with 5 expandable categories (Thuốc, Quần áo, Thức ăn, Nước uống, Khác) each showing item names, quantities, and units, wrapped in a NestedScrollView. Used to design the supply item detail interface. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1ndWhmxF6KFpFB78xhG-K1tEqSepdhyHN?usp=sharing)

### 2. Nguyễn Thanh Gia Bảo

- GitHub Copilot, GitHub (Microsoft). Accessed 20:13 on April 02, 2026. Prompt: "Hãy đóng vai trò là một Backend Engineer và AI Engineer hãy giúp tôi tích hợp AI speech to text (Whisper) để chuyển đoạn ghi âm qua text thông qua API: POST /aid-requests/voice." Used to design and implement an API endpoint that leverages Whisper to convert uploaded voice recordings into text for downstream processing. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1Lx8gaWRN0m7EM6Qat2sqjuNVT-mj3Lrv?usp=drive_link)

- GPT (OpenAI). Accessed 16:53 on April 03, 2026. Used to guide the refactoring of the system into a modular monolith architecture, with a focus on implementing event-driven design using event listeners to improve modularity and decoupling. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1Lx8gaWRN0m7EM6Qat2sqjuNVT-mj3Lrv?usp=drive_link)

- GitHub Copilot, GitHub (Microsoft). Accessed 13:50 on April 03, 2026. Prompt provided the new `sos_requests` table schema using PostGIS `geography` type and requested refactoring of the SOS request domain to replace `lat/lng` numeric columns with the PostGIS `location` field. Used to guide the refactoring of SOS request handling after the database was redesigned using PostGIS. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1Lx8gaWRN0m7EM6Qat2sqjuNVT-mj3Lrv?usp=drive_link)

### 3. Đặng Đăng Khoa

- Claude Opus 4.5, Anthropic. Accessed on March 25, 2026. Prompt requested analysis and fixes for two bugs: (1) a `lat/lng` migration failure caused by Hibernate `ddl-auto: update` attempting to add `NOT NULL` columns to a table with existing rows, and (2) a `EXTRACT(EPOCH FROM ...)` HQL query failure in Hibernate 7 due to a breaking change where subtraction of two `Instant` values returns `Duration` rather than a `TEMPORAL` type. AI performed root cause analysis, identified files to patch, proposed code fixes, and listed risks and a post-fix verification checklist. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1QlZv7d7RtTyt2d-gAY6_47xpPHXXBhNy?usp=drive_link)

### 4. Lê Tuấn Lộc

- Antigravity (Claude Opus 4.6), Anthropic. Accessed on March 30, 2026. Prompt: "Act as an expert Spring Boot Architect. I am migrating my project from a traditional Layered Architecture to a Pragmatic Modular Monolith. Your task is to refactor ONLY the [DOMAIN_NAME] domain..." Used as a guideline template for refactoring each domain module into the Modular Monolith architecture, with clear separation between the public Facade interface, public DTO, and strictly private internal layers (web, usecase, mapper, entity, repository). This prompt aimed to dismantle God Class services, create single-responsibility Use Cases, and establish inter-module boundaries. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1lKlh60uFxmWC4DfEU9CiZWSZ2AJTVoE-?hl=vi)

### 5. Nguyễn Tấn Phát

- Copilot, Model GPT-5.3-Codex. Accessed on March 29, 2026. **Prompt 1:** Requested implementation of Part 1 of the Victim SOS Detailed UI, including resource files (`strings_victim.xml`, drawables), the `fragment_victim_rescue_tab.xml` layout featuring a circular SOS button, health info fields, multi-select image upload area with RecyclerView, and the `VictimRescueTabFragment.java` with a `Dumb View` extractDataAndSubmit() method. **Evidence:** [Link to Evidence](https://drive.google.com/drive/folders/1macA5PzBsPSCg0GuOdQVJqCzscwT3Q2K)

- Copilot, Model GPT-5.3-Codex. Accessed on March 29, 2026. **Prompt 2:** Requested implementation of Part 2 (Supply Tab) for the Victim SOS screen, including `fragment_victim_supply_tab.xml` with expandable food/water categories using toggle animations, steppers for adult/elderly/children counts, a voice recording card with a pulsing ObjectAnimator effect, and `VictimSupplyTabFragment.java` following strict Dumb View rules. **Evidence:** [Link to Evidence](https://drive.google.com/drive/folders/1macA5PzBsPSCg0GuOdQVJqCzscwT3Q2K)

- Copilot, Model GPT-5.3-Codex. Accessed on March 30, 2026. **Prompt 3:** Requested implementation of `VictimSosRelativeFragment` for sending SOS on behalf of another person, including a static map placeholder card, form fields for relative name/address/phone/severity, a submit button with disclaimer text, and a `RelativeSosFormInput` inner class following the Dumb View pattern with NO validation logic. **Evidence:** [Link to Evidence](https://drive.google.com/drive/folders/1macA5PzBsPSCg0GuOdQVJqCzscwT3Q2K)

---

## WEEK 10 (2026-04-06 – 2026-04-12)

### 1. Nguyễn Thành Tiến

- Gemini 3 Flash. Google. Accessed 13:45 on April 06, 2026. Prompt requested creation of the Admin AI Summary screen (`fragment_admin_ai_summary.xml`) featuring a header with update timestamp, a main report card with sub-metrics for cargo and people assisted, two warning alerts, a "Tạo báo cáo" button, a Recent Activities card with color-dot timeline items, and a Robot FAB for navigating to the AI Chatbot. Also requested `AdminAiSummaryFragment.java` following the `VolunteerDashboardFragment` pattern and `AdminAiSummaryViewModel.java` with a TODO for AI UseCase integration. Used to develop the AI-powered administrative summary module; student validated the resource-set isolation and ensured navigation and event handling aligned with existing BaseFragment patterns. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/13UI4Nt8W__UfBCABQaZ4aBVAFjYIs7Fc?usp=sharing)

- Gemini 3 Flash. Google. Accessed 21:00 on April 08, 2026. Prompt requested creation of the Admin AI Chatbot screen (`fragment_admin_ai_chatbot.xml`) with a header showing system online status, a RecyclerView chat list supporting two view types (User bubble on right in primary color, AI bubble on left with robot icon), suggestion chips, a bottom chat bar with attachment/voice/send buttons, and `AdminAiChatbotFragment.java` with `AdminAiChatbotViewModel.java` holding `MutableLiveData<List<ChatMessage>>` and a TODO for Gemini API integration. Used to develop the AI Chatbot interface for administrators; student validated res-role-admin alignment and refined the RecyclerView scroll behavior. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/13UI4Nt8W__UfBCABQaZ4aBVAFjYIs7Fc?usp=sharing)

### 2. Nguyễn Thanh Gia Bảo

- GitHub Copilot, GitHub (Microsoft). Accessed 15:07 on April 9, 2026. Prompt: "You are now a Backend Developer. I need you to completed these task: 1) Implement APIs: GET /hubs List hubs. 2) Implement APIs: POST /hubs Create new hub (Admin only); PATCH /hubs/{id} Update hub (Admin only)." Used to implement the Hub management APIs with role-based access control for Admin. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1DivpdP94L_HNyGdLDiXOSeGcDDfnhzE3?usp=drive_link)

- ChatGPT, OpenAI. Accessed 18:35 on April 8, 2026. Prompt: "You are now a Backend Developer and AI Engineer. I need you to complete these tasks: 1) Instruct me step-by-step to set up Ollama 2) Give me the code to implement the LLM Service for analyzing transcripts from Whisper using CLI." Used to set up the local LLM service and integrate it with the Whisper-based voice transcript pipeline. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1DivpdP94L_HNyGdLDiXOSeGcDDfnhzE3?usp=drive_link)

### 3. Đặng Đăng Khoa

- Claude Opus 4.5, Anthropic. Accessed on April 06–12, 2026. Prompt: "Hãy thực hiện flow chuyển trạng thái mission từ PENDING sang DISPATCHING theo kiến trúc đã chốt trước đó..." Used to implement the full PENDING → DISPATCHING mission transition flow, including `DispatchMissionUseCase` with SOS (broadcast) and Aid (sequential) strategies, creation of `dispatch_attempt` records, publication of `MissionDispatchCreatedEvent`, and post-commit listeners for FCM popup and WebSocket realtime events. AI also updated `AcceptMissionUseCase` and `GetMyMissionsUseCase`, extended `VolunteerFacade` for candidate selection, and added FCM dispatch notification methods. Student reviewed the generated flow, verified the `compileJava` build pass, and documented remaining work (reject/timeout → re-dispatch logic). **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1miavYaOY5mqH9IAOK53-VEjowylG0SI5?usp=drive_link)

### 4. Lê Tuấn Lộc

- Antigravity (Claude Opus 4.6), Anthropic. Accessed on April 06–12, 2026. Provided official GraphHopper `RoutingExample` source code as reference and requested review and refinement of the project's existing GraphHopper configuration. AI was tasked with identifying deviations in initialization flow, `.osm.pbf` file path, cache mechanism, and Profile/Algorithm setup causing runtime errors, rewriting corrected configuration code, and explaining the root cause of failures. Used to integrate routing logic based on GraphHopper and OpenStreetMap into the backend. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1wCtL-AO8lo1RwPuRU2LZZy19DG665Axf)

### 5. Nguyễn Tấn Phát

- Copilot, Model GPT-5.3-Codex. Accessed on April 06–12, 2026.
  - **Prompt 1:** [Link to Prompt 1](https://drive.google.com/file/d/1vYbnkevz84I2yCE7wiUP_mYb_nk2qa4a/view)
  - **Prompt 2:** [Link to Prompt 2](https://drive.google.com/file/d/1BtA-yGmTqqBHXypGHKNDgoR_ZGVi8oME/view)
  - **Prompt 3:** [Link to Prompt 3](https://drive.google.com/file/d/1sSL8TGBDY2kjNM5_ZzxIU8E-P6HQJGXb/view)
  - **Prompt 4:** [Link to Prompt 4](https://drive.google.com/file/d/1r7k2BKlxuaOwu6w9EdWvjopqzqOFiMrP/view)

  Used to implement all screens of the Sponsor role. **Artifacts:** [Link to Artifacts](https://drive.google.com/drive/folders/1b_njfSMnPAQuIW3djPuvFP_sBO3mxmA3)

---

## WEEK 11 (2026-04-12 – 2026-04-19)

### 1. Nguyễn Thành Tiến

- Copilot, Model ChatGPT-5.3-Codex. Accessed 17:00 on April 13, 2026. Used to implement the full API integration for the Volunteer role. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1UeBOs8h_Abu427-stczVWOGBpXxqG-4g?usp=drive_link)

- Copilot, Model ChatGPT-5.3-Codex. Accessed 9:45 on April 15, 2026. Used to implement the full API integration for the Volunteer role. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1UeBOs8h_Abu427-stczVWOGBpXxqG-4g?usp=drive_link)

- Copilot, Model ChatGPT-5.3-Codex. Accessed 14:30 on April 16, 2026. Used to implement the API integration for the Volunteer map screen. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1UeBOs8h_Abu427-stczVWOGBpXxqG-4g?usp=drive_link)

### 2. Nguyễn Thanh Gia Bảo

- GitHub Copilot, GitHub (Microsoft). Accessed 14:57 on April 13, 2026. Prompt: "You are a senior backend engineer. Implement a REST API POST /donations using Spring Boot." Provided the full `donations` and `donation_items` table schemas. Used to implement the donation creation endpoint handling one donation with many items, where `item_category_id` can be NULL for unclassified items. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1lE43_EGrljUYgTnn30qC-LJ0w2CbhVVP?usp=drive_link)

- ChatGPT, OpenAI. Accessed 15:07 on April 13, 2026. Prompt reported a runtime error: `column "status" is of type donation_status but expression is of type character varying`, occurring in a `donations` INSERT statement. Used to diagnose and resolve the PostgreSQL enum type mismatch in the Spring Boot application layer. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1lE43_EGrljUYgTnn30qC-LJ0w2CbhVVP?usp=drive_link)

- ChatGPT, OpenAI. Accessed 14:35 on April 17, 2026. Prompt reported a `DataIntegrityViolationException` with `invalid input value for enum donation_status: "READY_FOR_INBOUND"` during a `donations` UPDATE statement. Used to investigate and fix the incorrect enum value mapping in the update flow. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/1lE43_EGrljUYgTnn30qC-LJ0w2CbhVVP?usp=drive_link)

### 3. Đặng Đăng Khoa

- Used AI assistance to implement push notification to volunteers when a Mission is created, and to implement GPS retrieval from the user when sending a request. For the notification feature, AI described the full real-time push flow including mission metadata and deep-link behavior. For the GPS feature, AI outlined the permission-check, location-retrieval, and request-attachment logic. Student implemented and integrated both features into the existing codebase. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/19HHXbI9UI8EOpXu5bkyBjjflFLAckTMg?usp=drive_link)

### 4. Lê Tuấn Lộc

- Copilot, Model ChatGPT-5.3-Codex. Accessed on April 12–19, 2026. Prompt: "Role & Context: Act as an Expert Mobile UI/UX Designer. Create a modern, clean, and production-ready Bottom Sheet for an emergency disaster relief app named AidBridge..." Detailed specifications for an expandable Bottom Sheet in two states (collapsed: mission title, destination, distance/ETA, "BẮT ĐẦU ĐI" button; expanded: route inputs, "Tính lộ trình" button, route options, "Mô phỏng" ghost button), dark mode with navy/gray palette, and mobile-first Android proportions. Used to complete the map routing UI and customization. **Evidence:** [Link to Prompt](https://drive.google.com/drive/folders/12i6J-lxYWPSnUZM-x-aL8e84A-vWyZ3M)

### 5. Nguyễn Tấn Phát

- Copilot, Model ChatGPT-5.3-Codex. Accessed on April 12–19, 2026.
  - **Prompt 1:** [Link to Prompt 1](https://drive.google.com/file/d/1nzovDwXPtq5JI0bUPfOkn-QpG6-1xqsT/view)
  - **Prompt 2:** [Link to Prompt 2](https://drive.google.com/file/d/1Gb6ESjEbEGZ2RPyDhNb3RvsKFFXdv2A8/view)
  - **Prompt 3:** [Link to Prompt 3](https://drive.google.com/file/d/1nNO66oVquG8MD12p6JaW17I1LIqOc3c1/view)
  - **Prompt 4:** [Link to Prompt 4](https://drive.google.com/file/d/1ePAw8xEdvZz9s-M08s-mEn9j4qv3eLDs/view)
  - **Prompt 5:** [Link to Prompt 5](https://drive.google.com/file/d/1zpXSKewVlhqqu4HP4DtmdUFZimwfpZN/view)

  Used to implement all screens of the Staff role. **Evidence:** [Link to all Staff screens](https://drive.google.com/drive/folders/1uxOGDISS9U81Ynnl2O8YtvAvYzypCcZV)
