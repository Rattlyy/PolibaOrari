package it.rattly

import klite.*
import klite.jackson.JsonBody
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToStream
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.reflect.full.primaryConstructor

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
        get("/calendario/:id/:anno") {
            val id = pathParams["id"] ?: throw BadRequestException("id is required")
            val anno = pathParams["anno"]?.toIntOrNull() ?: throw BadRequestException("anno is required or not int")
            if (anno < 1 || anno > 3) throw BadRequestException("anno must be between 1 and 3")
            val key = "$id///$anno"

            json.encodeToStream(
                CinecaAPI.lezioniCache.get(key),
                startResponse(StatusCode.OK, contentType = "application/json")
            )
        }

        get("/corsi") {
            json.encodeToStream(CinecaAPI.fetchCorsi(), startResponse(StatusCode.OK, contentType = "application/json"))
        }

        get("/ics/:id/:anno") {
            val id = pathParams["id"] ?: throw BadRequestException("id is required")
            val anno = pathParams["anno"]?.toIntOrNull() ?: throw BadRequestException("anno is required or not int")
            if (anno < 1 || anno > 3) throw BadRequestException("anno must be between 1 and 3")
            val key = "$id///$anno"

            val lessons = CinecaAPI.lezioniCache.get(key)
            val corso = CinecaAPI.fetchCorsi().find { it.id == id }
                ?: throw NotFoundException("corso not found")

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
