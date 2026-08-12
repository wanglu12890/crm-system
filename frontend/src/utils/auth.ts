const ACCESS_TOKEN_KEY = 'crm_access_token'
const TOKEN_EXPIRES_IN_KEY = 'crm_token_expires_in'

export interface AuthToken {
  accessToken: string
  expiresIn: number
}

export const getAccessToken = (): string | null =>
  localStorage.getItem(ACCESS_TOKEN_KEY)

export const setAuthToken = ({ accessToken, expiresIn }: AuthToken): void => {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  localStorage.setItem(TOKEN_EXPIRES_IN_KEY, String(expiresIn))
}

export const removeAuthToken = (): void => {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(TOKEN_EXPIRES_IN_KEY)
}
