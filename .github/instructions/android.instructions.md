# Instructions for Android Frontend

You are an Expert Android Architect. When generating code, refactoring, or answering questions related to the `drc-app/` directory, you **MUST** adhere strictly to the following rules. Failure to do so will break the project's Clean Architecture.

## General Rules

1. **Architecture:** Follow the MVVM pattern and Clean Architecture. Always separate business logic into ViewModels and data fetching into Repositories.
2. **Libraries:** Use Retrofit for API calls, Room for local database, and Dagger-Hilt for dependency injection.
3. **Context Awareness:** Before implementing a feature, always refer to `AidBridge/docs/main/requirements.md` for business rules, `AidBridge/docs/main/tech_stack.md` for tool constraints, and `AidBridge/docs/main/project_structure.md` for architectural decisions.
4. **Explanation:** Provide brief, clear explanations in Vietnamese for the generated code.
5. **Code Style:** Follow standard Java code conventions and Android best practices. Use meaningful variable names and include comments where necessary.
6. **Error Handling:** Implement proper error handling and user feedback for network requests and database operations.
7. **Performance:** Optimize for performance and battery efficiency, especially for location tracking features.
8. **Security:** Ensure secure handling of user data and API keys, following best practices for Android security.
9. **UI/UX:** Ensure that the UI is user-friendly and responsive across different screen sizes. Follow Material Design guidelines where applicable.

## 1. Absolute Tech Stack Constraints

- **Language:** Write ALL code in Java (Java 17). **ABSOLUTELY NO KOTLIN.**
- **UI Toolkit:** Use standard XML Layouts + ViewBinding. **ABSOLUTELY NO JETPACK COMPOSE.**
- **Libraries:** Dagger-Hilt (DI), Retrofit (Network), Room (Local DB), Jetpack Navigation Component (Routing).

## 2. Architecture Rules (Refer to `AidBridge/docs/frontend_guide.md`)

You must follow the established MVVM + Clean Architecture flow:
`Fragment -> ViewModel -> UseCase -> Repository Interface -> Repository Impl -> API/Local`

- **Dumb Fragments:** Fragments MUST NOT contain business logic, data mapping, or validation logic. They only bind UI and forward intents.
- **ViewModel & LiveData:** ViewModels must expose state via `LiveData`. API calls must be triggered using `Transformations.switchMap`.
- **Event Handling:** ALWAYS use the `NetworkResultWrapper.hasBeenHandled()` pattern with `BaseFragment.resultObserver()` to handle one-time UI events (Navigation, Toasts) to prevent duplicate triggers on backstack navigation.
- **Safe Navigation:** Never use raw `navController.navigate()`. Always use `navigateSafely()` and `popBackStackSafely()` from `BaseFragment`.

## 3. UI and Resource Conventions (STRICT)

Do not hardcode anything in XML or Java files.

- **Icons & Images:**
  - **First choice:** Use Android's built-in system icons (`@android:drawable/ic_...`) wherever a standard icon fits (e.g., location, menu, person) to save APK size.
  - **Second choice:** If a custom icon is needed, generate an XML Vector Drawable and place it in the correct feature resource folder (e.g., `res-common-ui/drawable/`).
- **Colors & Themes:** - NEVER hardcode hex colors (e.g., `#FF0000`).
  - Always use colors defined in `colors.xml` (e.g., `@color/color_primary`, `@color/text_secondary`) or create new ones as needed.
  - Reuse predefined styles from `themes.xml` (e.g., `style="@style/Widget.AidBridge.Button.Primary"`) or create new ones as needed.
- **Strings & Typography:**
  - NEVER hardcode text strings in UI or Toasts. Always extract them to `strings.xml`, but need to comment them appropriately.
- **Dimensions:** - Use `dimens.xml` for margins, padding, and text sizes. (e.g., `@dimen/margin_large`, `@dimen/text_title`).

## 4. File Placement & Project Structure

The project uses feature-based resource sets. You must place files in their exact designated locations:

- **Java code:** Follow the `ui/<feature>`, `domain/usecase/<feature>`, `data/repository` structure.
- **XML Layouts & Drawables:** Place them in the specific `res-*` directories (`res-auth`, `res-guest`, `res-role-victim`, `res-common-ui`), NOT just the default `res` folder.

## 5. Context & Documentation Awareness

Before writing code or proposing solutions, you MUST read and align with the following documents:

- **`AidBridge/docs/frontend_guide.md`**: For the exact folder tree, layer responsibilities, and coding standards.
- **`AidBridge/docs/frontend_plan.md`**: To know the current phase. **DO NOT generate Map, GPS, or WebSocket code if we are in Phase 1-4/**
- **`AidBridge/docs/main/requirements.md`**: For business logic, role behaviors, and terminology.
- **`AidBridge/docs/main/project_structure.md`**: For backend API alignment.

## 6. Output Format

- Provide brief, clear explanations in **Vietnamese**.
- Ensure generated code includes standard JavaDoc/comments in **English** for complex UseCase or Repository logic.
