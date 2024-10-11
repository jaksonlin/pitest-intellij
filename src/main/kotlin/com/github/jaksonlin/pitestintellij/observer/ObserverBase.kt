package com.github.jaksonlin.pitestintellij.observer

open class ObserverBase {
    private val observers = mutableListOf<RunHistoryObserver>()

    fun addObserver(observer: RunHistoryObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: RunHistoryObserver) {
        observers.remove(observer)
    }

    protected fun notifyObservers(eventObj: Any?) {
        observers.forEach { it.onRunHistoryChanged(eventObj) }
    }
}