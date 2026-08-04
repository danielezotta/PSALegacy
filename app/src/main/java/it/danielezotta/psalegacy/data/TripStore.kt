package it.danielezotta.psalegacy.data

import android.content.Context
import it.danielezotta.psalegacy.model.Trip
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object TripStore {
    private const val FILE_NAME = "trips.json"

    private var file: File? = null

    fun init(context: Context) {
        file = File(context.filesDir, FILE_NAME)
    }

    @Synchronized
    fun loadTrips(vin: String): List<Trip> = loadAllTrips().filter { it.vin == vin }

    @Synchronized
    fun loadAllTrips(): List<Trip> {
        val f = requireNotNull(file) { "TripStore.init(context) must be called first" }
        if (!f.exists()) return emptyList()
        return try {
            fromJsonArray(JSONArray(f.readText()))
        } catch (e: Exception) {
            val backup = File(f.parentFile, "${f.name}.corrupt")
            try {
                f.renameTo(backup)
            } catch (_: Exception) {
            }
            emptyList()
        }
    }

    @Synchronized
    fun saveTrips(vin: String, trips: List<Trip>) {
        val f = requireNotNull(file) { "TripStore.init(context) must be called first" }
        val merged = mergeTrips(loadAllTrips(), trips, vin)
        f.parentFile?.mkdirs()
        writeAtomic(f, toJsonArray(merged).toString())
    }

    fun mergeTrips(existing: List<Trip>, incoming: List<Trip>, vin: String): List<Trip> {
        val others = existing.filter { it.vin != vin }
        val mergedForVin = existing
            .filter { it.vin == vin }
            .associateBy { it.tripNumber }
            .toMutableMap()
        for (t in incoming) mergedForVin[t.tripNumber] = t
        return (others + mergedForVin.values).sortedBy { it.startEpochMs }
    }

    private fun writeAtomic(target: File, content: String) {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.writeText(content)
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }

    fun toJsonArray(trips: List<Trip>): JSONArray {
        val array = JSONArray()
        for (t in trips) array.put(toJson(t))
        return array
    }

    fun toJson(t: Trip): JSONObject = JSONObject().apply {
        put("vin", t.vin)
        put("tripNumber", t.tripNumber)
        put("startEpochMs", t.startEpochMs)
        put("endEpochMs", t.endEpochMs)
        put("startMileageKm", t.startMileageKm.toDouble())
        put("endMileageKm", t.endMileageKm.toDouble())
        put("distanceKm", t.distanceKm.toDouble())
        put("travelTimeMs", t.travelTimeMs)
        put("fuelConsumptionL", t.fuelConsumptionL.toDouble())
        put("fuelLevel", t.fuelLevel)
        put("fuelAutonomyKm", t.fuelAutonomyKm)
        put("otherEnergyType", t.otherEnergyType)
        put("otherEnergyLevel", t.otherEnergyLevel)
        put("otherEnergyAutonomyKm", t.otherEnergyAutonomyKm)
        put("startLat", t.startLat.toDouble())
        put("startLon", t.startLon.toDouble())
        put("startAltitude", t.startAltitude)
        put("endLat", t.endLat.toDouble())
        put("endLon", t.endLon.toDouble())
        put("endAltitude", t.endAltitude)
        put("destinationLat", t.destinationLat.toDouble())
        put("destinationLon", t.destinationLon.toDouble())
        put("distanceToNextMaintenance", t.distanceToNextMaintenance)
        put("daysUntilNextMaintenance", t.daysUntilNextMaintenance)
        put("maintenancePassed", t.maintenancePassed)
        put("endAddress", t.endAddress)
        put("destinationAddress", t.destinationAddress)
        put("alertCodes", JSONArray(t.alertCodes))
    }

    fun fromJsonArray(array: JSONArray): List<Trip> {
        val result = mutableListOf<Trip>()
        for (i in 0 until array.length()) {
            result.add(fromJson(array.getJSONObject(i)))
        }
        return result
    }

    fun fromJson(o: JSONObject): Trip {
        val codes = mutableListOf<Int>()
        val codeArray = o.optJSONArray("alertCodes")
        if (codeArray != null) {
            for (i in 0 until codeArray.length()) codes.add(codeArray.getInt(i))
        }
        return Trip(
            vin = o.getString("vin"),
            tripNumber = o.getLong("tripNumber"),
            startEpochMs = o.getLong("startEpochMs"),
            endEpochMs = o.getLong("endEpochMs"),
            startMileageKm = o.getDouble("startMileageKm").toFloat(),
            endMileageKm = o.getDouble("endMileageKm").toFloat(),
            distanceKm = o.getDouble("distanceKm").toFloat(),
            travelTimeMs = o.getLong("travelTimeMs"),
            fuelConsumptionL = o.getDouble("fuelConsumptionL").toFloat(),
            fuelLevel = o.getInt("fuelLevel"),
            fuelAutonomyKm = o.getInt("fuelAutonomyKm"),
            otherEnergyType = o.getInt("otherEnergyType"),
            otherEnergyLevel = o.getInt("otherEnergyLevel"),
            otherEnergyAutonomyKm = o.getInt("otherEnergyAutonomyKm"),
            startLat = o.getDouble("startLat").toFloat(),
            startLon = o.getDouble("startLon").toFloat(),
            startAltitude = o.getInt("startAltitude"),
            endLat = o.getDouble("endLat").toFloat(),
            endLon = o.getDouble("endLon").toFloat(),
            endAltitude = o.getInt("endAltitude"),
            destinationLat = o.getDouble("destinationLat").toFloat(),
            destinationLon = o.getDouble("destinationLon").toFloat(),
            distanceToNextMaintenance = o.getInt("distanceToNextMaintenance"),
            daysUntilNextMaintenance = o.getInt("daysUntilNextMaintenance"),
            maintenancePassed = o.getBoolean("maintenancePassed"),
            endAddress = if (o.isNull("endAddress")) null else o.getString("endAddress"),
            destinationAddress = if (o.isNull("destinationAddress")) null else o.getString("destinationAddress"),
            alertCodes = codes
        )
    }
}
