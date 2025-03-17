package it.rattly

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asLoadingCache
import com.sksamuel.aedile.core.expireAfterWrite
import klite.*
import klite.jackson.JsonBody
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToStream
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.reflect.full.primaryConstructor
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

@OptIn(DelicateCoroutinesApi::class, ExperimentalSerializationApi::class)
fun main() = Server(
    requestIdGenerator = XRequestIdGenerator(),
    httpExchangeCreator = XForwardedHttpExchange::class.primaryConstructor!!,
).apply {
    Config.useEnvFile()

    // calculate semesters, if date between September and December then it's first semester, else second
    val now = LocalDate.now()
    val year = now.year
    val isFirstSemester = now.monthValue in 9..12
    val academicYear = year - (if (isFirstSemester) 0 else 1)
    val SEMESTER_START_DATE = LocalDate.of(academicYear, 10, 23)
    val SEMESTER_END_DATE = LocalDate.of(academicYear + 1, 5, 31)

    ssr("${SEMESTER_START_DATE.year}/${SEMESTER_END_DATE.year.toString().takeLast(2)}")

    useOnly<JsonBody>()
    context("/api") {
        val lezioniCache = Caffeine
            .newBuilder()
            .expireAfterWrite(if (Config.isDev) 1.milliseconds else 12.hours)
            .asLoadingCache<String, List<Lezione>> { val (id, anno) = it.split("///"); lessons(id, anno.toInt()) }

        get("/calendario/:id/:anno") {
            val id = pathParams["id"] ?: throw BadRequestException("id is required")
            val anno = pathParams["anno"]?.toIntOrNull() ?: throw BadRequestException("anno is required or not int")
            if (anno < 1 || anno > 3) throw BadRequestException("anno must be between 1 and 3")
            val key = "$id///$anno"

            json.encodeToStream(
                lezioniCache.get(key),
                startResponse(StatusCode.OK, contentType = "application/json")
            )
        }

        var cachedCorsi: List<Corso>? = null
        get("/corsi") {
            if (cachedCorsi == null)
                cachedCorsi = ids().map {
                    corso(it).let {
                        Corso(
                            id = it.id ?: "",
                            titolo = it.payload?.titolo ?: "",
                            anno = it.payload?.anniCorso?.firstOrNull()?.toIntOrNull() ?: 1
                        )
                    }
                }

            json.encodeToStream(cachedCorsi, startResponse(StatusCode.OK, contentType = "application/json"))
        }

        get("/ics/:id/:anno") {
            val id = pathParams["id"] ?: throw BadRequestException("id is required")
            val anno = pathParams["anno"]?.toIntOrNull() ?: throw BadRequestException("anno is required or not int")
            if (anno < 1 || anno > 3) throw BadRequestException("anno must be between 1 and 3")
            val key = "$id///$anno"

            val lessons = lezioniCache.get(key)
            val corso = cachedCorsi?.find { it.id == id }
                ?: throw BadRequestException("corso not found, please hit /api/corsi endpoint to warm cache")

            startResponse(
                StatusCode.OK,
                contentType = "text/calendar",
                fileName = "${corso.titolo}.ics"
            ).use { stream ->
                stream.write("BEGIN:VCALENDAR\n")
                stream.write("VERSION:2.0\n")
                stream.write("PRODID:-//rattly//rattly//IT\n")
                stream.write("CALSCALE:GREGORIAN\n")
                stream.write("METHOD:PUBLISH\n")

                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
                val startDate = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))

                lessons
                    .asSequence()
                    .filterNot { it.id.contains("empty") }
                    .forEach { lesson ->
                        val eventDate = startDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(lesson.weekDay)))

                        val start = eventDate.atTime(lesson.orarioInizio, lesson.minuteInizio)
                            .atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC)

                        val end = eventDate.atTime(lesson.orarioFine, lesson.minuteFine)
                            .atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC)

                        stream.write("BEGIN:VEVENT\n")
                        stream.write("UID:${lesson.id}\n")
                        stream.write("DTSTAMP:${formatter.format(Instant.now())}\n")
                        stream.write("DTSTART:${formatter.format(start)}\n")
                        stream.write("DTEND:${formatter.format(end)}\n")

                        stream.write(
                            "RRULE:FREQ=WEEKLY;UNTIL=${
                                formatter.format(
                                    SEMESTER_END_DATE.atTime(23, 59).atZone(ZoneOffset.UTC)
                                )
                            }\n"
                        )

                        stream.write("SUMMARY:${lesson.materia}\n")
                        stream.write("LOCATION:${lesson.aula} - PoliBA\n")
                        stream.write("DESCRIPTION:${lesson.professore}\n")
                        stream.write("END:VEVENT\n")
                    }

                stream.write("END:VCALENDAR\n")
            }
        }
    }
}.start()

suspend fun lessons(id: String, anno: Int) =
    calendari(id).filter { (it.annoCorso?.toInt() ?: anno) == anno }.mapNotNull {
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
            aula = it.risorse?.firstOrNull { it?.aula != null }?.aula?.descrizione,
            professore = it.risorse
                ?.filter { it?.docente != null }
                ?.joinToString { "${it?.docente?.nome} ${it?.docente?.cognome}" } ?: "",
        )
    }.toMutableList().also { list ->
        list.groupBy { it.weekDay }.forEach { (day, lezioni) ->
            lezioni.forEachIndexed { i, it ->
                val next = lezioni.getOrNull(i + 1) ?: return@forEachIndexed
                if ((it.orarioFine + it.minuteFine) != (next.orarioInizio + next.minuteInizio)) {
                    val durata = Duration.of(
                        ((it.orarioFine - next.orarioInizio) * 60L) + (it.minuteFine - it.minuteInizio),
                        ChronoUnit.MINUTES
                    )

                    list.add(
                        Lezione(
                            id = "empty-$day-$i",
                            orarioInizio = it.orarioFine,
                            minuteInizio = it.minuteFine,
                            orarioFine = next.orarioInizio,
                            minuteFine = next.minuteInizio,
                            durata = "${if (durata.toHours() == 0L) "" else "${-durata.toHours()}h "}${-durata.toMinutesPart()}m",
                            durataOre = maxOf(1, durata.toMinutes() / 50).toInt(),
                            weekDay = day,
                            materia = "Pausa",
                            annoCorso = it.annoCorso,
                            aula = null,
                            professore = "",
                        )
                    )
                }
            }
        }
    }.sortedBy { it.orarioInizio }

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