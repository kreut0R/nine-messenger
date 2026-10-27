package app.nine.data

import app.nine.domain.MessengerData

/** Where the app data lives. For now it is a file on disk; a database can be added here later. */
interface MessengerStore {
    /** Reads the saved data. If there is none or the file is damaged, returns an empty state. */
    fun load(): MessengerData

    fun save(data: MessengerData)
}
