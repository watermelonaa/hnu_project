<!--
  @file components/admin/StatCard.vue
  @description 统计卡片组件

  功能：
  - 显示统计数值
  - 趋势指示器
  - 图标和标题
  - 用于仪表盘概览

  @author Frontend Team
-->
<template>
  <div class="bg-white rounded-xl p-6 shadow-sm card-hover">
    <div class="flex justify-between items-start">
      <div>
        <p class="text-gray-500 text-sm">{{ title }}</p>
        <h3 class="text-3xl font-bold mt-2">{{ value }}</h3>
        <p :class="[changeColor, 'text-sm mt-2']">
          <i :class="`fa ${changeIcon}`"></i> {{ change.value }}
        </p>
      </div>
      <div
        :class="`w-12 h-12 rounded-full ${bgColor} flex items-center justify-center ${textColor}`"
      >
        <i :class="`fa ${icon} text-xl`"></i>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface ChangeInfo {
  value: string
  type: 'positive' | 'negative' | 'neutral' | 'warning'
}

interface StatCardProps {
  icon: string
  title: string
  value: string
  change: ChangeInfo
  color: 'primary' | 'secondary' | 'success' | 'warning' | 'danger'
}

const props = defineProps<StatCardProps>()

const colorMapping = {
  primary: { bg: 'bg-primary/10', text: 'text-primary' },
  secondary: { bg: 'bg-secondary/10', text: 'text-secondary' },
  success: { bg: 'bg-success/10', text: 'text-success' },
  warning: { bg: 'bg-warning/10', text: 'text-warning' },
  danger: { bg: 'bg-danger/10', text: 'text-danger' },
}

const { bg, text } = colorMapping[props.color]

const changeColor = computed(() => {
  switch (props.change.type) {
    case 'positive':
      return 'text-success'
    case 'negative':
      return 'text-danger'
    case 'warning':
      return 'text-warning'
    default:
      return 'text-gray-500'
  }
})

const changeIcon = computed(() => {
  switch (props.change.type) {
    case 'positive':
      return 'fa-arrow-up'
    case 'negative':
      return 'fa-arrow-down'
    default:
      return 'fa-minus'
  }
})

const bgColor = computed(() => bg)
const textColor = computed(() => text)
</script>
