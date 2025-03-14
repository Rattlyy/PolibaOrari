import {z} from "zod";

const corsiSchema = z.array(
    z.object({
        id: z.string().optional(),
        titolo: z.string(),
        anno: z.number(),
    })
)
export const url = (import.meta.env.PROD ? "" : "http://localhost:8080")
export async function corsi() {
    return fetch(url + "/api/corsi")
        .then((res) => res.json()).then((data) => corsiSchema.parseAsync(data))
}

const lezioniSchema = z.array(
    z.object({
        id: z.string(),
        orarioInizio: z.number(),
        minuteInizio: z.number(),
        orarioFine: z.number(),
        minuteFine: z.number(),
        durata: z.string(),
        durataOre: z.number(),
        weekDay: z.number(),
        professore: z.string(),
        materia: z.string(),
        annoCorso: z.number(),
        aula: z.string().nullable()
    })
)

export type Lesson = z.infer<typeof lezioniSchema>[0]

export async function calendario(id: string, annoCorso: number) {
    if (id === "loading" || id === undefined || id == null || (annoCorso < 1 || annoCorso > 3) || isNaN(annoCorso)) {
        return []
    }

    return fetch(url + "/api/calendario/" + id + "/" + annoCorso)
        .then((res) => res.json()).then((data: []) => lezioniSchema.parseAsync(data))
}