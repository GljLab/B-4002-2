import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import PostDetailView from '../views/PostDetailView.vue'
import LoginView from '../views/LoginView.vue'
import AdminView from '../views/AdminView.vue'
import EditPostView from '../views/EditPostView.vue'
import CategoryNavView from '../views/CategoryNavView.vue'
import KeywordCloudView from '../views/KeywordCloudView.vue'
import AdminCategoryView from '../views/AdminCategoryView.vue'
import AdminKeywordView from '../views/AdminKeywordView.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/posts/:id', name: 'post-detail', component: PostDetailView },
    { path: '/categories', name: 'categories', component: CategoryNavView },
    { path: '/keywords', name: 'keywords', component: KeywordCloudView },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/admin', name: 'admin', component: AdminView, meta: { requiresAuth: true } },
    { path: '/admin/categories', name: 'admin-categories', component: AdminCategoryView, meta: { requiresAuth: true } },
    { path: '/admin/keywords', name: 'admin-keywords', component: AdminKeywordView, meta: { requiresAuth: true } },
    {
      path: '/admin/posts/:id/edit',
      name: 'post-edit',
      component: EditPostView,
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  await authStore.ensureLoaded()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.name === 'login' && authStore.isLoggedIn) {
    return { name: 'admin' }
  }

  return true
})

export default router
