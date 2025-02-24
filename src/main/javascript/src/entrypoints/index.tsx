import {ReactNode} from "react";
import src from '@/index.css?inline'

export function Index({children}: { children: ReactNode }) {
    return <html lang={"en"} className={"dark"}>
    <head>
        <meta charSet="UTF-8"/>
        <link rel="icon" type="image/svg+xml" href="/vite.svg"/>
        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
        <title>PoliBA Orari</title>
        <link rel="apple-touch-icon" sizes="180x180"
              href="https://corsproxy.io/?https://gmmz.dev/apple-touch-icon.png"/>
        <link rel="icon" type="image/png" sizes="32x32"
              href="https://corsproxy.io/?https://gmmz.dev/favicon-32x32.png"/>
        <link rel="icon" type="image/png" sizes="16x16"
              href="https://corsproxy.io/?https://gmmz.dev/favicon-16x16.png"/>
        <link rel="mask-icon" href="https://corsproxy.io/?https://gmmz.dev/safari-pinned-tab.svg" color="#5bbad5"/>
        {/* Inline the CSS as a base64-encoded string to prevent FOUCs */}
        <link rel="stylesheet" type="text/css" href={"data:text/css;base64," + btoa(src)}></link>
    </head>
    <body>
    <div id="root">
        {children}
    </div>
    </body>
    </html>
}