import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
import path from "path";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

const firebaseWorker = readFileSync(path.resolve(import.meta.dirname, "public/firebase-messaging-sw.js"));
const firebaseWorkerHash = createHash("sha256").update(firebaseWorker).digest("hex");

// https://vite.dev/config/
export default defineConfig({
    plugins: [react(), tailwindcss()],
    define: {
        global: 'window',
        __FIREBASE_SW_HASH__: JSON.stringify(firebaseWorkerHash),
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
