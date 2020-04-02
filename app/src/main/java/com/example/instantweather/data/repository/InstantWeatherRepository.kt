package com.example.instantweather.data.repository

import android.app.Application
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.example.instantweather.BuildConfig
import com.example.instantweather.data.local.WeatherDatabase
import com.example.instantweather.data.model.*
import com.example.instantweather.data.remote.WeatherApi
import com.example.instantweather.mapper.WeatherForecastMapperLocal
import com.example.instantweather.mapper.WeatherMapperLocal
import com.example.instantweather.ui.BaseViewModel
import com.example.instantweather.utils.SharedPreferenceHelper
import com.example.instantweather.mapper.toDatabaseModel
import com.example.instantweather.utils.convertKelvinToCelsius
import com.example.instantweather.utils.round
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Created by Mayokun Adeniyi on 27/02/2020.
 */

class InstantWeatherRepository(
    private val database: WeatherDatabase,
    application: Application
) : BaseViewModel(application) {

    private val weatherMapperLocal = WeatherMapperLocal()
    private val weatherForecastMapperLocal = WeatherForecastMapperLocal()
    private var prefHelper = SharedPreferenceHelper.getInstance(application)
    private var refreshTime = 5 * 60 * 1000 * 1000 * 1000L
    private val API_KEY = BuildConfig.API_KEY
    private val dao = database.weatherDao

    //Weather[Domain Model] exposed to the ViewModel to be used in application
    var weather = MutableLiveData<Weather>()
    val weatherDataFetchState = MutableLiveData<Boolean>()
    val weatherIsLoading = MutableLiveData<Boolean>()

    //WeatherForecast[Domain Model] exposed to the ViewModel to be used in application
    var weatherForecast = MutableLiveData<List<WeatherForecast>>()
    val weatherForecastDataFetchState = MutableLiveData<Boolean>()
    val weatherForecastIsLoading = MutableLiveData<Boolean>()

    val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)

    var latitude = 0.00
    var longitude = 0.00


    fun refreshWeatherData() {
        weatherIsLoading.value = true
        checkCacheDuration()
        val updateTime = prefHelper.getUpdateTime()
        if (updateTime != null && updateTime != 0L && System.nanoTime() - updateTime < refreshTime) {
            getLocalWeatherData()
        } else {
            getRemoteWeatherData()
        }
    }

    fun refreshWeatherForecastData() {
        weatherForecastIsLoading.value = true
        checkCacheDuration()
        val updateTime = prefHelper.getUpdateTime()
        if (updateTime != null && updateTime != 0L && System.nanoTime() - updateTime < refreshTime) {
            getLocalWeatherForecastData()
        } else {
            getRemoteWeatherForecast()
        }
    }

    private fun checkCacheDuration() {
        val cachePreference = prefHelper.getCacheDuration()
        try {
            val cacheDurationInt = cachePreference?.toInt() ?: 5 * 60
            refreshTime = cacheDurationInt.times(1000 * 1000 * 1000L)
        } catch (e: NumberFormatException) {
            Timber.i(e)
        }

    }

    fun getRemoteWeatherData() {
        Timber.i("Getting data from remote yooo!")
        val locationRequest = LocationRequest()
            .setInterval(2000)
            .setFastestInterval(2000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    for (location in locationResult.locations) {
                        latitude = location.latitude
                        longitude = location.longitude
                        Timber.i("This is another ${location.longitude} and ${location.latitude}")
                    }
                    // Few more things we can do here:
                    // For example: Update the location of user on server
                }
            },
            Looper.myLooper())

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            Timber.i("Please work ${location.longitude} and ${location.latitude}")
            Timber.i("Lat is ${latitude} and Long is ${longitude}")
            launch {
                Timber.i("Getting weather response........")
                try {
                    val networkWeather =
                        WeatherApi.retrofitService.getCurrentWeather(latitude, longitude, API_KEY)
                    //Save city ID to shared preferences
                    prefHelper.saveCityId(networkWeather.cityId)
                    storeRemoteWeatherDataLocally(networkWeather)

                    Timber.i("WEATHER RESPONSE HAS BEEN RECEIVED......")

                } catch (e: Exception) {
                    weatherIsLoading.postValue(false)
                    weatherDataFetchState.postValue(false)
                    Timber.i("AN ERROR OCCURRED ${e.message}")
                }
            }
        }
    }

    private fun getLocalWeatherData() {
        Timber.i("Getting data from local broo!")
        launch {
            withContext(Dispatchers.IO) {
                //Get the weather from the database
                val dbWeather = dao.getWeather()

                //Set the value for the MutableLiveData of type Weather
                weather.postValue(weatherMapperLocal.transformToDomain(dbWeather))
                weatherIsLoading.postValue(false)
                weatherDataFetchState.postValue(true)
            }
        }
    }

    private fun storeRemoteWeatherDataLocally(networkWeather: NetworkWeather) {
        launch {
            withContext(Dispatchers.IO) {
                //Empty the db
                dao.deleteAllWeather()

                //Get the temperature in kelvin and convert to celsius
                val kelvinValue = networkWeather.networkWeatherCondition.temp
                networkWeather.networkWeatherCondition.temp = convertKelvinToCelsius(kelvinValue)

                //Convert the network weather response to a database model
                val weatherResponse = networkWeather.toDatabaseModel()

                //Insert the weather of database model into the db
                dao.insertWeather(weatherResponse)

                //Get the weather from the database
                val dbWeather = dao.getWeather()

                Timber.i("The weather's city name is ${dbWeather.cityName}")

                //Set the value for the MutableLiveData of type Weather
                weather.postValue(weatherMapperLocal.transformToDomain(dbWeather))
                weatherIsLoading.postValue(false)
                weatherDataFetchState.postValue(true)
            }
        }
        prefHelper.saveUpdateTime(System.nanoTime())
    }

    fun getRemoteWeatherForecast() {
        launch {
            Timber.i("Getting weather forecast....")
            val cityId = prefHelper.getCityId()
            if (cityId != null)
                try {
                    val networkWeatherForecastResponse =
                        WeatherApi.retrofitService.getWeatherForecast(cityId, API_KEY)
                    val listOfNetworkWeatherForecast = networkWeatherForecastResponse.weathers
                    Timber.i("WEATHER FORECAST HAS BEEN RECEIVED......")
                    storeRemoteForecastDataLocally(listOfNetworkWeatherForecast)
                } catch (e: Exception) {
                    weatherForecastIsLoading.postValue(false)
                    weatherForecastDataFetchState.postValue(false)
                    Timber.i("AN ERROR OCCURRED ${e.message}")
                }
        }
    }

    private fun getLocalWeatherForecastData() {
        launch {
            withContext(Dispatchers.IO) {
                //Get weather forecast from db
                val weatherForecastDb = dao.getAllWeatherForecast()
                weatherForecast.postValue(
                    weatherForecastMapperLocal.transformToDomain(
                        weatherForecastDb
                    )
                )
                weatherForecastIsLoading.postValue(false)
                weatherForecastDataFetchState.postValue(true)
            }
        }
    }

    private fun storeRemoteForecastDataLocally(listOfNetworkWeatherForecast: List<NetworkWeatherForecast>) {
        launch {
            withContext(Dispatchers.IO) {
                //Empty the db
                dao.deleteAllWeatherForecast()
                //Insert each weather forecast into the db
                for (weatherForecast in listOfNetworkWeatherForecast) {
                    //Get the temperature in kelvin and convert to celsius
                    val kelvinValue = weatherForecast.networkWeatherCondition.temp
                    weatherForecast.networkWeatherCondition.temp =
                        convertKelvinToCelsius(kelvinValue)

                    dao.insertForecastWeather(weatherForecast.toDatabaseModel())
                }

                //Get a list of weather forecast from the db
                val dbForecast = dao.getAllWeatherForecast()

                //Set the value for the weather forecast of type mutable live data
                weatherForecast.postValue(weatherForecastMapperLocal.transformToDomain(dbForecast))
                weatherForecastIsLoading.postValue(false)
                weatherForecastDataFetchState.postValue(true)
            }
        }
    }

}
