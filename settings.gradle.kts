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
    "analytics-service",
    "financial-agent-service",
    "parse-budget-plan-service",
    "summary-llm-service",
    "recommendation-service",
    "alser-mock-service",
    "halyk-travel-mock-service",
)