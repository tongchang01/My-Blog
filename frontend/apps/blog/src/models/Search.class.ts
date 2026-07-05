interface CachedSearchResult {
  value: {
    id?: string
    title: string
    content: string
    slug: string
  }
}

export type SearchResultType = {
  id?: string
  title: string
  content: string
  slug: string
}

export class SearchResult {
  id = ''
  title = ''
  content = ''
  slug = ''

  constructor(raw?: SearchResultType) {
    if (raw) {
      this.id = raw.id ?? ''
      this.title = raw.title
      this.content = raw.content
      this.slug = raw.slug
    }
  }
}

export class RecentSearchResults {
  data = new Map()
  capacity = 5
  cacheKey = 'ob-recent-search-results-key'

  constructor(raw?: SearchResultType[]) {
    if (raw) {
      this.initData(raw)
    }
  }

  initData(data: SearchResultType[]): void {
    data.forEach(value => {
      this.add(value)
    })
  }

  /** Fetch data from the cache */
  getData(): SearchResultType[] {
    const cache = localStorage.getItem(this.cacheKey)
    if (cache === null) return []

    let cacheResults = JSON.parse(cache)
    cacheResults = cacheResults.map((result: CachedSearchResult) => {
      return {
        id: result.value.id ?? '',
        title: result.value.title,
        content: result.value.content,
        slug: result.value.slug
      }
    })
    if (cacheResults.length > this.data.size) {
      this.initData(cacheResults.reverse())
    }

    return cacheResults
  }

  /** Caching the recent search results */
  cache(): void {
    localStorage.setItem(this.cacheKey, JSON.stringify(this.toArray()))
  }

  /**
   * Convert the Map into an Array
   * also reverse the order of the records.
   */
  toArray(): { [key: string]: string }[] {
    return Array.from(this.data, ([name, value]) => ({ name, value })).reverse()
  }

  /**
   * Adding the recent search results into the
   * Map, remove the first one come into the cache
   * if the cache reach it's maximum capacity.
   */
  add(result: SearchResultType): void {
    const searchResult = new SearchResult(result)
    if (this.data.has(searchResult.slug)) return

    if (this.data.size === this.capacity) {
      // Remove the first one added into the cache.
      this.data.delete(this.data.keys().next().value)
    }

    this.data.set(searchResult.slug, searchResult)
    this.cache()
  }

  remove(slug: string): void {
    if (!this.data.has(slug)) return
    this.data.delete(slug)
    this.cache()
  }
}
