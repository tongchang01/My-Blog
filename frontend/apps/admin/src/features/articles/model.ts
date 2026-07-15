import type {
  CategoryItem,
  LocalizedNames,
  TagItem
} from "@/features/taxonomy/model";

export type {
  CategoryItem,
  LocalizedNames,
  TagItem
} from "@/features/taxonomy/model";

export type ArticleStatus =
  | "DRAFT"
  | "PUBLISHED"
  | "PRIVATE"
  | "PASSWORD"
  | "SCHEDULED";

export type ArticleStatusFilter = ArticleStatus | "ALL";
export type ArticleHomepageSlot = "NONE" | "PINNED" | "FEATURED";
export type AdminLocale = "zh" | "ja" | "en";

export interface ArticleListFilters {
  titleKeyword: string;
  status: ArticleStatusFilter;
  categoryId: string;
  tagId: string;
  createdFrom: string;
  createdTo: string;
  publishFrom: string;
  publishTo: string;
  page: number;
  size: number;
}

export interface ArticleListParams {
  titleKeyword?: string;
  status?: ArticleStatus;
  categoryId?: string;
  tagId?: string;
  createdFrom?: string;
  createdTo?: string;
  publishFrom?: string;
  publishTo?: string;
  page: number;
  size: number;
}

export interface ArticleListItem {
  id: string;
  titleZh: string | null;
  titleJa: string | null;
  titleEn: string | null;
  summaryZh: string | null;
  summaryJa: string | null;
  summaryEn: string | null;
  categoryId: string | null;
  categoryNameZh: string | null;
  slug: string;
  status: ArticleStatus;
  homepageSlot: ArticleHomepageSlot;
  publishAt: string | null;
  coverAttachmentId: string | null;
  coverUrl: string | null;
  commentCount: number;
  tagIds: string[];
  createdAt: string;
  createdBy: string | null;
  updatedAt: string;
  updatedBy: string | null;
}

export interface ArticleDetail extends ArticleListItem {
  body: string | null;
  authorId: string;
}

export interface DeletedArticleListItem {
  id: string;
  titleZh: string | null;
  titleJa: string | null;
  titleEn: string | null;
  status: ArticleStatus;
  categoryId: string | null;
  deletedAt: string;
  deletedBy: string | null;
}

export interface ArticleWritePayload {
  titleZh: string;
  titleJa: string | null;
  titleEn: string | null;
  summaryZh: string;
  summaryJa: string | null;
  summaryEn: string | null;
  body: string;
  categoryId: string | null;
  tagIds: string[];
  slug: string | null;
  status: ArticleStatus;
  homepageSlot: ArticleHomepageSlot;
  password: string | null;
  publishAt: string | null;
  coverAttachmentId: string | null;
}

export interface PageResponse<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
}
