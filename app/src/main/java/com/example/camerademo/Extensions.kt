@file:JvmName("ExtensionUtils")

package com.example.camerademo

fun Long?.millisToString(): String {
    if (this == null) return ""
    var rest = this
    val hours = (rest / (60 * 60 * 1000)).toString().padStart(2, '0')
    rest %= 60 * 60 * 1000
    val minutes = (rest / 60000).toString().padStart(2, '0')
    rest %= 60000
    val seconds = (rest / 1000).toString().padStart(2, '0')
    rest %= 1000
    val restStr = rest.toString().padStart(3, '0')
    return "$hours:$minutes:$seconds.$restStr"
}