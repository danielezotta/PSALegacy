package it.danielezotta.psalegacy.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertCatalogTest {

    @Test
    fun `known codes resolve in both languages`() {
        assertEquals("Anomalia batteria", AlertCatalog.title(18, "it"))
        assertEquals("Faulty battery", AlertCatalog.title(18, "en"))
        assertEquals("Anomalia impianto frenante", AlertCatalog.title(3, "it"))
        assertEquals("Faulty braking system", AlertCatalog.title(3, "en"))
        assertEquals("Anomalia motore", AlertCatalog.title(157, "it"))
        assertEquals("Faulty engine", AlertCatalog.title(157, "en"))
    }

    @Test
    fun `unknown codes return null`() {
        assertNull(AlertCatalog.title(109, "it"))
        assertNull(AlertCatalog.title(109, "en"))
        assertNull(AlertCatalog.title(255, "it"))
        assertNull(AlertCatalog.title(-1, "it"))
    }

    @Test
    fun `criticity resolves for defined codes`() {
        assertEquals("HIGH", AlertCatalog.criticity(18))
        assertEquals("MEDIUM", AlertCatalog.criticity(21))
        assertEquals("HIGH", AlertCatalog.criticity(141))
    }

    @Test
    fun `criticity is null for undefined codes`() {
        assertNull(AlertCatalog.criticity(109))
        assertNull(AlertCatalog.criticity(31))
    }
}
