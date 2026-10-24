package com.vynk.shared.openapi;

import java.math.BigDecimal;
import java.util.Arrays;

import com.vynk.shared.error.ApiErrorCode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publishes the Vynk OpenAPI document at {@code /v3/api-docs}.
 *
 * <p>Endpoints add their own operations through annotations. Shared contract documents that are
 * not yet referenced by an annotated endpoint (the RFC 9457 problem response and the page
 * envelope) are registered here so clients can generate models from a single artifact.</p>
 */
@Configuration
public class OpenApiConfig {

    public static final String TITLE = "Vynk API";
    public static final String API_VERSION = "v1";
    public static final String PROBLEM_SCHEMA = "Problem";
    public static final String FIELD_VIOLATION_SCHEMA = "FieldViolation";
    public static final String PAGE_RESPONSE_SCHEMA = "PageResponse";

    @Bean
    OpenAPI vynkOpenApi() {
        Components components = new Components()
                .addSchemas(PROBLEM_SCHEMA, problemSchema())
                .addSchemas(FIELD_VIOLATION_SCHEMA, fieldViolationSchema())
                .addSchemas(PAGE_RESPONSE_SCHEMA, pageResponseSchema());

        return new OpenAPI()
                .info(new Info()
                        .title(TITLE)
                        .version(API_VERSION)
                        .description("Session-authenticated REST API for the Vynk professional network. "
                                + "All endpoints are versioned under /api/v1. Errors use RFC 9457 problem "
                                + "responses with a stable code and traceId. List endpoints return the "
                                + "PageResponse envelope and accept page and size query parameters. "
                                + "See docs/api-conventions.md for the full contract conventions."))
                .components(components);
    }

    private static Schema<?> problemSchema() {
        StringSchema code = new StringSchema();
        code.setDescription("Stable machine-readable application error code.");
        code.setExample(ApiErrorCode.NOT_FOUND.name());
        code.setEnum(Arrays.stream(ApiErrorCode.values()).map(Enum::name).toList());

        StringSchema type = new StringSchema();
        type.setFormat("uri");
        type.setDescription("Problem type URI, for example urn:vynk:error:not-found.");
        type.setExample("urn:vynk:error:not-found");

        StringSchema title = new StringSchema();
        title.setDescription("Short, stable human-readable summary.");
        title.setExample("Not Found");

        IntegerSchema status = new IntegerSchema();
        status.setDescription("HTTP status code.");
        status.setExample(404);

        StringSchema detail = new StringSchema();
        detail.setDescription("Human-readable explanation safe to show to the caller.");

        StringSchema instance = new StringSchema();
        instance.setFormat("uri");
        instance.setDescription("Request path that produced the problem.");
        instance.setExample("/api/v1/profiles/example");

        StringSchema traceId = new StringSchema();
        traceId.setDescription("Request trace ID, also returned in the X-Trace-Id header.");

        ArraySchema errors = new ArraySchema();
        errors.setDescription("Field-level validation failures, present only for validation problems.");
        errors.setItems(new Schema<>().$ref("#/components/schemas/" + FIELD_VIOLATION_SCHEMA));

        ObjectSchema schema = new ObjectSchema();
        schema.setDescription("RFC 9457 problem response with Vynk extension members.");
        schema.addProperty("type", type);
        schema.addProperty("title", title);
        schema.addProperty("status", status);
        schema.addProperty("detail", detail);
        schema.addProperty("instance", instance);
        schema.addProperty("code", code);
        schema.addProperty("traceId", traceId);
        schema.addProperty("errors", errors);
        schema.addRequiredItem("type");
        schema.addRequiredItem("title");
        schema.addRequiredItem("status");
        schema.addRequiredItem("code");
        schema.addRequiredItem("traceId");
        return schema;
    }

    private static Schema<?> fieldViolationSchema() {
        StringSchema field = new StringSchema();
        field.setDescription("Request field or parameter name, or _request for request-wide failures.");
        field.setExample("username");

        StringSchema message = new StringSchema();
        message.setDescription("Human-readable, safe-to-expose validation message.");

        ObjectSchema schema = new ObjectSchema();
        schema.setDescription("A single field-level validation failure.");
        schema.addProperty("field", field);
        schema.addProperty("message", message);
        schema.addRequiredItem("field");
        schema.addRequiredItem("message");
        return schema;
    }

    private static Schema<?> pageResponseSchema() {
        ArraySchema items = new ArraySchema();
        items.setDescription("Items in the endpoint's documented order.");
        items.setItems(new ObjectSchema());

        IntegerSchema page = new IntegerSchema();
        page.setDescription("Zero-based index of this page.");
        page.setMinimum(BigDecimal.ZERO);

        IntegerSchema size = new IntegerSchema();
        size.setDescription("Maximum number of items per page.");
        size.setMinimum(BigDecimal.ONE);
        size.setMaximum(BigDecimal.valueOf(100));

        IntegerSchema totalElements = new IntegerSchema();
        totalElements.setFormat("int64");
        totalElements.setDescription("Total number of matching items.");
        totalElements.setMinimum(BigDecimal.ZERO);

        IntegerSchema totalPages = new IntegerSchema();
        totalPages.setDescription("Total number of pages.");
        totalPages.setMinimum(BigDecimal.ZERO);

        BooleanSchema hasNext = new BooleanSchema();
        hasNext.setDescription("Whether a page after this one exists.");

        ObjectSchema schema = new ObjectSchema();
        schema.setDescription("Bounded page of results with navigation metadata.");
        schema.addProperty("items", items);
        schema.addProperty("page", page);
        schema.addProperty("size", size);
        schema.addProperty("totalElements", totalElements);
        schema.addProperty("totalPages", totalPages);
        schema.addProperty("hasNext", hasNext);
        schema.addRequiredItem("items");
        schema.addRequiredItem("page");
        schema.addRequiredItem("size");
        schema.addRequiredItem("totalElements");
        schema.addRequiredItem("totalPages");
        schema.addRequiredItem("hasNext");
        return schema;
    }
}
