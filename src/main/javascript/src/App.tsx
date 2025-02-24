import {Route, Routes} from "react-router";
import {ReactNode} from "react";
import {ThemeProvider} from "@/components/theme-provider.tsx";
import Calendar from "@/components/Calendar.tsx";
import {QueryClient, QueryClientProvider} from "@tanstack/react-query";
import {ReactQueryDevtools} from "@tanstack/react-query-devtools";
import {Toaster} from "@/components/ui/sonner"

export function Providers({children}: { children: ReactNode }) {
    return <>
        <QueryClientProvider client={new QueryClient()}>
            <ThemeProvider>
                {children}
            </ThemeProvider>
        </QueryClientProvider>
        <Toaster/>
    </>
}

export function Routing() {
    return <Routes>
        <Route path={"/"} element={<App/>}/>
        <Route path={"*"} element={<div>Not Found</div>}/>
    </Routes>
}

function App() {
    return <>
        <Calendar/>
        <ReactQueryDevtools initialIsOpen={false}/>
    </>
}