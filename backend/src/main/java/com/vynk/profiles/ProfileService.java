package com.vynk.profiles;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import com.vynk.identity.Profile;
import com.vynk.identity.ProfileRepository;
import com.vynk.shared.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and writes structured profile data.
 *
 * <p>Public reads expose only {@link PublicProfileResponse}. Section writes are owner-scoped: the
 * caller's authenticated username always selects the profile, so a request can never edit another
 * account's data.</p>
 */
@Service
public class ProfileService {

    private final ProfileRepository profiles;
    private final ProfileExperienceRepository experience;
    private final ProfileEducationRepository education;
    private final ProfileCertificationRepository certifications;
    private final ProfileProjectRepository projects;
    private final ProfileSkillRepository skills;
    private final ProfileLanguageRepository languages;
    private final ProfileLinkRepository links;

    public ProfileService(ProfileRepository profiles,
                          ProfileExperienceRepository experience,
                          ProfileEducationRepository education,
                          ProfileCertificationRepository certifications,
                          ProfileProjectRepository projects,
                          ProfileSkillRepository skills,
                          ProfileLanguageRepository languages,
                          ProfileLinkRepository links) {
        this.profiles = profiles;
        this.experience = experience;
        this.education = education;
        this.certifications = certifications;
        this.projects = projects;
        this.skills = skills;
        this.languages = languages;
        this.links = links;
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse publicProfile(String username) {
        Profile profile = profiles.findByUserAccountUsernameIgnoreCase(username)
                .orElseThrow(() -> ApiException.notFound("Profile not found."));
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse ownProfile(String username) {
        return publicProfile(username);
    }

    @Transactional
    public PublicProfileResponse saveBasics(String username, ProfileUpdateRequests.Basics request) {
        Profile profile = requireProfile(username);
        profile.setFirstName(request.firstName().trim());
        profile.setLastName(request.lastName().trim());
        profile.setHeadline(request.headline().trim());
        profile.setSummary(request.summary().trim());
        profile.setLocation(blankToNull(request.location()));
        return toResponse(profile);
    }

    @Transactional
    public PublicProfileResponse saveLinks(String username, ProfileUpdateRequests.Links request) {
        Profile profile = requireProfile(username);
        List<ProfileUpdateRequests.LinkItem> items = nullSafe(request.items());
        links.deleteByProfileId(profile.getId());
        links.flush();
        for (int i = 0; i < items.size(); i++) {
            ProfileUpdateRequests.LinkItem item = items.get(i);
            links.save(new ProfileLink(profile, item.label().trim(), requireHttpUrl(item.url()), i));
        }
        return toResponse(profile);
    }

    @Transactional
    public PublicProfileResponse saveExperience(String username, ProfileUpdateRequests.Experience request) {
        Profile profile = requireProfile(username);
        List<ProfileUpdateRequests.ExperienceItem> items = nullSafe(request.items());
        for (ProfileUpdateRequests.ExperienceItem item : items) {
            requireDates(item.startDate(), item.endDate());
            if (item.current() && item.endDate() != null) {
                throw ApiException.validationFailed("A current role must not have an end date.");
            }
        }
        experience.deleteByProfileId(profile.getId());
        experience.flush();
        for (int i = 0; i < items.size(); i++) {
            ProfileUpdateRequests.ExperienceItem item = items.get(i);
            experience.save(new ProfileExperience(
                    profile,
                    item.title().trim(),
                    item.company().trim(),
                    blankToNull(item.location()),
                    item.startDate(),
                    item.current() ? null : item.endDate(),
                    item.current(),
                    blankToNull(item.description()),
                    i));
        }
        return toResponse(profile);
    }

    @Transactional
    public PublicProfileResponse saveEducation(String username, ProfileUpdateRequests.Education request) {
        Profile profile = requireProfile(username);
        List<ProfileUpdateRequests.EducationItem> items = nullSafe(request.items());
        for (ProfileUpdateRequests.EducationItem item : items) {
            requireDates(item.startDate(), item.endDate());
        }
        education.deleteByProfileId(profile.getId());
        education.flush();
        for (int i = 0; i < items.size(); i++) {
            ProfileUpdateRequests.EducationItem item = items.get(i);
            education.save(new ProfileEducation(
                    profile,
                    item.school().trim(),
                    blankToNull(item.degree()),
                    blankToNull(item.fieldOfStudy()),
                    item.startDate(),
                    item.endDate(),
                    blankToNull(item.description()),
                    i));
        }
        return toResponse(profile);
    }

    @Transactional
    public PublicProfileResponse saveCertifications(
            String username, ProfileUpdateRequests.Certifications request) {
        Profile profile = requireProfile(username);
        List<ProfileUpdateRequests.CertificationItem> items = nullSafe(request.items());
        for (ProfileUpdateRequests.CertificationItem item : items) {
            requireDates(item.issueDate(), item.expirationDate());
        }
        certifications.deleteByProfileId(profile.getId());
        certifications.flush();
        for (int i = 0; i < items.size(); i++) {
            ProfileUpdateRequests.CertificationItem item = items.get(i);
            certifications.save(new ProfileCertification(
                    profile,
                    item.name().trim(),
                    blankToNull(item.issuer()),
                    item.issueDate(),
                    item.expirationDate(),
                    item.credentialUrl() == null || item.credentialUrl().isBlank()
                            ? null
                            : requireHttpUrl(item.credentialUrl()),
                    i));
        }
        return toResponse(profile);
    }

    @Transactional
    public PublicProfileResponse saveProjects(String username, ProfileUpdateRequests.Projects request) {
        Profile profile = requireProfile(username);
        List<ProfileUpdateRequests.ProjectItem> items = nullSafe(request.items());
        for (ProfileUpdateRequests.ProjectItem item : items) {
            requireDates(item.startDate(), item.endDate());
        }
        projects.deleteByProfileId(profile.getId());
        projects.flush();
        for (int i = 0; i < items.size(); i++) {
            ProfileUpdateRequests.ProjectItem item = items.get(i);
            projects.save(new ProfileProject(
                    profile,
                    item.name().trim(),
                    blankToNull(item.description()),
                    item.url() == null || item.url().isBlank() ? null : requireHttpUrl(item.url()),
                    item.startDate(),
                    item.endDate(),
                    i));
        }
        return toResponse(profile);
    }

    @Transactional
    public PublicProfileResponse saveSkills(String username, ProfileUpdateRequests.Skills request) {
        Profile profile = requireProfile(username);
        List<ProfileUpdateRequests.SkillItem> items = nullSafe(request.items());
        skills.deleteByProfileId(profile.getId());
        skills.flush();
        for (int i = 0; i < items.size(); i++) {
            skills.save(new ProfileSkill(profile, items.get(i).name().trim(), i));
        }
        return toResponse(profile);
    }

    @Transactional
    public PublicProfileResponse saveLanguages(String username, ProfileUpdateRequests.Languages request) {
        Profile profile = requireProfile(username);
        List<ProfileUpdateRequests.LanguageItem> items = nullSafe(request.items());
        languages.deleteByProfileId(profile.getId());
        languages.flush();
        for (int i = 0; i < items.size(); i++) {
            ProfileUpdateRequests.LanguageItem item = items.get(i);
            languages.save(new ProfileLanguage(
                    profile, item.name().trim(), blankToNull(item.proficiency()), i));
        }
        return toResponse(profile);
    }

    private Profile requireProfile(String username) {
        if (username == null || username.isBlank()) {
            throw ApiException.unauthenticated("Authentication required.");
        }
        return profiles.findByUserAccountUsernameIgnoreCase(username)
                .orElseThrow(() -> ApiException.notFound("Profile not found."));
    }

    private PublicProfileResponse toResponse(Profile profile) {
        Long profileId = profile.getId();
        return new PublicProfileResponse(
                profile.getUserAccount().getUsername(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getHeadline(),
                profile.getSummary(),
                profile.getLocation(),
                links.findByProfileIdOrderBySortOrderAsc(profileId).stream()
                        .map(link -> new PublicProfileResponse.LinkDto(link.getLabel(), link.getUrl()))
                        .toList(),
                experience.findByProfileIdOrderBySortOrderAsc(profileId).stream()
                        .map(item -> new PublicProfileResponse.ExperienceDto(
                                item.getTitle(), item.getCompany(), item.getLocation(),
                                item.getStartDate(), item.getEndDate(), item.isCurrent(),
                                item.getDescription()))
                        .toList(),
                education.findByProfileIdOrderBySortOrderAsc(profileId).stream()
                        .map(item -> new PublicProfileResponse.EducationDto(
                                item.getSchool(), item.getDegree(), item.getFieldOfStudy(),
                                item.getStartDate(), item.getEndDate(), item.getDescription()))
                        .toList(),
                certifications.findByProfileIdOrderBySortOrderAsc(profileId).stream()
                        .map(item -> new PublicProfileResponse.CertificationDto(
                                item.getName(), item.getIssuer(), item.getIssueDate(),
                                item.getExpirationDate(), item.getCredentialUrl()))
                        .toList(),
                projects.findByProfileIdOrderBySortOrderAsc(profileId).stream()
                        .map(item -> new PublicProfileResponse.ProjectDto(
                                item.getName(), item.getDescription(), item.getUrl(),
                                item.getStartDate(), item.getEndDate()))
                        .toList(),
                skills.findByProfileIdOrderBySortOrderAsc(profileId).stream()
                        .map(item -> new PublicProfileResponse.SkillDto(item.getName()))
                        .toList(),
                languages.findByProfileIdOrderBySortOrderAsc(profileId).stream()
                        .map(item -> new PublicProfileResponse.LanguageDto(
                                item.getName(), item.getProficiency()))
                        .toList());
    }

    private static void requireDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw ApiException.validationFailed("End date must not be before the start date.");
        }
    }

    private static String requireHttpUrl(String value) {
        String url = value == null ? "" : value.trim();
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null
                    || !(uri.getScheme().equals("http") || uri.getScheme().equals("https"))
                    || uri.getHost() == null) {
                throw ApiException.validationFailed("Links must be absolute http or https URLs.");
            }
            return url;
        } catch (IllegalArgumentException exception) {
            throw ApiException.validationFailed("Links must be absolute http or https URLs.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static <T> List<T> nullSafe(List<T> items) {
        return items == null ? List.of() : items;
    }
}
