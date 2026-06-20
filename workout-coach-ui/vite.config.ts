/// <reference types="vitest" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api/v1/auth": {
        target: "http://localhost:8081",
        changeOrigin: true,
        secure: false,
      },
      "/api/v1/uploads": {
        target: "http://localhost:8082",
        changeOrigin: true,
        secure: false,
      },
      "/api/v1/vault": {
        target: "http://localhost:8082",
        changeOrigin: true,
        secure: false,
      },
      "/api/v1/sessions": {
        target: "http://localhost:8083",
        changeOrigin: true,
        secure: false,
      },
      "/api/v1/enrollments": {
        target: "http://localhost:8083",
        changeOrigin: true,
        secure: false,
      },
      "/api/v1/stats": {
        target: "http://localhost:8084",
        changeOrigin: true,
        secure: false,
      },
      "/api/v1/activities": {
        target: "http://localhost:8084",
        changeOrigin: true,
        secure: false,
      },
      "/ws/sessions": {
        target: "http://localhost:8083",
        changeOrigin: true,
        ws: true,
      },
    },
  },
  test: {
    globals: true,
    environment: "jsdom",
    setupFiles: "./src/test/setup.ts",
    css: true,
  },
});
