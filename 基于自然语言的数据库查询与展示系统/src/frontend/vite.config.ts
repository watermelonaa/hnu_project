import path from 'path';
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue'; 


export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, '.', ''); // 加载环境变量
    
    return {
        server: {
            // 端口、Host 和 Proxy 配置
            port: 3000, 
            host: '0.0.0.0',
            proxy: {
                // 代理通义千问请求（关键配置，必须在 /api 之前）
                '/api/qwen': {
                    target: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
                    changeOrigin: true, // 开启跨域代理
                    rewrite: (p) => p.replace(/^\/api\/qwen/, ''), // 重写路径
                    headers: {
                        // 注入API密钥（仅开发环境安全，生产需用后端代理）
                        Authorization: `Bearer ${env.VITE_QWEN_API_KEY}`,
                    },
                },
                // 代理后端API请求到Spring Boot服务器
                '/api': {
                    target: 'http://localhost:8173',
                    changeOrigin: true,
                    rewrite: (path) => path.replace(/^\/api/, ''),
                    // 添加错误处理，避免连接失败时阻塞开发
                    configure: (proxy, _options) => {
                        proxy.on('error', (err, _req, _res) => {
                            console.warn('代理错误（后端服务可能未启动）:', err.message);
                        });
                    },
                },
            },
        },
        
        plugins: [
            vue(), 
        ],
        
        define: {
            'process.env.API_KEY': JSON.stringify(env.GEMINI_API_KEY),
            'process.env.GEMINI_API_KEY': JSON.stringify(env.GEMINI_API_KEY)
        },
        
        resolve: {
            alias: {
                '@': path.resolve(__dirname, '.'), 
            }
        },
        
        optimizeDeps: {
            include: ['sockjs-client', '@stomp/stompjs']
        }
    };
});