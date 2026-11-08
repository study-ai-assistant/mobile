package ru.studyassistant.mobile

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class ServerSettingsTest {
    @Test fun anotherSettingsInstanceReadsSavedAddress() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settings = ServerSettings(context)
        val old = settings.address
        try {
            settings.save("http://192.168.1.10:8080/")
            assertEquals("http://192.168.1.10:8080", ServerSettings(context).address)
        } finally { settings.save(old) }
    }
}
