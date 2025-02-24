package it.rattly

import klite.BadRequestException
import klite.Config
import klite.Server
import klite.XForwardedHttpExchange
import klite.XRequestIdGenerator
import klite.jackson.JsonBody
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import kotlin.reflect.full.primaryConstructor

@OptIn(DelicateCoroutinesApi::class, ExperimentalSerializationApi::class)
fun main() = Server(
    requestIdGenerator = XRequestIdGenerator(),
    httpExchangeCreator = XForwardedHttpExchange::class.primaryConstructor!!
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
                    DateTimeFormatter.ISO_DATE_TIME.parse(it.orarioFine ?: return@mapNotNull null).get(ChronoField.HOUR_OF_DAY)
                val parsedWeekDay =
                    DateTimeFormatter.ISO_DATE_TIME.parse(it.dataInizio ?: return@mapNotNull null).get(ChronoField.DAY_OF_WEEK)

                Lezione(
                    id = it.id ?: "",
                    orarioInizio = parsedOrarioInizio.get(ChronoField.HOUR_OF_DAY),
                    minuteInizio = parsedOrarioInizio.get(ChronoField.MINUTE_OF_HOUR),
                    durataOre = parsedOrarioFine - parsedOrarioInizio.get(ChronoField.HOUR_OF_DAY),
                    weekDay = parsedWeekDay,
                    professore =
                        it.risorse
                            ?.filter { it?.docente != null }
                            ?.joinToString { "${it?.docente?.nome} ${it?.docente?.cognome}" } ?: "",
                    materia = it.nome ?: "",
                    annoCorso = it.annoCorso?.toInt() ?: 1
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
    val durataOre: Int,
    val weekDay: Int,
    val professore: String,
    val materia: String,
    val annoCorso: Int,
)