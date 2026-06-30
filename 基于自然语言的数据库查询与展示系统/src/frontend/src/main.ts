/**
 * @file main.ts
 * @description 应用程序入口文件
 *
 * 职责：
 * - 创建 Vue 应用实例
 * - 注册全局插件（Pinia 状态管理、Vue Router）
 * - 导入全局样式
 * - 挂载应用到 DOM
 *
 * @author Frontend Team
 * @since 1.0.0
 */

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import './assets/index.css'
// Font Awesome 通过 CDN 引入（在 index.html 中），这里不需要导入

// 创建 Vue 应用实例
const app = createApp(App)

// 注册 Pinia 状态管理
app.use(createPinia())

// 注册 Vue Router
app.use(router)

// 挂载应用到 #root 元素
app.mount('#root')
