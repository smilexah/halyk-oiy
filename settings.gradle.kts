rootProject.name = "maqsat"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(
    "common",
    "eureka-server",
    "gateway",
    "auth-service",
    "budget-service",
    "transaction-service",
    "goals-service",
    "family-service",
    "ai-assistant-service",
    "notification-service",
    "integration-service",
)