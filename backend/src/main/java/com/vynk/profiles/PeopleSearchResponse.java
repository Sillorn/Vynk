package com.vynk.profiles;

import com.vynk.identity.entities.Profile;

public record PeopleSearchResponse(String username, String firstName, String lastName,
                                  String headline, String location) {
    public static PeopleSearchResponse from(Profile profile) {
        return new PeopleSearchResponse(profile.getUserAccount().getUsername(),
                profile.getFirstName(), profile.getLastName(), profile.getHeadline(), profile.getLocation());
    }
}
