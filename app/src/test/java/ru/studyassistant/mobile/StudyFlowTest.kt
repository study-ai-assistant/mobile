package ru.studyassistant.mobile

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test
import ru.studyassistant.mobile.api.ApiException

class StudyFlowTest {
    @Test fun invalidMaterialStaysOnInputAndDoesNotSend() = runBlocking {
        var calls = 0
        val flow = StudyFlow(this, { _, _ -> calls++; "Ответ" }, { "ok" })
        flow.editMaterial(" \n ")
        flow.showModes()
        flow.submit()
        yield()
        assertEquals(Screen.MATERIAL, flow.state.value.screen)
        assertNotNull(flow.state.value.error)
        assertEquals(0, calls)
        flow.editMaterial("😀".repeat(10_001))
        flow.showModes()
        assertEquals(Screen.MATERIAL, flow.state.value.screen)
        assertNotNull(flow.state.value.error)
    }

    @Test fun resultAndBackPreserveMaterialWithoutResending() = runBlocking {
        var calls = 0
        val flow = StudyFlow(this, { text, mode ->
            calls++
            assertEquals("Материал", text)
            assertEquals(ProcessingMode.SHORT_SUMMARY, mode)
            "Конспект"
        }, { "ok" })
        flow.editMaterial("Материал")
        flow.showModes()
        flow.selectMode(ProcessingMode.KEY_TERMS)
        assertEquals(ProcessingMode.SHORT_SUMMARY, flow.state.value.mode)
        flow.submit()
        flow.submit()
        yield()
        assertEquals(Screen.RESULT, flow.state.value.screen)
        assertEquals("Конспект", flow.state.value.result)
        flow.back()
        assertEquals(Screen.MODES, flow.state.value.screen)
        flow.back()
        assertEquals("Материал", flow.state.value.material)
        assertEquals(Screen.MATERIAL, flow.state.value.screen)
        assertEquals(1, calls)
    }

    @Test fun failureKeepsInputAndRetriesOnlyOnRequest() = runBlocking {
        var calls = 0
        val flow = StudyFlow(this, { _, _ ->
            if (++calls == 1) throw ApiException("Сервер недоступен")
            "Результат"
        }, { "ok" })
        flow.editMaterial("Материал")
        flow.showModes()
        flow.submit()
        yield()
        assertEquals(Screen.MODES, flow.state.value.screen)
        assertEquals("Сервер недоступен", flow.state.value.error)
        assertFalse(flow.state.value.busy)
        assertEquals(1, calls)
        flow.submit()
        yield()
        assertEquals(Screen.RESULT, flow.state.value.screen)
        assertEquals(2, calls)
    }

    @Test fun cancelledResponseCannotOverwriteNextRequest() = runBlocking {
        val oldResponse = CompletableDeferred<String>()
        val newResponse = CompletableDeferred<String>()
        var calls = 0
        val flow = StudyFlow(this, { _, _ ->
            if (++calls == 1) withContext(NonCancellable) { oldResponse.await() }
            else newResponse.await()
        }, { "ok" })
        flow.editMaterial("Материал")
        flow.showModes()
        flow.submit()
        yield()
        flow.cancel()
        assertFalse(flow.state.value.busy)
        assertEquals("Материал", flow.state.value.material)
        flow.submit()
        yield()
        oldResponse.complete("Старый ответ")
        yield()
        assertTrue(flow.state.value.busy)
        assertEquals(Screen.MODES, flow.state.value.screen)
        newResponse.complete("Новый ответ")
        yield()
        assertEquals("Новый ответ", flow.state.value.result)
        assertEquals(Screen.RESULT, flow.state.value.screen)
    }

    @Test fun healthDoesNotAdvanceToResult() = runBlocking {
        val flow = StudyFlow(this, { _, _ -> error("Не должен вызываться") }, { "status = ok" })
        flow.checkHealth()
        yield()
        assertEquals(Screen.MATERIAL, flow.state.value.screen)
        assertEquals("status = ok", flow.state.value.notice)
        assertFalse(flow.state.value.busy)
    }
}
