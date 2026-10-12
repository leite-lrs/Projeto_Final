package com.flashsale.orderprocessor;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;

@Provider
public class ApiKeySecurityFilter implements ContainerRequestFilter {
    @ConfigProperty(name = "app.security.require-api-key", defaultValue = "false")
    boolean requireApiKey;

    @ConfigProperty(name = "app.security.api-key", defaultValue = "unset")
    String apiKey;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        enforceAuthentication(requestContext, path);
    }

    private void enforceAuthentication(ContainerRequestContext requestContext, String path) {
        if (isProtectedPath(path) && shouldRequireAuthentication(requireApiKey, applicationEnvironment())) {
            rejectInvalidApiKey(requestContext);
        }
    }

    private static String applicationEnvironment() {
        return System.getenv().getOrDefault("APP_ENV", "dev");
    }

    private static boolean isProtectedPath(String path) {
        return !isPublicPath(path);
    }

    private void rejectInvalidApiKey(ContainerRequestContext requestContext) {
        String providedKey = requestContext.getHeaderString("X-API-Key");
        if (isValidApiKey(providedKey, apiKey)) {
            return;
        }
        requestContext.abortWith(unauthorizedResponse());
    }

    private static Response unauthorizedResponse() {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(Map.of("mensagem", "Chave de API inválida ou ausente"))
            .build();
    }

    static boolean shouldRequireAuthentication(boolean configuredValue, String environment) {
        return configuredValue || "prod".equalsIgnoreCase(environment);
    }

    static boolean isValidApiKey(String providedValue, String expectedValue) {
        if (isUnconfiguredApiKey(expectedValue)) {
            return false;
        }
        return expectedValue.equals(providedValue);
    }

    private static boolean isUnconfiguredApiKey(String expectedValue) {
        return expectedValue == null || expectedValue.isBlank() || isDefaultApiKey(expectedValue);
    }

    private static boolean isDefaultApiKey(String expectedValue) {
        return "unset".equals(expectedValue) || "change-me-in-production".equals(expectedValue);
    }

    private static boolean isPublicPath(String path) {
        if (isEmptyPath(path)) {
            return true;
        }
        return isHealthPath(path) || isDocumentationPath(path);
    }

    private static boolean isEmptyPath(String path) {
        return path == null || path.isBlank();
    }

    private static boolean isHealthPath(String path) {
        return path.startsWith("health");
    }

    private static boolean isDocumentationPath(String path) {
        return path.startsWith("q/") || path.startsWith("openapi") || path.startsWith("swagger-ui");
    }
}
