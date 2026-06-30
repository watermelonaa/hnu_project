<!--
  @file views/SettingsPage.vue
  @description 设置页面

  功能：
  - 主题切换
  - 其他设置选项
-->
<template>
  <section class="p-6 overflow-y-auto bg-neutral h-full">
    <div class="max-w-4xl mx-auto space-y-6">
      <!-- 设置标题 -->
      <div class="bg-white rounded-xl shadow-sm border p-6">
        <h2 class="text-2xl font-bold flex items-center">
          <i class="fa fa-cog text-primary text-2xl mr-3"></i>
          设置
        </h2>
      </div>

      <!-- 外观设置 -->
      <div class="bg-white rounded-xl shadow-sm border p-6">
        <h3 class="text-lg font-semibold text-gray-700 mb-4">主题设置</h3>
        <div class="space-y-3">
          <div
            v-for="themeOption in themeOptions"
            :key="themeOption.value"
            @click="handleSelectTheme(themeOption.value)"
            :class="[
              'flex items-center justify-between p-4 rounded-lg transition-all cursor-pointer border-2',
              theme === themeOption.value
                ? 'bg-primary/10 border-primary shadow-md'
                : 'bg-gray-50 hover:bg-gray-100 border-transparent hover:border-gray-300'
            ]"
          >
            <div class="flex items-center space-x-4">
              <div :class="[
                'w-12 h-12 rounded-lg flex items-center justify-center text-xl',
                theme === themeOption.value ? 'bg-primary text-white' : 'bg-gray-200 text-gray-600'
              ]">
                <i :class="['fa', themeOption.icon]"></i>
              </div>
              <div>
                <span class="text-sm font-medium block">{{ themeOption.name }}</span>
                <p class="text-xs text-gray-500 mt-1">{{ themeOption.description }}</p>
              </div>
            </div>
            <div v-if="theme === themeOption.value" class="text-primary">
              <i class="fa fa-check-circle text-xl"></i>
            </div>
          </div>
        </div>
      </div>

      <!-- 其他设置 -->
      <div class="bg-white rounded-xl shadow-sm border p-6">
        <h3 class="text-lg font-semibold text-gray-700 mb-4">其他设置</h3>
        <div class="space-y-4">
          <div class="p-4 bg-gray-50 rounded-lg">
            <p class="text-sm text-gray-500">更多设置功能即将推出...</p>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useTheme, themeConfig, type Theme } from '../composables/useTheme'

const { theme, setTheme } = useTheme()

const themeOptions = computed(() => {
  return Object.entries(themeConfig).map(([value, config]) => ({
    value: value as Theme,
    name: config.name,
    icon: config.icon,
    description: config.description,
  }))
})

const handleSelectTheme = (selectedTheme: Theme) => {
  setTheme(selectedTheme)
}
</script>



