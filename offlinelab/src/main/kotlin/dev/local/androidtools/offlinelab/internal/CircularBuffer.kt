package dev.local.androidtools.offlinelab.internal

/** Bounded, thread-safe FIFO buffer — evicts the oldest entry once [capacity] is reached. */
internal class CircularBuffer<T>(private val capacity: Int) {
    private val items = ArrayDeque<T>(capacity)

    @Synchronized
    fun add(item: T) {
        if (items.size >= capacity) items.removeFirst()
        items.addLast(item)
    }

    @Synchronized
    fun snapshot(): List<T> = items.toList()

    @Synchronized
    fun clear() = items.clear()

    @Synchronized
    fun size(): Int = items.size
}
