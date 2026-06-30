/**
 * @file composables/useTheme.ts
 * @description 主题管理 composable
 * 
 * 功能：
 * - 多种主题样式切换（浅色、深色、蓝色、绿色、紫色）
 * - 主题状态持久化
 */

import { ref } from 'vue'

export type Theme = 'light' | 'dark' | 'blue' | 'green' | 'purple' | 'large'

// 主题配置
export const themeConfig = {
  light: {
    name: '浅色主题',
    icon: 'fa-sun-o',
    description: '经典浅色模式，适合日间使用',
    bg: 'bg-gray-50',
    card: 'bg-white',
    text: 'text-gray-900',
    primary: '#165DFF',
  },
  dark: {
    name: '深色主题',
    icon: 'fa-moon-o',
    description: '护眼深色模式，适合夜间使用',
    bg: 'bg-gray-900',
    card: 'bg-gray-800',
    text: 'text-gray-100',
    primary: '#3B82F6',
  },
  blue: {
    name: '蓝色主题',
    icon: 'fa-tint',
    description: '清新蓝色风格，专业商务',
    bg: 'bg-blue-50',
    card: 'bg-white',
    text: 'text-blue-900',
    primary: '#2563EB',
  },
  green: {
    name: '绿色主题',
    icon: 'fa-leaf',
    description: '自然绿色风格，舒适护眼',
    bg: 'bg-green-50',
    card: 'bg-white',
    text: 'text-green-900',
    primary: '#10B981',
  },
  purple: {
    name: '紫色主题',
    icon: 'fa-diamond',
    description: '优雅紫色风格，创意设计',
    bg: 'bg-purple-50',
    card: 'bg-white',
    text: 'text-purple-900',
    primary: '#8B5CF6',
  },
  large: {
    name: '大字主题',
    icon: 'fa-font',
    description: '大字体模式，适合阅读和视力保护',
    bg: 'bg-gray-50',
    card: 'bg-white',
    text: 'text-gray-900',
    primary: '#165DFF',
  },
}

const theme = ref<Theme>('light')

// 应用主题到 DOM
const applyTheme = (newTheme: Theme) => {
  const root = document.documentElement
  const body = document.body
  
  // 移除所有主题类
  root.classList.remove('theme-light', 'theme-dark', 'theme-blue', 'theme-green', 'theme-purple', 'theme-large')
  body.classList.remove('theme-large-font')
  
  // 添加新主题类
  root.classList.add(`theme-${newTheme}`)
  
  // 如果是大字主题，添加字体大小类
  if (newTheme === 'large') {
    body.classList.add('theme-large-font')
  }
  
  // 设置CSS变量（使用CSS变量定义的值）
  const config = themeConfig[newTheme]
  root.style.setProperty('--theme-primary', config.primary)
}

// 从 localStorage 读取主题设置
const loadTheme = () => {
  if (typeof window === 'undefined') return
  
  const savedTheme = localStorage.getItem('theme') as Theme | null
  const validThemes: Theme[] = ['light', 'dark', 'blue', 'green', 'purple', 'large']
  if (savedTheme && validThemes.includes(savedTheme)) {
    theme.value = savedTheme
  } else {
    // 默认使用浅色主题
    theme.value = 'light'
  }
  applyTheme(theme.value)
}

// 初始化主题（在应用启动时立即调用）
if (typeof window !== 'undefined') {
  // 立即加载并应用主题
  loadTheme()
  
  // 确保在DOM加载完成后再次应用主题（防止被覆盖）
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      applyTheme(theme.value)
    })
  } else {
    // DOM已经加载完成，立即应用
    applyTheme(theme.value)
  }
  
  // 监听系统主题变化（仅用于默认主题选择）
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  mediaQuery.addEventListener('change', (e) => {
    // 如果用户没有手动设置过主题，则跟随系统
    if (!localStorage.getItem('theme')) {
      theme.value = e.matches ? 'dark' : 'light'
      applyTheme(theme.value)
    }
  })
}

export function useTheme() {
  // 切换主题
  const toggleTheme = () => {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    localStorage.setItem('theme', theme.value)
    applyTheme(theme.value)
  }

  // 设置主题
  const setTheme = (newTheme: Theme) => {
    theme.value = newTheme
    localStorage.setItem('theme', theme.value)
    applyTheme(theme.value)
  }

  return {
    theme,
    toggleTheme,
    setTheme,
  }
}

