import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const { CLINCANYWHERE_API_TARGET = 'http://127.0.0.1:8080' } = loadEnv(mode, process.cwd(), 'CLINCANYWHERE_');
  return {
    plugins: [react()],
    css: { postcss: { plugins: [] } },
    server: {
      proxy: {
        '/api': {
          target: CLINCANYWHERE_API_TARGET,
          configure(proxy) {
            proxy.on('proxyReq', proxyRequest => proxyRequest.removeHeader('origin'));
          },
        },
        '/ws': { target: CLINCANYWHERE_API_TARGET.replace(/^http/, 'ws'), ws: true },
      },
    },
  };
});
