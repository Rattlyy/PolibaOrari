// Create http server
import express from "express";

const app = express()

const {createServer} = await import('vite')
/** @type {import('vite').ViteDevServer | undefined} */
let vite = await createServer({
    server: {middlewareMode: true},
    appType: 'custom',
    base: '/',
})
app.use(vite.middlewares)

// Start http server
app.listen(5173, () => {
    console.log(`Server started at http://localhost:5173`)
})

app.use('*all', async (req, res) => {
    try {
        /** @type {import('./src/entrypoints/entry-dev-server.tsx').render} */
        let render = (await vite.ssrLoadModule('./src/entrypoints/entry-dev-server.tsx')).render

        const {pipe} = render(req.path, {
            bootstrapModules: [`src/entrypoints/entry-client.tsx`],
            onShellReady: () => {
                pipe(res)
            },
        })

        res.status(200)
        res.set({'Content-Type': 'text/html'})

        res.write(
            // PS: This will produce invalid HTML, but it's fine for development :)
            `
        <script type="module" src="http://localhost:5173/@vite/client"></script>

        <script type="module">
            import RefreshRuntime from 'http://localhost:5173/@react-refresh'
            RefreshRuntime.injectIntoGlobalHook(window)
            window.$RefreshReg$ = () => {}
            window.$RefreshSig$ = () => (type) => type
            window.__vite_plugin_react_preamble_installed__ = true
        </script>
        `
        )

        render(req.url, res)
    } catch (e) {
        vite?.ssrFixStacktrace(e)
        console.log(e.stack)
        res.status(500).end(e.stack)
    }
})