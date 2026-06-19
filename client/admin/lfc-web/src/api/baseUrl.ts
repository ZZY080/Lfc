/** API 请求前缀：使用当前页面所在域名，便于 dev / staging / prod 同源部署 */
export function getApiBaseUrl(): string {
  if (typeof window === 'undefined') {
    return ''
  }
  return window.location.origin
}
