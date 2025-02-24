package it.rattly

import fuel.httpGet
import fuel.httpPost
import it.rattly.objects.CalendarioItem
import it.rattly.objects.CorsiItem
import kotlinx.io.asInputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    isLenient = true
}

@OptIn(ExperimentalSerializationApi::class)
suspend fun corso(calendarioId: String) = json.decodeFromStream<CorsiItem>(
    "https://poliba.prod.up.cineca.it/api/LinkCalendario/searchCalendarioPubblico".httpPost(
        headers = mapOf("Content-Type" to "application/json;charset=utf-8"),
        // clienteId sensitive? idk
        body = "{\"linkCalendarioId\":\"$calendarioId\",\"filter\":{\"clienteId\":\"5a841031b15b88201b28c8d5\"}}"
    ).source.asInputStream()
)

@OptIn(ExperimentalSerializationApi::class)
suspend fun calendari(
    calendarioId: String,
    start: ZonedDateTime = Instant.now().atZone(ZoneId.of("UTC")),
    end: ZonedDateTime = start.plus(10, ChronoUnit.DAYS),
) =
    json.decodeFromStream<List<CalendarioItem>>(
        "https://poliba.prod.up.cineca.it/api/Impegni/getImpegniCalendarioPubblico".httpPost(
            body = "{\"mostraImpegniAnnullati\":true,\"mostraIndisponibilitaTotali\":false," +
                    "\"linkCalendarioId\":\"$calendarioId\",\"clienteId\":\"5a841031b15b88201b28c8d5\",\"pianificazioneTemplate\":true," +
                    "\"dataInizioTemplate\":\"${DateTimeFormatter.ISO_DATE_TIME.format(start).replace("[UTC]", "")}\"," +
                    "\"dataFineTemplate\":\"${DateTimeFormatter.ISO_DATE_TIME.format(end).replace("[UTC]", "")}\"}",
            headers = mapOf("Content-Type" to "application/json;charset=utf-8")
        ).source.asInputStream()
    )

suspend fun ids(): List<String> {
    val regex = Regex("(?<=data = ).*}")
    val url = "http://www.poliba.it/it/content/orari-delle-lezioni-20242025"
    val data = url.httpGet().source.asInputStream().bufferedReader().use { it.readText() }
    val matches = regex.findAll(data).toList()
    return Regex("linkCalendarioId=[a-f0-9]{24}").findAll(matches[0].value)
        .map { it.value.replace("linkCalendarioId=", "") }.toList().also { println("id list size: " + it.size) }
}