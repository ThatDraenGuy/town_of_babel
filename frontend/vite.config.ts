import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';
import { mockDevServerPlugin } from 'vite-plugin-mock-dev-server';

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react({
      babel: {
        plugins: [['babel-plugin-react-compiler']],
      },
    }),
    // mockDevServerPlugin({
    //   prefix: '^/api/v1',
    // }),
  ],
});
