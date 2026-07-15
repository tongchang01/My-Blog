import type {
  ArticleListFilters,
  ArticleListParams
} from "./model";

export function buildArticleListParams(
  filters: ArticleListFilters
): ArticleListParams {
  const titleKeyword = filters.titleKeyword.trim();
  return {
    ...(titleKeyword ? { titleKeyword } : {}),
    ...(filters.status === "ALL" ? {} : { status: filters.status }),
    ...(filters.categoryId ? { categoryId: filters.categoryId } : {}),
    ...(filters.tagId ? { tagId: filters.tagId } : {}),
    ...(filters.createdFrom ? { createdFrom: filters.createdFrom } : {}),
    ...(filters.createdTo ? { createdTo: filters.createdTo } : {}),
    ...(filters.publishFrom ? { publishFrom: filters.publishFrom } : {}),
    ...(filters.publishTo ? { publishTo: filters.publishTo } : {}),
    page: filters.page,
    size: filters.size
  };
}
