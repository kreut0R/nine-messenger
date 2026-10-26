package app.nine.support

import app.nine.data.MessengerStore
import app.nine.domain.MessengerData
import app.nine.domain.TimeSource

/** Время, которым управляет тест. */
class FakeTime(var now: Long = 0L) : TimeSource {
    override fun nowMillis(): Long = now
}

/** Хранилище в памяти. [saves] позволяет проверить, что изменения действительно записываются. */
class InMemoryStore(var stored: MessengerData = MessengerData()) : MessengerStore {
    var saves = 0
        private set

    override fun load(): MessengerData = stored

    override fun save(data: MessengerData) {
        stored = data
        saves++
    }
}

/** Генератор предсказуемых id: id-1, id-2 и так далее. */
fun sequentialIds(): () -> String {
    var next = 0
    return { "id-${++next}" }
}
