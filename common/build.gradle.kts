plugins {
    `java-library`
}

// Thin contracts module: event records, ApiError, CurrentUser. No business logic, no bootJar.
dependencies {
    // Only needed to read the authenticated principal out of the SecurityContext.
    // Consumers (resource servers) already provide this at runtime.
    compileOnly("org.springframework.security:spring-security-core")
}