package ru.studyassistant.mobile.api

object ServerConfig {
    // Android Emulator → компьютер. Для телефона: IP компьютера в общей Wi-Fi сети.
    const val BASE_URL = "http://10.0.2.2:8080"
    const val CONNECT_TIMEOUT_MS = 10_000
    const val PROCESS_TIMEOUT_MS = 90_000
}
