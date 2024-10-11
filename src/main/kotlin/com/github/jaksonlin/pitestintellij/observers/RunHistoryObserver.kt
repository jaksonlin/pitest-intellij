package com.github.jaksonlin.pitestintellij.observers

interface RunHistoryObserver {
    fun onRunHistoryChanged(eventObj:Any?)
}