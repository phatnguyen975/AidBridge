# Instructions for Android Frontend

When generating code or answering questions related to the `drc-app/` directory, you MUST adhere strictly to these rules:

1. **Language & UI Constraints:** 
  - Write ALL code in Java (Java 17). Absolutely NO Kotlin.
  - Use standard XML Layouts for UI. Absolutely NO Jetpack Compose.
2. **Architecture:** Follow the MVVM pattern and Clean Architecture. Always separate business logic into ViewModels and data fetching into Repositories.
3. **Libraries:** Use Retrofit for API calls, Room for local database, and Dagger-Hilt for dependency injection.
4. **Context Awareness:** Before implementing a feature, always refer to `AidBridge/docs/requirements.md` for business rules, `AidBridge/docs/tech_stack.md` for tool constraints, and `AidBridge/docs/project_structure.md` for architectural decisions.
5. **Explanation:** Provide brief, clear explanations in Vietnamese for the generated code.
6. **Code Style:** Follow standard Java code conventions and Android best practices. Use meaningful variable names and include comments where necessary.
7. **Error Handling:** Implement proper error handling and user feedback for network requests and database operations.
8. **Performance:** Optimize for performance and battery efficiency, especially for location tracking features.
9. **Security:** Ensure secure handling of user data and API keys, following best practices for Android security.
