/**
 * Public profile payload returned by `GET /api/v1/profiles/{username}`.
 *
 * Mirrors the backend `PublicProfileResponse`. Optional collections are omitted by the server when
 * empty, and no private account data is ever part of this contract.
 */
export interface PublicProfile {
  readonly username: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly headline: string;
  readonly summary: string;
  readonly location?: string;
  readonly links?: readonly ProfileLink[];
  readonly experience?: readonly ProfileExperience[];
  readonly education?: readonly ProfileEducation[];
  readonly certifications?: readonly ProfileCertification[];
  readonly projects?: readonly ProfileProject[];
  readonly skills?: readonly ProfileSkill[];
  readonly languages?: readonly ProfileLanguage[];
}

export interface ProfileLink {
  readonly label: string;
  readonly url: string;
}

export interface ProfileExperience {
  readonly title: string;
  readonly company: string;
  readonly location?: string;
  readonly startDate?: string;
  readonly endDate?: string;
  readonly current: boolean;
  readonly description?: string;
}

export interface ProfileEducation {
  readonly school: string;
  readonly degree?: string;
  readonly fieldOfStudy?: string;
  readonly startDate?: string;
  readonly endDate?: string;
  readonly description?: string;
}

export interface ProfileCertification {
  readonly name: string;
  readonly issuer?: string;
  readonly issueDate?: string;
  readonly expirationDate?: string;
  readonly credentialUrl?: string;
}

export interface ProfileProject {
  readonly name: string;
  readonly description?: string;
  readonly url?: string;
  readonly startDate?: string;
  readonly endDate?: string;
}

export interface ProfileSkill {
  readonly name: string;
}

export interface ProfileLanguage {
  readonly name: string;
  readonly proficiency?: string;
}
