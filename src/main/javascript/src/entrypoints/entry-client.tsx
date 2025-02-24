import {StrictMode} from 'react'
import {hydrateRoot} from 'react-dom/client'
import {BrowserRouter} from "react-router";
import {Providers, Routing} from "@/App.tsx";

hydrateRoot(
    document.getElementById('root') as HTMLElement,
    <StrictMode>
        <Providers>
            <BrowserRouter>
                <Routing/>
            </BrowserRouter>
        </Providers>
    </StrictMode>,
)
