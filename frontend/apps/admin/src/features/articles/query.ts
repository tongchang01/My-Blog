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
    page: filters.page,
    size: filters.size
  };
}
