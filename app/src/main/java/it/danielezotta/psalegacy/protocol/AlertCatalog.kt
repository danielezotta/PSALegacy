package it.danielezotta.psalegacy.protocol

import java.util.Locale

/**
 * Local catalog of vehicle alert indicators (bits 0..255 of the trip message's
 * miscIndicatorsAndAlerts mask), extracted verbatim from the original MyPeugeot
 * APK string resources (ShortAlertVehicle_xxx / criticity_xxx). Codes not present
 * in the original app's tables (e.g. 109) resolve to null.
 */
object AlertCatalog {

    data class AlertInfo(
        val titleIt: String,
        val titleEn: String,
        val criticity: String? = null
    )

    private val CATALOG: Map<Int, AlertInfo> = mapOf(
        0 to AlertInfo("Pressione olio motore anomala", "Abnormal engine oil pressure", "HIGH"),
        1 to AlertInfo("Temperatura motore troppo elevata", "Engine temperature too high", "HIGH"),
        3 to AlertInfo("Anomalia impianto frenante", "Faulty braking system", "HIGH"),
        5 to AlertInfo("Anomalia servosterzo", "Faulty power steering", "MEDIUM"),
        6 to AlertInfo("Livello liquido di raffreddamento insufficiente", "Cooling circuit level too low", "HIGH"),
        8 to AlertInfo("Livello olio motore insufficiente", "Engine oil level too low", "HIGH"),
        17 to AlertInfo("Anomalia ESP / ASR", "Faulty ESP/ASR", "MEDIUM"),
        18 to AlertInfo("Anomalia batteria", "Faulty battery", "HIGH"),
        20 to AlertInfo("Anomalia del filtro gasolio", "Faulty diesel filter", "HIGH"),
        21 to AlertInfo("Stadio avanzato di usura delle pastiglie dei freni", "Advanced wear of brake pads", "MEDIUM"),
        22 to AlertInfo("Livello carburante basso", "Fuel level low", "HIGH"),
        23 to AlertInfo("Anomalia Airbag", "Faulty airbag(s)", "MEDIUM"),
        25 to AlertInfo("Anomalia del sistema antinquinamento", "Faulty emission control system", "HIGH"),
        26 to AlertInfo("Anomalia ABS", "Faulty ABS", "MEDIUM"),
        27 to AlertInfo("Rischio di intasamento del filtro antiparticolato (FAP)", "Risk of particle filter clogging", "HIGH"),
        29 to AlertInfo("Livello additivo del filtro antiparticolato insufficiente", "Particle filter additive level too low", "HIGH"),
        31 to AlertInfo("Anomalia al sistema delle sospensioni", "Faulty suspension system"),
        36 to AlertInfo("Anomalia bloccasterzo", "Faulty steering lock system"),
        37 to AlertInfo("Anomalia dell'antiavviamento elettronico", "Faulty electronic immobilizer", "HIGH"),
        38 to AlertInfo("Livello olio troppo alto", "Oil level too high"),
        43 to AlertInfo("Anomalia regolazione automatica dei fari", "Faulty automatic headlight adjustment", "MEDIUM"),
        44 to AlertInfo("Anomalia del sistema di trazione ibrida", "Faulty hybrid system"),
        45 to AlertInfo("Anomalia del sistema di trazione ibrida", "Faulty hybrid system"),
        46 to AlertInfo("Livello liquido lavacristalli insufficiente", "Windscreen washer fluid level too low", "HIGH"),
        52 to AlertInfo("Pressione pneumatico/i insufficiente", "Tyre pressure too low", "MEDIUM"),
        53 to AlertInfo("Sensore radar di guida sporco", "Tracking radar dirty"),
        59 to AlertInfo("Pressione pneumatico/i insufficiente", "Tyre pressure too low", "MEDIUM"),
        60 to AlertInfo("Pressione pneumatico/i insufficiente", "Tyre pressure too low", "MEDIUM"),
        61 to AlertInfo("Pressione pneumatico/i insufficiente", "Tyre pressure too low", "MEDIUM"),
        62 to AlertInfo("Pressione pneumatico/i insufficiente", "Tyre pressure too low", "MEDIUM"),
        63 to AlertInfo("Anomalia lampadina/e luci di posizione (Anteriore Sinistra)", "Faulty side marker light bulb(s) (Front Left)", "MEDIUM"),
        64 to AlertInfo("Anomalia lampadina/e luci di posizione (Anteriore Destra)", "Faulty side marker light bulb(s) (Front Right)", "MEDIUM"),
        65 to AlertInfo("Anomalia lampadina/e luci di posizione (Posteriore Destra)", "Faulty side marker light bulb(s) (Rear Right)", "MEDIUM"),
        66 to AlertInfo("Anomalia lampadina/e luci di posizione (Posteriore Sinistra)", "Faulty side marker light bulb(s) (Rear Left)", "MEDIUM"),
        67 to AlertInfo("Anomalia anabbaglianti", "Faulty dipped beams", "MEDIUM"),
        68 to AlertInfo("Anomalia anabbaglianti", "Faulty dipped beams", "MEDIUM"),
        69 to AlertInfo("Anomalia abbaglianti", "Faulty full beams", "MEDIUM"),
        70 to AlertInfo("Anomalia abbaglianti", "Faulty full beams", "MEDIUM"),
        71 to AlertInfo("Anomalia lampadina stop Posteriore Destra", "Faulty rear right brake light bulb", "MEDIUM"),
        72 to AlertInfo("Anomalia lampadina stop Posteriore Sinistra", "Faulty rear left brake light bulb", "MEDIUM"),
        73 to AlertInfo("Anomalia fendinebbia", "Faulty foglights", "MEDIUM"),
        74 to AlertInfo("Anomalia fendinebbia Anteriori", "Faulty front foglights", "MEDIUM"),
        75 to AlertInfo("Anomalia lampadina/e fendinebbia Posteriori Destra", "Faulty rear right foglight bulb(s)", "MEDIUM"),
        76 to AlertInfo("Anomalia lampadina/e fendinebbia Posteriori", "Faulty rear foglight bulb warning", "MEDIUM"),
        77 to AlertInfo("Anomalia indicatore/i di direzione (Anteriore Destro)", "Faulty indicator(s) (Front Right)", "MEDIUM"),
        78 to AlertInfo("Anomalia indicatore/i di direzione (Posteriore Destro)", "Faulty indicator(s) (Rear Right)", "MEDIUM"),
        79 to AlertInfo("Anomalia indicatore/i di direzione (Anteriore Sinistro)", "Faulty indicator(s) (Front Left)", "MEDIUM"),
        80 to AlertInfo("Anomalia indicatore/i di direzione (Posteriore Sinistro)", "Faulty indicator(s) (Rear Left)", "MEDIUM"),
        81 to AlertInfo("Anomalia luce di retromarcia", "Faulty reverse light", "MEDIUM"),
        82 to AlertInfo("Anomalia lampadina luce di retromarcia", "Faulty reverse light bulb", "MEDIUM"),
        91 to AlertInfo("Anomalia sistema di parcheggio assistito", "Faulty parking aid system", "HIGH"),
        94 to AlertInfo("Pressione pneumatico/i insufficiente", "Tyre pressure too low", "MEDIUM"),
        95 to AlertInfo("Pressione pneumatico/i insufficiente", "Tyre pressure too low", "MEDIUM"),
        96 to AlertInfo("Pressione pneumatico/i insufficiente", "Tyre pressure too low", "MEDIUM"),
        97 to AlertInfo("Pressione pneumatico/i insufficiente", "Tyre pressure too low", "MEDIUM"),
        100 to AlertInfo("Anomalia antinquinamento", "Faulty emission control system"),
        101 to AlertInfo("Anomalia antinquinamento", "Faulty emission control system"),
        102 to AlertInfo("Anomalia antinquinamento", "Faulty emission control system"),
        123 to AlertInfo("Anomalia sistema freno di stazionamento", "Faulty hand brake system", "MEDIUM"),
        126 to AlertInfo("Anomalia luce/i di curva", "Faulty directional headlight(s)", "MEDIUM"),
        133 to AlertInfo("Anomalia al cambio", "Faulty gearbox", "HIGH"),
        141 to AlertInfo("Malfunzionamento generale del motore", "General engine dysfunction", "HIGH"),
        142 to AlertInfo("Anomalia della sospensione", "Faulty suspension", "HIGH"),
        148 to AlertInfo("Pressione pneumatico insufficiente", "Tyre pressure too low", "MEDIUM"),
        149 to AlertInfo("Pressione pneumatico insufficiente", "Tyre pressure too low"),
        150 to AlertInfo("Pressione pneumatico insufficiente", "Tyre pressure too low"),
        151 to AlertInfo("Pressione pneumatico insufficiente", "Tyre pressure too low"),
        152 to AlertInfo("Anomalia della sospensione", "Faulty suspension", "MEDIUM"),
        153 to AlertInfo("Anomalia servosterzo", "Faulty power steering", "MEDIUM"),
        157 to AlertInfo("Anomalia motore", "Faulty engine"),
        159 to AlertInfo("Anomalia monitoraggio pressione pneumatici", "Faulty under-inflation monitoring system"),
        160 to AlertInfo("Pneumatico posteriore destro sgonfio", "Under-inflated rear right tyre"),
        161 to AlertInfo("Pneumatico posteriore sinistro sgonfio", "Under-inflated rear left tyre"),
        162 to AlertInfo("Pneumatico anteriore destro sgonfio", "Under-inflated front right tyre"),
        163 to AlertInfo("Pneumatico anteriore sinistro sgonfio", "Under-inflated front left tyre"),
        165 to AlertInfo("Livello additivo antinquinamento insufficiente", "Emission control additive level too low"),
        166 to AlertInfo("Livello additivo antinquinamento insufficiente", "Emission control additive level too low"),
        167 to AlertInfo("Livello additivo antinquinamento insufficiente", "Emission control additive level too low")
    )

    fun lookup(code: Int): AlertInfo? = CATALOG[code]

    fun title(code: Int, language: String? = Locale.getDefault().language): String? {
        val info = lookup(code) ?: return null
        return if (language == "it") info.titleIt else info.titleEn
    }

    fun criticity(code: Int): String? = lookup(code)?.criticity
}
