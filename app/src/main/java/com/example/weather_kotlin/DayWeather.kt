package com.example.weather_kotlin

data class DayWeather(
    var date: String,
    var img: String,
    var maxTemp: String,
    val minTemp:String,
    val humidity:String,
    val rainVolume:String,
    val windSpeed:String,
    val pressure:String,
    val sunset:Long,
    val sunrise:Long,
    val description:String
)
