package com.example.instantweather.data.model

/**
 * Created by Mayokun Adeniyi on 27/02/2020.
 */
data class Weather(

    val uId: Long,
    val cityId: Long,
    val name: String,
    val networkWeatherDescription: List<NetworkWeatherDescription>,
    val networkWeatherCondition: NetworkWeatherCondition
)