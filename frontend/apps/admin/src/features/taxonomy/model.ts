export interface LocalizedNames {
  nameZh: string | null;
  nameJa: string | null;
  nameEn: string | null;
}

export interface TaxonomyAuditFields {
  id: string;
  createdAt: string;
  createdBy: string | null;
  updatedAt: string;
  updatedBy: string | null;
}

export interface CategoryItem extends LocalizedNames, TaxonomyAuditFields {
  nameZh: string;
  slug: string;
  sortOrder: number;
}

export interface TagItem extends LocalizedNames, TaxonomyAuditFields {
  nameZh: string;
  slug: string;
}

export interface CategoryWritePayload {
  nameZh: string;
  nameJa: string | null;
  nameEn: string | null;
  slug: string;
  sortOrder: number;
}

export type TagWritePayload = Omit<CategoryWritePayload, "sortOrder">;

export interface CategorySortItem {
  id: string;
  sortOrder: number;
}
