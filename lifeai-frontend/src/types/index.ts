export interface ApiResponse<T = unknown> {
  success: boolean
  data?: T
  error?: string
}

export interface User {
  id: number
  username: string
  email: string
}
