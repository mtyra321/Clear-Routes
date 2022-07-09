package edu.byui.mynavigation

import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

class DailyForecast {
    var min by Delegates.notNull<Double>()
    var max by Delegates.notNull<Double>()
    lateinit var degrees: String
    lateinit var day: String
    var dateTime by Delegates.notNull<Long>()
    lateinit var iconId: String
    var icon by Delegates.notNull<Int>()
    lateinit var weatherDesc: String

    override fun toString(): String {
        day = getDayOfWeek(dateTime)
        //  convertToCelsius()
      //  Log.i("e", "icon is: $icon")
        return "min: ${min.toInt()}\u00B0 max: ${max.toInt()}\u00B0"
    }

    fun getDayOfWeek(timestamp: Long): String {
        return SimpleDateFormat("EEEE", Locale.ENGLISH).format(timestamp * 1000)
    }

    fun convertToCelsius() {
        max = (max - 32) * 5 / 9
        min = (min - 32) * 5 / 9
        degrees = "C"

    }

    fun convertToFahrenheit() {
        max = (max * 9 / 5) + 32
        min = (min * 9 / 5) + 32
        degrees = "F"
    }
}


