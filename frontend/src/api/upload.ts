import { http } from './http'

export async function uploadImage(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<{ url: string }>('/upload/image', formData)
  return data.url
}
