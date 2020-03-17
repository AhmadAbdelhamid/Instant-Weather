package com.example.instantweather.data.model

/**
 * Created by Mayokun Adeniyi on 27/02/2020.
 */

//This represents the Domain weather model
data class Weather(

    val uId: Int,
    val cityId: Int,
    val name: String,
    val wind: Wind,
    val networkWeatherDescription: List<NetworkWeatherDescription>,
    val networkWeatherCondition: NetworkWeatherCondition
)