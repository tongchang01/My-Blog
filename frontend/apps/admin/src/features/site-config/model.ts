export interface SiteConfigPayload {
  siteTitleZh: string;
  siteTitleJa: string;
  siteTitleEn: string;
  siteSubtitleZh: string | null;
  siteSubtitleJa: string | null;
  siteSubtitleEn: string | null;
  aboutMdZh: string | null;
  aboutMdJa: string | null;
  aboutMdEn: string | null;
  logoUrl: string | null;
  faviconUrl: string | null;
  icpNo: string | null;
  spotifyPlaylistId: string | null;
}

export interface SiteConfig extends SiteConfigPayload {
  updatedAt: string;
  updatedBy: string | null;
}
