import {z} from "zod";

const corsiSchema = z.array(
    z.object({
        id: z.string().optional(),
        titolo: z.string(),
        anno: z.number(),
    })
)

export async function corsi() {
    return fetch((import.meta.env.PROD ? "" : "http://localhost:8080") + "/api/corsi").then((res) => res.json()).then((data) => corsiSchema.parseAsync(data))
}

const lezioniSchema = z.array(
    z.object({
        id: z.string(),
        orarioInizio: z.number(),
        minuteInizio: z.number(),
        durataOre: z.number(),
        weekDay: z.number(),
        professore: z.string(),
        materia: z.string(),
        annoCorso: z.number(),
    })
)

// type from zod schema
export type Lesson = z.infer<typeof lezioniSchema>[0]

export async function calendario(id: string, annoCorso: number) {
    if (id === "loading" || annoCorso < 1 || annoCorso > 3) {
        return []
    }

    // create a map of corsoId to name
    return fetch("http://localhost:8080/api/calendario/" + id + "/" + annoCorso).then((res) => res.json()).then((data: []) => lezioniSchema.parseAsync(data))
}