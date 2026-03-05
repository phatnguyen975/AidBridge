# Instructions for Spring Boot Backend

When generating code or answering questions related to the `spring-backend/` directory, you MUST adhere strictly to these rules:

1. **Core Framework:** Use Java 25 and Spring Boot 4.x features.
2. **Database & Geospatial:** The database is Supabase (PostgreSQL). You MUST use PostGIS for any location-based queries (e.g., finding nearby hubs, calculating distances). Use `hibernate-spatial` dependencies and appropriate spatial data types (`Point`, `Polygon`) in Entities.
3. **Coding Standards:**

- Keep controllers clean; put business logic in the `service` layer.
- Always use DTOs for Request/Response (never expose Entities directly).
- Validate DTOs using `@Valid` and Jakarta Validation.
- Use Spring Data JPA repositories for database access, and write custom queries for geospatial operations when needed.
- Follow the layered architecture: Controller -> Service -> Repository. Avoid circular dependencies and ensure proper separation of concerns.
- Follow standard naming conventions and code formatting for readability and maintainability. Use Lombok annotations (e.g., `@Data`, `@Builder`) to reduce boilerplate code where appropriate.
- Follow SOLID principles and design patterns (e.g., Strategy for different SOS handling strategies, Observer for real-time updates to volunteers).
- Always include JavaDoc comments for public methods and classes to explain their purpose and usage.
- Use meaningful variable and method names that clearly indicate their purpose. Avoid abbreviations unless they are widely understood in the context of the project.
- Ensure that your code is modular and reusable. Avoid hardcoding values; use configuration properties where applicable.
- Always handle edge cases and potential exceptions gracefully, providing informative error messages to the client. Use appropriate HTTP status codes in API responses to indicate success or failure of requests.
- When implementing new features or endpoints, consider the overall user experience and how it fits into the existing architecture. Ensure that new code integrates well with existing components and follows the established design patterns and conventions of the project.
- When writing database queries, especially for geospatial data, ensure that they are optimized for performance. Use indexes on spatial columns and consider the use of pagination for queries that may return large datasets. Avoid N+1 query problems by using appropriate fetching strategies in JPA.

4. **Context Awareness:** Before implementing an endpoint or logic, always refer to `AidBridge/docs/requirements.md` for business rules, `AidBridge/docs/tech_stack.md` for tool constraints, and `AidBridge/docs/project_structure.md` for architectural decisions.
5. **Explanation:** Provide brief, clear explanations in Vietnamese for the generated code.
6. **Testing:** Include unit tests for Services using JUnit and Mockito.
7. **Error Handling:** Implement proper error handling with custom exceptions and global exception handlers.
8. **Performance:** Optimize database queries, especially for geospatial operations. Use indexes on spatial columns and avoid N+1 query problems.
9. **Security:** Ensure secure handling of user data and API keys, following best practices for Spring Security. Implement authentication and authorization as needed.
