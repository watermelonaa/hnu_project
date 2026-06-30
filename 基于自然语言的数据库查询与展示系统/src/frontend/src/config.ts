/**
 * @file config.ts
 * @description 应用配置文件
 *
 * 配置项：
 * - api.baseUrl: API 基础地址
 * - api.useMock: 是否使用模拟数据
 *
 * @author Frontend Team
 */
export const config = {
  api: {
    // 从环境变量读取API地址，开发环境使用代理，生产环境使用相对路径
    baseUrl: import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? '' : '/api'),
    useMock: false, // 使用真实API，不使用模拟数据
  },
}
