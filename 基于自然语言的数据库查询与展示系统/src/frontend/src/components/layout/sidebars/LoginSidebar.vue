<!--
  @file components/layout/sidebars/LoginSidebar.vue
  @description 登录页面右侧装饰栏

  功能：
  - 展示系统 Logo 和标语
  - 功能特性展示
  - 仅在大屏幕显示

  @author Frontend Team
-->
<template>
  <div class="hidden lg:flex flex-col items-center justify-center bg-blue-50 p-12 text-center">
    <div class="max-w-md">
      <h3 class="text-3xl font-bold text-primary mb-4">智能数据，一语洞穿</h3>
      <p class="text-gray-600 mb-8">
        无需复杂的SQL，只需用您最熟悉的自然语言提问，即可获得精准的数据洞察、直观的可视化图表，并轻松与团队分享。
      </p>
      <ul class="space-y-4 text-left inline-block">
        <li class="flex items-center text-gray-700">
          <i class="fa fa-magic text-primary w-6 text-lg"></i><span>自然语言查询</span>
        </li>
        <li class="flex items-center text-gray-700">
          <i class="fa fa-pie-chart text-primary w-6 text-lg"></i><span>智能图表生成</span>
        </li>
        <li class="flex items-center text-gray-700">
          <i class="fa fa-share-alt text-primary w-6 text-lg"></i><span>结果轻松分享</span>
        </li>
      </ul>
    </div>
    <div class="mt-8">
      <img :src="illustrationSvg" alt="Data Analysis Illustration" class="w-full max-w-sm h-auto" />
    </div>
  </div>
</template>

<script setup lang="ts">
//插图
const illustrationSvg =
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNTEyIiBoZWlnaHQ9IjUxMiIgdmlld0JveD0iMCAwIDUxMiA1MTIiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PHBhdGggZmlsbD0iI0VBRUlGRiIgZD0iTTQzMyAxNDFhMTQgMTQgMCAwIDAtMTQgMTQuMDQ0djE5OC45MTJBMTQgMTQgMCAwIDAgNDMzIDM2OGgxNFYxNDFoLTE0eiIvPjxwYXRoIGZpbGw9IiNDRUQ4RkYiIGQ9Ik0zMzUgMTg0YTQgNCAwIDAgMC00IDR2MTM2YTQgNCAwIDAgMCA4IDBWMTg4YTQgNCAwIDAgMC00LTR6TTI1NSA5N2E0IDQgMCAwIDAtNCA0djI1MGE0IDQgMCAwIDAgOCAwdl0yNTBhNCA0IDAgMCAwLTQtNHpNMzc1IDIyN2E0IDQgMCAwIDAtNCA0djg5YTQgNCAwIDAgMCA4IDB2LTg5YTQgNCAwIDAgMC00LTR6Ii8+PHBhdGggZmlsbD0iI0VBRUlGRiIgZD0iTTgxIDIwMWExNCAxNCAwIDAgMC0xNCAxNC4wNDR2MTM3LjkyQTE0IDE0IDAgMCAwIDgxIDM2OGgxNFYyMDFIODF6Ii8+PHBhdGggZmlsbD0iI0NFRDhGRiIgZD0iTTE5NSA2MWExNCAxNCAwIDAgMC0xNCAxNC4wNDR2MjgwLjkyQTE0IDE0IDAgMCAwIDE5NSAzNjhIMTlWMjYwaDE2NHYtNTZIMTlWOTFoMTc2VjYxaC0xNHptLTE2NCAxODVWMjEzaDE2NFYxNTVIMzF2NDh6bTE2NC05NVY5OWgtMTZWNzdhNCA0IDAgMCAwLTQgNFY2MWgtNDB2MTJhNCA0IDAgMCAwLTQgN3YxNkgzMXY0MGgxNjR6Ii8+PHBhdGggZmlsbD0iI0VBRUlGRiIgZD0iTTI5NiAyNTlhMTQgMTQgMCAwIDAtMTQgMTQuMDQ0djk0LjkyQTE0IDE0IDAgMCAwIDI5NiAzODJoMTRWMjU5aC0xNHpNMzU2IDMwOWExNCAxNCAwIDAgMC0xNCAxNC4wNDR2NTUuOTJBMTQgMTQgMCAwIDAgMzU2IDM5MWgxNFYzMDloLTM1eiIvPjxwYXRoIGZpbGw9IiNDRUQ4RkYiIGQ9Ik0xMzUgMTI0YTQgNCAwIDAgMC00IDR2MjE3YTQgNCAwIDAgMCA4IDBWMTE4YTQgNCAwIDAgMC00LTR6Ii8+PHBhdGggZmlsbD0iI0ZGRkZGRiIgZD0iTTQyMyAzODRINjlhMTQgMTQgMCAwIDAtMTQgMTR2NTRoMzg1VjM5OGExNCAxNCAwIDAgMC0xNC0xNHoiLz48cGF0aCBmaWxsPSIjQ0VEOEZGIiBkPSJNMzY4IDQyNEgxNDR2MTZoMjI0di0xNnpNMzYxIDQwNUg4NmExNCAxNCAwIDAgMCAwIDI4aDI3NWExNCAxNCAwIDAgMCAwLTI4eiIvPjxwYXRoIGZpbGw9IiNGNEY5RkYiIGQ9Ik01NSAzOTIuMTY0VjM5OGExNCAxNCAwIDAgMCAxNCAxNGg1NDFBMTQgMTQgMCAwIDEgNDM3IDQyNkg1OFY0MDZoMzAzdjEyaDI0di0xMmgyOHYxMmgyNHYtMTJoLTh2LTEySDU4djEyLjE2NEExMy45IDEzLjkgMCAwIDEgNTUgMzkyLjE2NHptMCAzMS44MzZWMzk4YTE0IDE0IDAgMCAxIDE0LTE0aDM1N2EzMyAzMyAwIDAgMSAzMyAzM2gtNDIydi0zMmgzMDN2LTguMTY0QTEzLjkgMTMuOSAwIDAgMSA0MjQgNDI0eiIvPjxwYXRoIGZpbGw9IiM0Q0I2RkYiIGQ9Ik0yMTEgMjg4aC0zM2wtMzUtOTBoLTI4bDQ3IDEyM2g0OGwxNi00NGg0NmwtOCA0NGg0Mmw0Ny0xMjNoLTM4bC0yMyA2M2gtNDVsMTYtNDVaIi8+PHBhdGggZmlsbD0iI0ZGRkZGRiIgZD0iTTk1IDQ1MmgyNHYyNEg5NXptMjA4IDBoMjR2MjRIMzAzem0xMjAgMGgyNHYyNGgtMjR6Ii8+PC9zdmc+'
</script>
