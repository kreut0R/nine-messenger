package app.nine.support

import app.nine.data.MessengerStore
import app.nine.domain.MessengerData
import app.nine.domain.TimeSource

/** Time controlled by the test. */
class FakeTime(var now: Long = 0L) : TimeSource {
    override fun nowMillis(): Long = now
}

/** In-memory store. [saves] lets a test check that changes are actually written. */
class InMemoryStore(var stored: MessengerData = MessengerData()) : MessengerStore {
    var saves = 0
        private set

    override fun load(): MessengerData = stored

    override fun save(data: MessengerData) {
        stored = data
        saves++
    }
}

/** Generator of predictable ids: id-1, id-2 and so on. */
fun sequentialIds(): () -> String {
    var next = 0
    return { "id-${++next}" }
}
