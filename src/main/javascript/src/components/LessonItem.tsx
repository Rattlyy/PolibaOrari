import {motion} from "framer-motion"
import {Lesson} from "@/lib/api.ts";
import {Badge} from "@/components/ui/badge.tsx";

interface LessonItemProps {
    lesson: Lesson
}

export default function LessonItem({lesson}: LessonItemProps) {
    return (
        <motion.div
            layout
            initial={{opacity: 0, scale: 0.8, y: 20}}
            animate={{opacity: 1, scale: 1, y: 0}}
            exit={{opacity: 0, scale: 0.8, y: -20}}
            transition={{type: "spring", stiffness: 500, damping: 30}}
            className={"flex w-full"}
            whileHover={{scale: 1.02}}
            whileTap={{scale: 0.98}}
        >
            <div className={"flex flex-col pr-2"}>
                {Array.from({length: lesson.durataOre}, (_, i) =>
                    <div key={i}
                         className={"h-full w-4 bg-accent rounded-md mb-2 text-center text-sm flex items-center justify-center"}>{i + 1}</div>)}
            </div>
            <div
                className={"bg-accent w-full text-accent-foreground rounded-lg flex justify-between items-center shadow-md mb-2 pl-4 pr-4"}
                style={{paddingTop: lesson.durataOre * 10 + "px", paddingBottom: lesson.durataOre * 10 + "px"}}>
                <div>
                    <h3 className="font-medium">{lesson.materia}</h3>
                    <p className="text-sm text-muted-foreground">{lesson.professore}</p>
                </div>

                {lesson.aula ? <Badge className={"text-center ml-4"}>{lesson.aula}</Badge> : null}
            </div>
        </motion.div>
    )
}

