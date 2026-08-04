package it.danielezotta.psalegacy.protocol

import it.danielezotta.psalegacy.model.Trip

interface SessionEvents {
    fun onConnected()
    fun onAuthenticated(btelType: Short, serviceStatus: Short)
    fun onActivationAck(tripCount: Int, serviceActive: Boolean)
    fun onTrip(trip: Trip)
    fun onError(message: String)
    fun onDisconnected()
}
