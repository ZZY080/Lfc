import { ApiError } from './client'
import { getApiBaseUrl } from './baseUrl'

export async function uploadPostImage(
  token: string,
  file: File,
): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)

  const response = await fetch(
    `${getApiBaseUrl()}/consumer/upload/image?scope=post`,
    {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: formData,
    },
  )

  if (!response.ok) {
    let message = response.statusText
    try {
      const data = (await response.json()) as { message?: string | string[] }
      if (Array.isArray(data.message)) {
        message = data.message.join('；')
      } else if (data.message) {
        message = data.message
      }
    } catch {
      // ignore
    }
    throw new ApiError(message || '图片上传失败', response.status)
  }

  const data = (await response.json()) as { url: string }
  return data.url
}
