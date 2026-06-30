/**
 * @file router/index.ts
 * @description Vue Router 配置
 *
 * 说明：
 * - 当前使用 App.vue 内部路由逻辑
 * - 添加默认路由以避免警告
 *
 * @author Frontend Team
 */
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('../App.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('../App.vue'),
    },
  ],
})

export default router
