import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

export default defineConfig({
  plugins: [react()],
  base: "/tech-pulse/",
  build: {
    outDir: path.resolve(__dirname, "../target/classes/static"),
    emptyOutDir: true
  }
});
