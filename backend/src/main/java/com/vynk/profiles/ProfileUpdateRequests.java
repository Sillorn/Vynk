package com.vynk.profiles;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Validated request payloads for owner-only profile section saves.
 *
 * <p>Each section is saved as a whole list, which makes explicit ordering and atomic validation
 * straightforward. Entries are stored in the submitted order.</p>
 */
public final class ProfileUpdateRequests {

    private ProfileUpdateRequests() {
    }

    public record Basics(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Size(max = 200) String headline,
            @NotBlank @Size(max = 2000) String summary,
            @Size(max = 200) String location) {
    }

    public record LinkItem(
            @NotBlank @Size(max = 100) String label,
            @NotBlank @Size(max = 500) String url) {
    }

    public record Links(@Valid List<LinkItem> items) {
    }

    public record ExperienceItem(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 200) String company,
            @Size(max = 200) String location,
            LocalDate startDate,
            LocalDate endDate,
            boolean current,
            @Size(max = 4000) String description) {
    }

    public record Experience(@Valid List<ExperienceItem> items) {
    }

    public record EducationItem(
            @NotBlank @Size(max = 200) String school,
            @Size(max = 200) String degree,
            @Size(max = 200) String fieldOfStudy,
            LocalDate startDate,
            LocalDate endDate,
            @Size(max = 4000) String description) {
    }

    public record Education(@Valid List<EducationItem> items) {
    }

    public record CertificationItem(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 200) String issuer,
            LocalDate issueDate,
            LocalDate expirationDate,
            @Size(max = 500) String credentialUrl) {
    }

    public record Certifications(@Valid List<CertificationItem> items) {
    }

    public record ProjectItem(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 4000) String description,
            @Size(max = 500) String url,
            LocalDate startDate,
            LocalDate endDate) {
    }

    public record Projects(@Valid List<ProjectItem> items) {
    }

    public record SkillItem(@NotBlank @Size(max = 100) String name) {
    }

    public record Skills(@Valid List<SkillItem> items) {
    }

    public record LanguageItem(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 50) String proficiency) {
    }

    public record Languages(@Valid List<LanguageItem> items) {
    }
}
