# Security Configuration Guide

The `SecurityConfig.java` file controls the global security settings. Here is a breakdown of the key configurations.

## 1. CORS (Cross-Origin Resource Sharing)

We explicitly whitelist the Angular Frontend to allow it to communicate with the Backend.

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:4200")); // Frontend URL
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
    configuration.setAllowCredentials(true);
    // ...
}
```
*   **Why?** Browsers block cross-domain requests by default (e.g., localhost:4200 -> localhost:8080).

## 2. CSRF (Cross-Site Request Forgery)

```java
http.csrf(AbstractHttpConfigurer::disable)
```
*   **Status**: **DISABLED**
*   **Why?** CSRF attacks exploit the browser's behavior of automatically sending cookies (like `JSESSIONID`). Since we use **JWTs** stored in `localStorage` (or memory) and explicitly send them in the `Authorization` header, the browser does not auto-send credentials. Therefore, CSRF protection is unnecessary and can be safely disabled for stateless APIs.

## 3. Session Management

```java
http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```
*   **Type**: **STATELESS**
*   **Why?** In a REST API, every request should be self-contained (carrying the JWT). The server should not store any state (HttpSession) between requests. This improves scalability.

## 4. Password Encoding

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```
*   **Algorithm**: **BCrypt**
*   **Usage**: When a user registers or is created, their raw password is hashed using specific iterations (default is 10). When logging in, the raw password entered is hashed and compared to the stored hash.

## 5. Method Security

```java
@EnableMethodSecurity
public class SecurityConfig { ... }
```
*   **Purpose**: Enables the use of security annotations on Service or Controller methods.
*   **Usage examples**:
    *   `@PreAuthorize("hasRole('ADMIN')")`
    *   `@PreAuthorize("hasAuthority('MOD_PATIENTS')")`
