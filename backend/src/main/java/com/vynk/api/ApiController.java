package com.vynk.api;

import com.vynk.shared.api.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service metadata endpoints.
 */
@RestController
@Tag(name = "System", description = "Service metadata endpoints.")
@RequestMapping(ApiPaths.BASE)
public class ApiController {

    @Operation(
            summary = "Ping the service",
            description = "Returns a constant healthy response. Used by startup checks and smoke tests.")
    @ApiResponse(
            responseCode = "200",
            description = "Service is running.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PingResponse.class)))
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(ref = "#/components/schemas/Problem")))
    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("ok");
    }

    /**
     * Ping response payload.
     *
     * @param status constant readiness marker
     */
    @Schema(description = "Ping response payload.")
    public record PingResponse(
            @Schema(description = "Readiness marker.", example = "ok") String status) {
    }
}
