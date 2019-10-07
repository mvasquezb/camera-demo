package com.example.camerademo.lrc

interface ILrcPlayer {
    fun timePassed(): Long
    fun newLine(index: Int)
}