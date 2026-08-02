import DOMPurify from 'dompurify'

/**
 * 评论 HTML 已由后端 OWASP 白名单清洗，这里再过一遍 DOMPurify 作为前端纵深防御，
 * 避免后端清洗策略回归或被绕过时，恶意脚本经 v-html 进入页面。
 */
export const sanitizeCommentHtml = (html: string): string =>
  DOMPurify.sanitize(html)
