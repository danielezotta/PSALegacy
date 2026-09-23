package it.danielezotta.psalegacy.model

/** Peugeot models from the SMARTAPPS V1 head-unit era that have a bundled photo. */
enum class VehicleModel(val label: String) {
    P208("208"),
    P2008("2008"),
    P308("308"),
    P3008("3008"),
    P5008("5008"),
    P508("508"),
}

data class VehicleInfo(val brand: String?, val model: VehicleModel?, val modelYear: Int?) {
    /** "Peugeot 2008", "Peugeot", or null when nothing could be decoded. */
    val name: String? get() = listOfNotNull(brand, model?.label).joinToString(" ").ifEmpty { null }
}

/**
 * Best-effort decoding of a PSA VIN. Positions 1-3 are the WMI (brand), 4-5 the PSA family
 * code and 10 the model year. 3008 II and 5008 II share the "M" family, so they can't be
 * told apart from the VIN alone: those decode as 3008 and the user can override in Settings.
 */
object VinDecoder {
    private val BRANDS = mapOf(
        "VF3" to "Peugeot", "VR3" to "Peugeot",
        "VF7" to "Citroën", "VR7" to "Citroën",
        "VR1" to "DS",
    )

    fun decode(vin: String): VehicleInfo {
        val v = vin.trim().uppercase()
        if (v.length != 17) return VehicleInfo(null, null, null)
        val brand = BRANDS[v.substring(0, 3)]
        val model = if (brand == "Peugeot") peugeotModel(v.substring(3, 5)) else null
        return VehicleInfo(brand, model, modelYear(v[9]))
    }

    private fun peugeotModel(family: String): VehicleModel? = when {
        family == "CU" -> VehicleModel.P2008
        family[0] == 'C' -> VehicleModel.P208
        family[0] == 'L' -> VehicleModel.P308
        family[0] == 'M' -> VehicleModel.P3008
        family == "8D" || family == "8E" -> VehicleModel.P508
        else -> null
    }

    /** ISO 3779 year code, resolved to the 2001-2030 cycle (digits 2001-2009, letters 2010+). */
    fun modelYear(code: Char): Int? {
        if (code in '1'..'9') return 2000 + (code - '0')
        val index = "ABCDEFGHJKLMNPRSTVWXY".indexOf(code)
        return if (index >= 0) 2010 + index else null
    }
}
