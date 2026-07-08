import { defineStore } from 'pinia'
import { fetchImplicitPageBySource } from '@/api'
import { Page } from '@/models/Article.class'

export const useArticleStore = defineStore('articleStore', () => {
  const fetchArticle = async (source: string): Promise<Page> => {
    const { data } = await fetchImplicitPageBySource(source)
    return new Promise(resolve =>
      setTimeout(() => {
        resolve(new Page(data))
      }, 200)
    )
  }

  return {
    fetchArticle
  }
})
