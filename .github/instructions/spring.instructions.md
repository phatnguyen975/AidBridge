# Instructions for Spring Boot Backend

When generating code or answering questions related to the `spring-backend/` directory, you MUST adhere strictly to these rules:

1. **Core Framework:** Use Java 25 and Spring Boot 4.x features.
2. **Database & Geospatial:** The database is Supabase (PostgreSQL). You MUST use PostGIS for any location-based queries (e.g., finding nearby hubs, calculating distances). Use `hibernate-spatial` dependencies and appropriate spatial data types (`Point`, `Polygon`) in Entities.
3. **Coding Standards:** - Keep controllers clean; put business logic in the `service` layer.
  - Always use DTOs for Request/Response (never expose Entities directly).
  - Validate DTOs using `@Valid` and Jakarta Validation.
4. **Context Awareness:** Before implementing an endpoint or logic, always refer to `AidBridge/docs/requirements.md` for business rules, `AidBridge/docs/tech_stack.md` for tool constraints, and `AidBridge/docs/project_structure.md` for architectural decisions.
5. **Explanation:** Provide brief, clear explanations in Vietnamese for the generated code.
6. **Testing:** Include unit tests for Services using JUnit and Mockito.
7. **Error Handling:** Implement proper error handling with custom exceptions and global exception handlers.
8. **Performance:** Optimize database queries, especially for geospatial operations. Use indexes on spatial columns and avoid N+1 query problems.
9. **Security:** Ensure secure handling of user data and API keys, following best practices for Spring Security. Implement authentication and authorization as needed.
