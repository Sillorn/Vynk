import { defineConfig } from 'vite-plus';

export default defineConfig({
  lint: {
    ignorePatterns: ['.angular/**', 'dist/**', 'node_modules/**'],
  },
});
