import path from "path";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig({
    plugins: [react(), tailwindcss()],
    define: {
        global: 'window',
    },
    resolve: {
        alias: {
            "@": path.resolve(import.meta.dirname, "./src"),
        },
    },
    build: {
        rolldownOptions: {
            output: {
                codeSplitting: {
                    // Shared icons/primitives otherwise become dozens of tiny
                    // requests before the first page can render on a cold load.
                    groups: [
                        { name: 'ui-icons', test: /node_modules[\\/]lucide-react[\\/]/, minShareCount: 2 },
                        { name: 'ui-primitives', test: /node_modules[\\/]@base-ui[\\/]/, minShareCount: 2 },
                    ],
                },
            },
        },
    },
});
