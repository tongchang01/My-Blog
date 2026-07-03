import type { PublicTaxonomyDto } from './contract'
import type { TaxonomyItemViewModel } from './model'

const mapTaxonomy = (
  dto: PublicTaxonomyDto,
  basePath: 'categories' | 'tags'
): TaxonomyItemViewModel => ({
  id: dto.id,
  name: dto.name,
  slug: dto.slug,
  path: `${basePath}/${dto.slug}/`,
  count: dto.articleCount
})

export const mapCategories = (
  dto: PublicTaxonomyDto[]
): TaxonomyItemViewModel[] =>
  dto.map(item => mapTaxonomy(item, 'categories'))

export const mapTags = (dto: PublicTaxonomyDto[]): TaxonomyItemViewModel[] =>
  dto.map(item => mapTaxonomy(item, 'tags'))
