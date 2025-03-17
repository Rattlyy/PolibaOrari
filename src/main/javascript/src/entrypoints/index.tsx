import {ReactNode} from "react";
import src from '@/index.css?inline'

export function Index({children, aa}: { children: ReactNode, aa: string }) {
    return <html lang={"it"} className={"dark"}>
    <head>
        <meta name="theme_color" content="#11151c"/>
        <meta charSet="UTF-8"/>
        <link rel="icon" type="image/svg+xml" href="/vite.svg"/>

        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
        <meta name="author" content="Giovanni Mazzone"/>
        <meta name="keywords"
              content="poliba, orari, politecnico, bari, università, universita, university, timetable, schedule"/>
        <meta name="description" content={`Orari del Politecnico di Bari`}/>
        <meta name="robots" content="index, follow"/>

        <meta property="og:title" content={"Orari PoliBA " + aa}/>
        <meta property="og:description" content={"Orari del Politecnico di Bari AA " + +aa}/>
        <meta property="og:image" content="https://www.poliba.it/sites/default/files/poliba.png"/>

        <title>{`PoliBA Orari - ${aa}`}</title>
        <link rel="manifest" href="/assets/manifest.webmanifest"/>
        <script async src="https://cdn.jsdelivr.net/npm/pwacompat" crossOrigin="anonymous"></script>
        <script async src="https://cdn.jsdelivr.net/npm/@khmyznikov/pwa-install/dist/pwa-install.bundle.js"
                type={"module"} crossOrigin="anonymous"></script>
        <script defer data-domain="orari.gmmz.dev" src="https://plausible.gmmz.dev/js/script.js"></script>
        <link rel="apple-touch-icon" sizes="180x180" href="/assets/apple-touch-icon.png"/>
        <link rel="icon" type="image/png" sizes="32x32" href="/assets/favicon-32x32.png"/>
        <link rel="icon" type="image/png" sizes="16x16" href="/assets/favicon-16x16.png"/>
        {/* Inline the CSS as a base64-encoded string to prevent FOUCs */}
        <link rel="stylesheet" type="text/css" href={"data:text/css;base64," + btoa(src)}></link>
    </head>
    <body>
    <div id="root">
        {children}
    </div>

    {/* @ts-ignore */}
    <pwa-install manifest-url="/assets/manifest.webmanifest"></pwa-install>
    </body>
    </html>
}