package com.example.instantweather.utils

import androidx.room.TypeConverter
import com.example.instantweather.data.model.Wind
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Created by Mayokun Adeniyi on 16/03/2020.
 */

class WindConverter {
    val gson = Gson()

    val type: Type = object : TypeToken<Wind?>() {}.type

    @TypeConverter
    fun fromWind(wind: Wind?): String{
        return gson.toJson(wind,type)
    }

    @TypeConverter
    fun toWind(json: String?): Wind{
        return gson.fromJson(json,type)
    }
}