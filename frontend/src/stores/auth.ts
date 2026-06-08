import { defineStore } from 'pinia'
import type { AuthUser, LoginPayload } from '../types'
import { loginSession, logoutSession, meSession } from '../api/auth'

interface AuthState {
  user: AuthUser | null
  loaded: boolean
  loading: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    loaded: false,
    loading: false,
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.user),
  },

  actions: {
    async ensureLoaded() {
      if (this.loaded || this.loading) {
        return
      }

      this.loading = true
      try {
        this.user = await meSession()
      } catch {
        this.user = null
      } finally {
        this.loaded = true
        this.loading = false
      }
    },

    async login(payload: LoginPayload) {
      this.user = await loginSession(payload)
      this.loaded = true
    },

    async logout() {
      try {
        await logoutSession()
      } finally {
        this.user = null
        this.loaded = true
      }
    },
  },
})
