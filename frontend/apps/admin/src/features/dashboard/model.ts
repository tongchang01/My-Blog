export interface StatsTrendPoint {
  date: string;
  pv: number;
  uv: number;
}

export interface StatsTopArticle {
  articleId: string;
  title: string | null;
  pv: number;
  dailyUvSum: number;
}

export interface StatsLanguageDistribution {
  language: string;
  pv: number;
  ratio: number;
}

export interface StatsDashboard {
  periodPv: number;
  todayPv: number;
  todayUv: number;
  averageDailyUv: number;
  trend: StatsTrendPoint[];
  topArticles: StatsTopArticle[];
  languageDistribution: StatsLanguageDistribution[];
}

export interface StatsDashboardFilters {
  from?: string;
  to?: string;
}
