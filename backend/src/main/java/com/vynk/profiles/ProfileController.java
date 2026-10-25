package com.vynk.profiles;

import com.vynk.shared.api.ApiPaths;
import com.vynk.shared.error.ApiException;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public profile reads and owner-only profile section edits.
 */
@RestController
@RequestMapping(ApiPaths.BASE + "/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/{username}")
    public PublicProfileResponse publicProfile(@PathVariable String username) {
        return profileService.publicProfile(username);
    }

    @GetMapping("/me")
    public PublicProfileResponse ownProfile(Authentication authentication) {
        return profileService.ownProfile(requireUsername(authentication));
    }

    @PutMapping("/me")
    public PublicProfileResponse saveBasics(@Valid @RequestBody ProfileUpdateRequests.Basics request,
                                            Authentication authentication) {
        return profileService.saveBasics(requireUsername(authentication), request);
    }

    @PutMapping("/me/links")
    public PublicProfileResponse saveLinks(@Valid @RequestBody ProfileUpdateRequests.Links request,
                                           Authentication authentication) {
        return profileService.saveLinks(requireUsername(authentication), request);
    }

    @PutMapping("/me/experience")
    public PublicProfileResponse saveExperience(
            @Valid @RequestBody ProfileUpdateRequests.Experience request,
            Authentication authentication) {
        return profileService.saveExperience(requireUsername(authentication), request);
    }

    @PutMapping("/me/education")
    public PublicProfileResponse saveEducation(
            @Valid @RequestBody ProfileUpdateRequests.Education request,
            Authentication authentication) {
        return profileService.saveEducation(requireUsername(authentication), request);
    }

    @PutMapping("/me/certifications")
    public PublicProfileResponse saveCertifications(
            @Valid @RequestBody ProfileUpdateRequests.Certifications request,
            Authentication authentication) {
        return profileService.saveCertifications(requireUsername(authentication), request);
    }

    @PutMapping("/me/projects")
    public PublicProfileResponse saveProjects(
            @Valid @RequestBody ProfileUpdateRequests.Projects request,
            Authentication authentication) {
        return profileService.saveProjects(requireUsername(authentication), request);
    }

    @PutMapping("/me/skills")
    public PublicProfileResponse saveSkills(
            @Valid @RequestBody ProfileUpdateRequests.Skills request,
            Authentication authentication) {
        return profileService.saveSkills(requireUsername(authentication), request);
    }

    @PutMapping("/me/languages")
    public PublicProfileResponse saveLanguages(
            @Valid @RequestBody ProfileUpdateRequests.Languages request,
            Authentication authentication) {
        return profileService.saveLanguages(requireUsername(authentication), request);
    }

    private static String requireUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw ApiException.unauthenticated("Authentication required.");
        }
        return authentication.getName();
    }
}
