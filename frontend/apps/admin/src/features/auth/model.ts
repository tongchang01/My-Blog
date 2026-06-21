export type AdminRole = "ADMIN" | "DEMO";

export interface LoginRequest {
  username: string;
  password: string;
}

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
  accessExpiresIn: number;
  refreshExpiresIn: number;
}

export interface StoredSession {
  accessToken: string;
  refreshToken: string;
  accessExpiresAt: number;
  refreshExpiresAt: number;
}

export interface UserProfile {
  nickname: string;
  avatarUrl: string | null;
  bioZh: string | null;
  bioJa: string | null;
  bioEn: string | null;
  location: string | null;
  website: string | null;
  emailPublic: string | null;
  githubUrl: string | null;
  twitterUrl: string | null;
  linkedinUrl: string | null;
  zhihuUrl: string | null;
  qiitaUrl: string | null;
  juejinUrl: string | null;
}

export interface CurrentUser {
  id: string;
  username: string;
  type: AdminRole;
  profile: UserProfile;
}
