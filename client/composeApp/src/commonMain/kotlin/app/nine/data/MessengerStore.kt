package app.nine.data

import app.nine.domain.MessengerData

/** Где лежат данные приложения. Сейчас это файл на диске, позже сюда можно добавить базу данных. */
interface MessengerStore {
    /** Читает сохранённые данные. Если их нет или файл повреждён, возвращает пустое состояние. */
    fun load(): MessengerData

    fun save(data: MessengerData)
}
