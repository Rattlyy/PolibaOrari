import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react-swc'
import viteCompression from 'vite-plugin-compression';
import * as path from "node:path";
import type {UserConfig} from 'vite'

export default defineConfig(({command, isSsrBuild}) => {
    let config = {
        plugins: [react(), viteCompression()],
        ssr: {
            noExternal: true,
            target: 'webworker'
        },

        resolve: {
            alias: {
                "@": path.resolve(__dirname, "./src"),
            },
        },

        build: {
            rollupOptions: {
                input: {
                    index: path.resolve('./src/entrypoints/entry-client.tsx'),
                },

                output: {
                    manualChunks: {libs: ["framer-motion", "react", "react-dom"]}
                },

                external: [] as string[]
            }
        }
    } satisfies UserConfig

    if (command == "serve") {
        //@ts-ignore
        delete config.ssr
    }

    if (isSsrBuild) {
        //@ts-ignore
        delete config.build.rollupOptions.output.manualChunks

        config.build.rollupOptions.external = [
            "src/components/pwa-install.tsx",
        ]
    }

    return config
})
