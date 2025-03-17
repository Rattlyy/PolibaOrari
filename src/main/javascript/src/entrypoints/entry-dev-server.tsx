import {renderToPipeableStream, RenderToPipeableStreamOptions} from "react-dom/server";
import {StrictMode} from "react";
import {Index} from "@/entrypoints/index.tsx";
import {Providers, Routing} from "@/App.tsx";
import {StaticRouter} from "react-router";

export function render(path: string, options?: RenderToPipeableStreamOptions) {
    return renderToPipeableStream(
        <StrictMode>
            <Index aa={"2024/25"}>
                <Providers>
                    <StaticRouter location={path}>
                        <Routing/>
                    </StaticRouter>
                </Providers>
            </Index>
        </StrictMode>, options
    )
}