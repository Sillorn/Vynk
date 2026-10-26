package com.vynk.profiles;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Public, privacy-safe representation of a professional profile.
 *
 * <p>Only data intended for anonymous visitors is included. Internal identifiers, account status,
 * creation timestamps and credentials are never exposed. Empty optional sections are omitted from
 * the payload.</p>
 */
@Schema(description = "Public professional profile.")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record PublicProfileResponse(
        @Schema(description = "Immutable public username.") String username,
        String firstName,
        String lastName,
        String headline,
        String summary,
        String location,
        List<LinkDto> links,
        List<ExperienceDto> experience,
        List<EducationDto> education,
        List<CertificationDto> certifications,
        List<ProjectDto> projects,
        List<SkillDto> skills,
        List<LanguageDto> languages) {

    public record LinkDto(String label, String url) {
    }

    public record ExperienceDto(
            String title,
            String company,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            boolean current,
            String description) {
    }

    public record EducationDto(
            String school,
            String degree,
            String fieldOfStudy,
            LocalDate startDate,
            LocalDate endDate,
            String description) {
    }

    public record CertificationDto(
            String name,
            String issuer,
            LocalDate issueDate,
            LocalDate expirationDate,
            String credentialUrl) {
    }

    public record ProjectDto(
            String name,
            String description,
            String url,
            LocalDate startDate,
            LocalDate endDate) {
    }

    public record SkillDto(String name) {
    }

    public record LanguageDto(String name, String proficiency) {
    }
}
