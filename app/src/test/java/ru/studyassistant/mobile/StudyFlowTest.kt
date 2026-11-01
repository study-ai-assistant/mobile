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
        val requests = mutableListOf<Pair<String, ProcessingMode>>()
        val flow = StudyFlow(this, { text, mode ->
            requests += text to mode
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
        assertTrue(flow.state.value.canRetry)
        assertEquals(1, calls)
        flow.retry()
        flow.retry()
        yield()
        assertEquals(Screen.RESULT, flow.state.value.screen)
        assertEquals(2, calls)
        assertEquals(requests[0], requests[1])
        assertFalse(flow.state.value.canRetry)
    }

    @Test fun loadingBlocksDuplicateRequestsAndEditing() = runBlocking {
        val response = CompletableDeferred<String>()
        var calls = 0
        val flow = StudyFlow(this, { _, _ -> calls++; response.await() }, { "ok" })
        flow.editMaterial("Исходный материал")
        flow.showModes()
        flow.submit()
        yield()
        assertTrue(flow.state.value.busy)
        flow.editMaterial("Другой материал")
        flow.submit()
        flow.checkHealth()
        flow.retry()
        yield()
        assertEquals(1, calls)
        assertEquals("Исходный материал", flow.state.value.material)
        response.complete("Ответ")
        yield()
        assertFalse(flow.state.value.busy)
    }

    @Test fun editingAfterErrorDiscardsRetryAndNewSubmitUsesNewMaterial() = runBlocking {
        val requests = mutableListOf<String>()
        val flow = StudyFlow(this, { text, _ ->
            requests += text
            throw ApiException("Нет связи")
        }, { "ok" })
        flow.editMaterial("Первый текст")
        flow.showModes()
        flow.submit()
        yield()
        flow.back()
        flow.editMaterial("Новый текст")
        flow.retry()
        yield()
        assertEquals(listOf("Первый текст"), requests)
        flow.showModes()
        flow.submit()
        yield()
        assertEquals(listOf("Первый текст", "Новый текст"), requests)
    }

    @Test fun retryOfHealthDoesNotSendText() = runBlocking {
        var calls = 0
        val flow = StudyFlow(this, { _, _ -> error("POST не должен вызываться") }, {
            if (++calls == 1) throw ApiException("Нет связи") else "ok"
        })
        flow.checkHealth()
        yield()
        assertTrue(flow.state.value.canRetry)
        flow.retry()
        yield()
        assertEquals(2, calls)
        assertEquals(Screen.MATERIAL, flow.state.value.screen)
        assertEquals("ok", flow.state.value.notice)
        assertFalse(flow.state.value.canRetry)
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
