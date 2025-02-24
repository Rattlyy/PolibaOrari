import {useEffect, useState} from "react"
import {motion, AnimatePresence} from "framer-motion"
import {Button} from "@/components/ui/button"
import LessonItem from "./LessonItem"
import {useQuery} from "@tanstack/react-query";
import {corsi as corsiFn} from "@/lib/api";
import {calendario as lezioniFn} from "@/lib/api";
import {toast} from "sonner"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"

const daysOfWeek = ["Lunedi", "Martedi", "Mercoledi", "Giovedi", "Venerdi"]

export default function Calendar() {
    const [selectedCorso, setSelectedCorso] = useState("")
    const [selectedDay, setSelectedDay] = useState(daysOfWeek[0])
    const {isLoading: corsiLoading, isError: isCorsiError, data: corsi, error: corsiError} = useQuery({
        queryKey: ["corsi"],
        queryFn: corsiFn
    })

    const {isLoading: lezioniLoading, isError: isLezioniError, data: lezioni, error: lezioniError} = useQuery({
        queryKey: ["lezioni", selectedCorso],
        queryFn: () => lezioniFn(selectedCorso.split("///")[0], parseInt(selectedCorso.split("///")[1]))
    })

    useEffect(() => {
        if (isLezioniError)
            toast("Errore caricamento lezioni", {description: () => lezioniError?.message})
    }, [isLezioniError, lezioniError]);

    useEffect(() => {
        if (isCorsiError)
            toast("Errore caricamento corsi", {description: () => corsiError?.message})
    }, [isCorsiError, corsiError]);

    return (
        <div className={`flex h-screen`}>
            <div className="flex-grow overflow-hidden flex flex-col">
                <header className="bg-primary text-primary-foreground p-4 flex justify-between items-center">
                    <img src={"https://www.poliba.it/sites/default/files/poliba.png"} width={60} height={60}
                         className={"animate-spin"} alt={"logo"}></img>
                    <div className="flex space-x-2">
                        {daysOfWeek.map((day) => (
                            <Button
                                key={day}
                                variant={selectedDay === day ? "secondary" : "ghost"}
                                size="default"
                                onClick={() => setSelectedDay(day)}
                            >
                                {day.slice(0, 3)}
                            </Button>
                        ))}
                    </div>
                </header>

                <div className={"p-6 pb-2"}>
                    <Select onValueChange={(e) => setSelectedCorso(e)}>
                        <SelectTrigger className="w-full">
                            <SelectValue placeholder="Corso"/>
                        </SelectTrigger>
                        <SelectContent>
                            {corsiLoading ? (
                                <SelectItem value={"loading"}>Loading...</SelectItem>
                            ) : isCorsiError ? (
                                <SelectItem value={"error"}>Error: {corsiError.message}</SelectItem>
                            ) : (
                                corsi?.sort()?.map((corso, i) => (
                                    <SelectItem key={i} value={corso.id + "///" + corso.anno}>
                                        {corso.titolo}
                                    </SelectItem>
                                ))
                            )}
                        </SelectContent>
                    </Select>
                </div>
                <div className="flex-grow overflow-auto p-6">
                    {lezioniLoading || isLezioniError ? <></> :
                        <AnimatePresence>
                            {lezioni
                                ?.filter((lesson) => lesson.weekDay === daysOfWeek.indexOf(selectedDay) + 1)
                                ?.map((lesson, i) =>
                                    <motion.div key={i}>
                                        <h2 className="text-lg font-bold">{lesson.orarioInizio + 1}:{lesson.minuteInizio < 10 ? "0" + lesson.minuteInizio : lesson.minuteInizio}</h2>
                                        <LessonItem key={lesson.materia} lesson={lesson}/>
                                    </motion.div>
                                )}
                        </AnimatePresence>}
                </div>
            </div>
        </div>
    )
}

