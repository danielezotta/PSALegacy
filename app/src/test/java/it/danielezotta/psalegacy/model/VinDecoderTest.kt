package it.danielezotta.psalegacy.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VinDecoderTest {

    private fun model(vin: String) = VinDecoder.decode(vin).model

    @Test
    fun `peugeot family codes map to models`() {
        assertEquals(VehicleModel.P2008, model("VF3CUHNSSJY113725"))
        assertEquals(VehicleModel.P208, model("VF3CA5GRMJW049855"))
        assertEquals(VehicleModel.P208, model("VF3CCHMRPJW104818"))
        assertEquals(VehicleModel.P308, model("VF3LB9HCGES091248"))
        assertEquals(VehicleModel.P3008, model("VF3MRHNSMLS313743"))
        assertEquals(VehicleModel.P508, model("VF38DAHRMFL036865"))
    }

    @Test
    fun `decodes brand, name and model year`() {
        val info = VinDecoder.decode(" vf3cuhnssjy113725 ")
        assertEquals("Peugeot", info.brand)
        assertEquals("Peugeot 2008", info.name)
        assertEquals(2018, info.modelYear)
    }

    @Test
    fun `other brands and unknown families have no model`() {
        val citroen = VinDecoder.decode("VF7SXHMZ6HT123456")
        assertEquals("Citroën", citroen.brand)
        assertNull(citroen.model)
        assertEquals("Citroën", citroen.name)
        assertNull(model("VF3DDNFP0DJ123456"))
    }

    @Test
    fun `invalid vin decodes to nothing`() {
        val info = VinDecoder.decode("VF3CU")
        assertNull(info.name)
        assertNull(info.modelYear)
    }

    @Test
    fun `model year codes skip I O Q U Z`() {
        assertEquals(2010, VinDecoder.modelYear('A'))
        assertEquals(2016, VinDecoder.modelYear('G'))
        assertEquals(2018, VinDecoder.modelYear('J'))
        assertEquals(2022, VinDecoder.modelYear('N'))
        assertEquals(2023, VinDecoder.modelYear('P'))
        assertEquals(2009, VinDecoder.modelYear('9'))
        assertNull(VinDecoder.modelYear('I'))
    }
}
