package it.rattly

import klite.*
import klite.jackson.JsonBody
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinWorkerThread
import kotlin.reflect.full.primaryConstructor

@OptIn(DelicateCoroutinesApi::class, ExperimentalSerializationApi::class)
fun main() = Server(
    requestIdGenerator = XRequestIdGenerator(),
    httpExchangeCreator = XForwardedHttpExchange::class.primaryConstructor!!,
    workerPool = ForkJoinPool(
        Runtime.getRuntime().availableProcessors(),
        ForkJoinPool.ForkJoinWorkerThreadFactory {
            object : ForkJoinWorkerThread(it) {
                override fun onStart() {
                    println("Adding ctx to $id")
                    threadCtxMap[id] = ctx()
                }
            }
        },
        null,
        false
    )
).apply {
    Config.useEnvFile()
    ssr()

    useOnly<JsonBody>()
    context("/api") {
        get("/calendario/:id/:anno") {
            val id = pathParams["id"] ?: throw BadRequestException("id is required")
            val anno = pathParams["anno"]?.toIntOrNull() ?: throw BadRequestException("anno is required or not int")
            if (anno < 1 || anno > 3) throw BadRequestException("anno must be between 1 and 3")

            calendari(id).mapNotNull {
                val parsedOrarioInizio =
                    DateTimeFormatter.ISO_DATE_TIME.parse(it.orarioInizio ?: return@mapNotNull null)
                val parsedOrarioFine =
                    DateTimeFormatter.ISO_DATE_TIME.parse(it.orarioFine ?: return@mapNotNull null)
                val parsedWeekDay =
                    DateTimeFormatter.ISO_DATE_TIME.parse(it.dataInizio ?: return@mapNotNull null)
                        .get(ChronoField.DAY_OF_WEEK)

                val durata = Duration.between(Instant.from(parsedOrarioInizio), Instant.from(parsedOrarioFine))

                Lezione(
                    id = it.id ?: "",
                    orarioInizio = parsedOrarioInizio.get(ChronoField.HOUR_OF_DAY),
                    minuteInizio = parsedOrarioInizio.get(ChronoField.MINUTE_OF_HOUR),
                    orarioFine = parsedOrarioFine.get(ChronoField.HOUR_OF_DAY),
                    minuteFine = parsedOrarioFine.get(ChronoField.MINUTE_OF_HOUR),

                    durata = "${durata.toHours()}h ${durata.toMinutesPart()}m",
                    durataOre = (durata.toMinutes() / 50).toInt(),
                    weekDay = parsedWeekDay,

                    materia = it.nome ?: "",
                    annoCorso = it.annoCorso?.toInt() ?: 1,
                    aula = it.risorse?.firstOrNull { it?.aula != null }?.aula?.descrizione ?: "",
                    professore = it.risorse
                        ?.filter { it?.docente != null }
                        ?.joinToString { "${it?.docente?.nome} ${it?.docente?.cognome}" } ?: "",
                )
            }
        }

        var cachedCorsi: List<Corso>? = null
        get("/corsi") {
            if (cachedCorsi == null)
                cachedCorsi = ids().map { corso(it) }.map {
                    Corso(
                        id = it.id ?: "",
                        titolo = it.payload?.titolo ?: "",
                        anno = it.payload?.anniCorso?.firstOrNull()?.toIntOrNull() ?: 1
                    )
                }

            cachedCorsi
        }
    }
}.start()

@Serializable
data class Corso(
    val id: String,
    val titolo: String,
    val anno: Int,
)

@Serializable
data class Lezione(
    val id: String,

    val orarioInizio: Int,
    val minuteInizio: Int,
    val orarioFine: Int,
    val minuteFine: Int,

    val durata: String,
    val durataOre: Int,
    val weekDay: Int,

    val professore: String,
    val materia: String,
    val annoCorso: Int,

    val aula: String,
)