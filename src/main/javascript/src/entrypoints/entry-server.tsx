import {StrictMode} from 'react'
import {
    renderToReadableStream,
} from 'react-dom/server'
import {Index} from "@/entrypoints/index.tsx";
import {Providers, Routing} from "@/App.tsx";
import {StaticRouter} from "react-router";

export async function ssr(
    path: string,
    indexJSFile: string | undefined,
    obj: any
) {
    let didError = false
    let toThrow = null
    const stream = await renderToReadableStream(
        <StrictMode>
            <Index>
                <Providers>
                    <StaticRouter location={path}>
                        <Routing/>
                    </StaticRouter>
                </Providers>
            </Index>
        </StrictMode>,

        {
            bootstrapModules: (indexJSFile ? [indexJSFile] : undefined),
            onError(error: any) {
                didError = true
                toThrow = error
            }
        },
    )

    await stream.allReady

    if (didError) {
        throw toThrow;
    }

    obj.startResponse(didError ? "error" : "success")
    obj.header('Content-Type', 'text/html')

    await stream.pipeTo(new WritableStream<any>({
        write(chunk) {
            obj.stream().write(chunk)
        },

        abort(error) {
            didError = true
            toThrow = error
        }
    }))

    if (didError)
        throw toThrow
}