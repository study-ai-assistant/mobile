package ru.studyassistant.mobile

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import ru.studyassistant.mobile.api.ApiException

class ModesTest {
    @Test fun mapsEveryContractIdToRussianTitleAndDescription() {
        val titles = mapOf("SHORT_SUMMARY" to "Краткий конспект",
            "DETAILED_SUMMARY" to "Подробный конспект",
            "SIMPLE_EXPLANATION" to "Объяснение простыми словами",
            "KEY_TERMS" to "Ключевые термины",
            "SELF_CHECK_QUESTIONS" to "Вопросы для самопроверки")
        for ((id, title) in titles) {
            val mode = ProcessingMode.fromId(id)!!
            assertEquals(title, mode.title)
            assertTrue(mode.description.isNotBlank())
        }
        assertNull(ProcessingMode.fromId("FUTURE_MODE"))
    }

    @Test fun sendsEverySelectedModeAndKeepsResultMode() = runBlocking {
        val sent = mutableListOf<ProcessingMode>()
        val flow = StudyFlow(this, { _, mode -> sent += mode; "Ответ ${mode.name}" }, { "ok" },
            loadModes = { ProcessingMode.entries })
        flow.editMaterial("Материал")
        flow.showModes()
        flow.submit() // Загрузка списка ещё не завершена: POST запрещён.
        assertTrue(sent.isEmpty())
        yield()
        assertEquals(5, flow.state.value.modes.size)
        for (mode in ProcessingMode.entries) {
            flow.selectMode(mode)
            flow.submit()
            yield()
            assertEquals(Screen.RESULT, flow.state.value.screen)
            assertEquals(mode, flow.state.value.resultMode)
            assertEquals("Ответ ${mode.name}", flow.state.value.result)
            flow.back()
        }
        assertEquals(ProcessingMode.entries, sent)
        flow.newMaterial()
        assertEquals("", flow.state.value.material)
        assertEquals(Screen.MATERIAL, flow.state.value.screen)
    }

    @Test fun listFailureCanBeRetriedAndLateResponseIgnored() = runBlocking {
        var count = 0
        val pending = CompletableDeferred<List<ProcessingMode>>()
        val flow = StudyFlow(this, { _, _ -> "Ответ" }, { "ok" }, loadModes = {
            if (++count == 1) throw ApiException("Нет связи")
            withContext(NonCancellable) { pending.await() }
        })
        flow.editMaterial("Материал")
        flow.showModes(); yield()
        assertFalse(flow.state.value.modesLoaded)
        assertTrue(flow.state.value.modesError!!.contains("Нет связи"))
        flow.refreshModes(); yield()
        flow.cancel(); flow.back()
        pending.complete(ProcessingMode.entries); yield()
        assertEquals(Screen.MATERIAL, flow.state.value.screen)
        assertFalse(flow.state.value.modesLoaded)
    }
    @Test fun retriesSelectedServerModeWithoutDuplicatePost() = runBlocking {
        val requests = mutableListOf<Pair<String, ProcessingMode>>()
        val flow = StudyFlow(this, { text, mode ->
            requests += text to mode
            if (requests.size == 1) throw ApiException("Нет связи")
            "Термины"
        }, { "ok" }, loadModes = { ProcessingMode.entries })
        flow.editMaterial("Учебный материал")
        flow.showModes(); yield()
        flow.selectMode(ProcessingMode.KEY_TERMS)
        flow.submit(); yield()
        assertTrue(flow.state.value.canRetry)
        flow.retry(); flow.retry(); yield()
        assertEquals(2, requests.size)
        assertEquals(requests[0], requests[1])
        assertEquals(ProcessingMode.KEY_TERMS, flow.state.value.resultMode)
        assertEquals(Screen.RESULT, flow.state.value.screen)
        assertFalse(flow.state.value.canRetry)
    }
}
