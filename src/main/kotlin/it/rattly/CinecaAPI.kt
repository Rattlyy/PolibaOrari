@file:OptIn(ExperimentalSerializationApi::class)

package it.rattly

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asLoadingCache
import com.sksamuel.aedile.core.expireAfterWrite
import fuel.httpGet
import fuel.httpPost
import it.rattly.objects.CalendarioJsonEntry
import it.rattly.objects.CorsoJsonEntry
import klite.Config
import klite.isDev
import kotlinx.io.asInputStream
import kotlinx.io.readString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    isLenient = true
}

object CinecaAPI {
    internal var cachedCorsi: List<Corso>? = null
    val lezioniCache = Caffeine
        .newBuilder()
        .expireAfterWrite(if (Config.isDev) 1.milliseconds else 12.hours)
        .asLoadingCache<String, List<Lezione>> { val (id, anno) = it.split("///"); fetchLezioni(id, anno.toInt()) }

    suspend fun fetchCorsi() =
        cachedCorsi ?: ids().map {
            corso(it).let {
                Corso(
                    id = it.id ?: "",
                    titolo = it.payload?.titolo ?: "",
                    anno = it.payload?.anniCorso?.firstOrNull()?.toIntOrNull() ?: 1
                )
            }
        }.also { cachedCorsi = it }

    suspend fun fetchLezioni(id: String, anno: Int) =
        calendari(id).filter { (it.annoCorso?.toInt() ?: anno) == anno }.mapNotNull {
            val parsedOrarioInizio =
                DateTimeFormatter.ISO_DATE_TIME.parse(it.orarioInizio ?: return@mapNotNull null)
            val parsedOrarioFine =
                DateTimeFormatter.ISO_DATE_TIME.parse(it.orarioFine ?: return@mapNotNull null)
            val weekDay =
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
                weekDay = weekDay,

                materia = it.nome ?: "",
                annoCorso = it.annoCorso?.toInt() ?: 1,
                aula = it.risorse?.firstOrNull { it?.aula != null }?.aula?.descrizione,
                professore = it.risorse
                    ?.filter { it?.docente != null }
                    ?.joinToString { "${it?.docente?.nome} ${it?.docente?.cognome}" } ?: "",
            )
        }.let { originalList ->
            originalList.plus(
                originalList.groupBy { it.weekDay }
                    .flatMap { (day, lessons) ->
                        lessons.zipWithNext()
                            .mapIndexedNotNull { index, (current, next) ->
                                if ((current.orarioFine + current.minuteFine) == (next.orarioInizio + next.minuteInizio)) {
                                    null
                                } else {
                                    val duration = Duration.of(
                                        ((current.orarioFine - next.orarioInizio) * 60L) + (current.minuteFine - next.minuteInizio),
                                        ChronoUnit.MINUTES
                                    )

                                    Lezione(
                                        id = "break-$day-$index",
                                        orarioInizio = current.orarioFine,
                                        minuteInizio = current.minuteFine,
                                        orarioFine = next.orarioInizio,
                                        minuteFine = next.minuteInizio,
                                        durata = "${if (duration.toHours() == 0L) "" else "${-duration.toHours()}h "}${-duration.toMinutesPart()}m",
                                        durataOre = maxOf(1, duration.toMinutes() / 50).toInt(),
                                        weekDay = day,
                                        materia = "Pausa",
                                        annoCorso = current.annoCorso,
                                        aula = null,
                                        professore = ""
                                    )
                                }
                            }
                    }
            )
        }.sortedBy { it.orarioInizio }

    suspend fun corso(calendarioId: String) = json.decodeFromStream<CorsoJsonEntry>(
        "https://poliba.prod.up.cineca.it/api/LinkCalendario/searchCalendarioPubblico".httpPost(
            headers = mapOf("Content-Type" to "application/json;charset=utf-8"),
            // clienteId sensitive? idk probabilmente id del politecnico di bari
            body = "{\"linkCalendarioId\":\"$calendarioId\",\"filter\":{\"clienteId\":\"5a841031b15b88201b28c8d5\"}}"
        ).source.asInputStream()
    )

    suspend fun calendari(
        calendarioId: String,
        start: ZonedDateTime = Instant.now().atZone(ZoneId.of("UTC")),
        end: ZonedDateTime = start.plus(10, ChronoUnit.DAYS),
    ) =
        json.decodeFromStream<List<CalendarioJsonEntry>>(
            "https://poliba.prod.up.cineca.it/api/Impegni/getImpegniCalendarioPubblico".httpPost(
                body = "{\"mostraImpegniAnnullati\":true,\"mostraIndisponibilitaTotali\":false," +
                        "\"linkCalendarioId\":\"$calendarioId\",\"clienteId\":\"5a841031b15b88201b28c8d5\",\"pianificazioneTemplate\":true," +
                        "\"dataInizioTemplate\":\"${
                            DateTimeFormatter.ISO_DATE_TIME.format(start).replace("[UTC]", "")
                        }\"," +
                        "\"dataFineTemplate\":\"${DateTimeFormatter.ISO_DATE_TIME.format(end).replace("[UTC]", "")}\"}",
                headers = mapOf("Content-Type" to "application/json;charset=utf-8")
            ).source.asInputStream()
        )

    suspend fun ids() =
        Regex("linkCalendarioId=[a-f0-9]{24}").findAll(
            Regex("(?<=data = ).*}").findAll(
                "http://www.poliba.it/it/content/orari-delle-lezioni-20242025"
                    .httpGet().source.readString()
            ).first().value
        ).map { it.value.replace("linkCalendarioId=", "") }.toList()
}


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

    val aula: String?,
)