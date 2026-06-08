<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from './stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const displayName = computed(() => authStore.displayName)
const isAdmin = computed(() => authStore.isAdmin)
const isAuthor = computed(() => authStore.isAuthor)

async function logout() {
  await authStore.logout()
  ElMessage.success('已退出登录')
  await router.push('/login')
}
</script>

<template>
  <div class="app-shell" :class="{ 'author-theme': isAuthor }">
    <header class="top-nav">
      <div class="top-nav-inner">
        <div class="brand" @click="$router.push('/')">简易博客</div>
        <nav class="nav-links" aria-label="主导航">
          <router-link class="nav-link" to="/">首页</router-link>
          <router-link class="nav-link" to="/categories">分类</router-link>
          <router-link class="nav-link" to="/keywords">关键词</router-link>
          <template v-if="isAdmin">
            <router-link class="nav-link" to="/admin">后台管理</router-link>
            <router-link class="nav-link" to="/admin/categories">分类管理</router-link>
            <router-link class="nav-link" to="/admin/keywords">关键词统计</router-link>
            <router-link class="nav-link" to="/admin/authors">作者管理</router-link>
            <router-link class="nav-link" to="/admin/reviews">审核队列</router-link>
            <router-link class="nav-link" to="/admin/stats">全局统计</router-link>
          </template>
          <template v-if="isAuthor">
            <router-link class="nav-link" to="/author">工作台</router-link>
            <router-link class="nav-link" to="/author/posts">我的文章</router-link>
            <router-link class="nav-link" to="/author/posts/create">创作中心</router-link>
            <router-link class="nav-link" to="/author/stats">个人统计</router-link>
            <router-link class="nav-link" to="/author/settings">个人设置</router-link>
          </template>
          <router-link v-if="!authStore.isLoggedIn" class="nav-link" to="/login">登录</router-link>
          <a v-if="authStore.isLoggedIn" class="nav-link" href="#" @click.prevent="logout">退出（{{ displayName }}）</a>
        </nav>
      </div>
    </header>

    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>
