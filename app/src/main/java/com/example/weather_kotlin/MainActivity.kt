package com.example.weather_kotlin

import android.annotation.SuppressLint
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weather_kotlin.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.log

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val CITY: String = "London,gb"
    val API: String = "982cfb3f7fe00de65d41907bfbe382f9"

    private lateinit var adapter: Adapter
    private var list: MutableList<HourWeather> = mutableListOf()
    private var list1: MutableList<HourWeather> = mutableListOf()

    private lateinit var adapterDay: AdapterDay
    private var listDay: MutableList<DayWeather> = mutableListOf()

    private lateinit var address: String

    override fun onCreate(savedInstanceState: Bundle?) {
        // Check the current hour before calling super.onCreate()
        val currentHour = getCurrentHour().toInt()

        if (currentHour in 6..17) {
            setTheme(R.style.Base_Theme_Weather_kotlin_Light)
        } else {
            setTheme(R.style.Base_Theme_Weather_kotlin_Dark)
        }

        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (currentHour in 6..17) {
            binding.main.setBackgroundResource(R.drawable.linear_bg)
        } else {
            binding.main.setBackgroundResource(R.drawable.linear_bg_night)
        }


        weatherTask().execute()
        WeatherTaskHour().execute()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                setUpRecycleView()
            }
        }

    }


    private fun getCurrentHour(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("HH", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMM, d", Locale.ENGLISH)
        return dateFormat.format(calendar.time)
    }


    private fun setUpRecycleView() {
        adapter = Adapter(list)
        binding.recyleviewDayWeather.adapter = adapter
        binding.recyleviewDayWeather.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        adapterDay = AdapterDay(listDay)
        binding.recycleviewDay.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.recycleviewDay.adapter = adapterDay

        try {
            adapterDay.onClickItem = { dayWeather, position ->
                val intent = Intent(this, Detail::class.java).apply {
                    // Define the date formats
                    val sourceDateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)
                    val targetDateFormat = SimpleDateFormat("MMM, d", Locale.ENGLISH)

                    // Filter the list based on date comparison
                    val selectedDayWeatherList = list1.filter {
                        try {
                            val parsedDate = sourceDateFormat.parse(it.date)
                            val formattedDate = targetDateFormat.format(parsedDate)
                            Log.d("Formatted it.date", formattedDate)
                            Log.d("dayWeather.date", dayWeather.date)
                            formattedDate == dayWeather.date
                        } catch (e: Exception) {
                            Log.e("DateParsingError", "Error parsing date: ${it.date}", e)
                            false
                        }
                    }

                    // Log the size of selectedDayWeatherList for debugging
                    Log.d("SelectedDayWeatherListSize", "Size: ${selectedDayWeatherList.size}")

                    // Pass the filtered list
                    intent.putParcelableArrayListExtra("HOUR_WEATHER_LIST", ArrayList(selectedDayWeatherList))
                    putExtra("ADDRESS", address)
                    putExtra("DATE", dayWeather.date)
                    putExtra("WEATHER_ICON", dayWeather.img)
                    putExtra("MAX_TEMP", dayWeather.maxTemp)
                    putExtra("MIN_TEMP", dayWeather.minTemp)
                    putExtra("HUMIDITY", dayWeather.humidity)
                    putExtra("RAIN_VOLUME", dayWeather.rainVolume)
                    putExtra("WIND_SPEED", dayWeather.windSpeed)
                    putExtra("PRESSURE", dayWeather.pressure)
                    putExtra("SUNSET", dayWeather.sunset)
                    putExtra("SUNRISE", dayWeather.sunrise)
                    putExtra("DESCRIPTION", dayWeather.description)
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.d("checkList1", e.toString())
        }

    }

    inner class weatherTask() : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            binding.imgNetwork.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            binding.tvError.visibility = View.GONE
            binding.mainContainer.visibility = View.GONE
        }

        override fun doInBackground(vararg params: String?): String? {
            var reponse: String?

            try {
                reponse =
                    URL("https://api.openweathermap.org/data/2.5/weather?q=$CITY&units=metric&appid=$API").readText(
                        Charsets.UTF_8
                    )
            } catch (e: Exception) {
                reponse = null
            }
            return reponse
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result != null) {
                try {
                    val jsonObj = JSONObject(result)
                    val main = jsonObj.getJSONObject("main")
                    val sys = jsonObj.getJSONObject("sys")
                    val wind = jsonObj.getJSONObject("wind")
                    val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
                    address = jsonObj.getString("name") + ", " + sys.getString("country")
                    val updateAt: Long = jsonObj.getLong("dt")

                    val updateAtText =
                        SimpleDateFormat(
                            "EEE, MMM dd",
                            Locale.ENGLISH
                        ).format(Date(updateAt * 1000))

                    val temp = main.getString("temp")


                    val tempMin = "Min.: " + main.getString("temp_min") + "°C"
                    val tempMax = "Max.: " + main.getString("temp_max") + "°C"
                    val pressure = main.getString("pressure") + " hPa"
                    val humidity = main.getString("humidity") + " %"
                    val sunrise: Long = sys.getLong("sunrise")
                    val sunset: Long = sys.getLong("sunset")
                    val windSpeed = wind.getString("speed") + " km/h"
                    val weatherDescription = weather.getString(("description"))
                    val checkRain = weather.getString("main")

                    var rainVolume = "0 mm"
                    if (checkRain.equals("Rain")) {
                        if (jsonObj.has("rain")) {
                            val rain = jsonObj.getJSONObject("rain")
                            if (rain.has("1h")) {
                                rainVolume = rain.getString("1h") + " mm"
                                binding.tvRainVolume.text = rainVolume
                                Log.d("rain2", rainVolume)
                            }
                        }
                    } else {
                        binding.tvRainVolume.text = rainVolume
                        Log.d("rain2", "none")
                    }


                    val weatherId = weather.getString("id")
                    val tempDouble = temp.toDoubleOrNull()
                    val tempInt = tempDouble?.toInt()

                    binding.tvAddress.text = address

                    binding.tvUpdateAt.text = getCurrentDate()

                    val currentHour = getCurrentHour().toInt()

                    if (currentHour in 6..17) {
                        if (weatherId == "800") {
                            binding.img.setImageResource(R.drawable.sunny)
                        } else if (weatherId == "801") {
                            binding.img.setImageResource(R.drawable.pcloudy)
                        } else if (weatherId == "802") {
                            binding.img.setImageResource(R.drawable.pcloudy)
                        } else if (weatherId == "803") {
                            binding.img.setImageResource(R.drawable.mcloudy)
                        } else if (weatherId == "804") {
                            binding.img.setImageResource(R.drawable.mcloudy)
                        } else if (weatherId == "500") {
                            binding.img.setImageResource(R.drawable.lrain)
                        } else if (weatherId == "501") {
                            binding.img.setImageResource(R.drawable.lrain)
                        } else if (weatherId == "502") {
                            binding.img.setImageResource(R.drawable.lrain)
                        } else if (weatherId == "503") {
                            binding.img.setImageResource(R.drawable.rain)
                        } else if (weatherId == "504") {
                            binding.img.setImageResource(R.drawable.rain)
                        } else if (weatherId == "511") {
                            binding.img.setImageResource(R.drawable.rain)
                        } else if (weatherId == "600") {
                            binding.img.setImageResource(R.drawable.lsnow)
                        } else if (weatherId == "601") {
                            binding.img.setImageResource(R.drawable.lsnow)
                        } else if (weatherId == "602") {
                            binding.img.setImageResource(R.drawable.snow)
                        } else if (weatherId == "611") {
                            binding.img.setImageResource(R.drawable.sleet)
                        } else if (weatherId == "612") {
                            binding.img.setImageResource(R.drawable.sleet)
                        } else if (weatherId == "613") {
                            binding.img.setImageResource(R.drawable.sleet)
                        } else if (weatherId == "614") {
                            binding.img.setImageResource(R.drawable.sleet)
                        } else if (weatherId == "200") {
                            binding.img.setImageResource(R.drawable.tshower)
                        } else if (weatherId == "201") {
                            binding.img.setImageResource(R.drawable.tshower)
                        } else if (weatherId == "202") {
                            binding.img.setImageResource(R.drawable.tstorm)
                        } else if (weatherId == "210") {
                            binding.img.setImageResource(R.drawable.sleet)
                        } else if (weatherId == "211") {
                            binding.img.setImageResource(R.drawable.sleet)
                        } else if (weatherId == "212") {
                            binding.img.setImageResource(R.drawable.sleet)
                        } else if (weatherId == "701") {
                            binding.img.setImageResource(R.drawable.foggy)
                        } else if (weatherId == "702") {
                            binding.img.setImageResource(R.drawable.foggy)
                        } else if (weatherId == "703") {
                            binding.img.setImageResource(R.drawable.foggy)
                        } else if (weatherId == "704") {
                            binding.img.setImageResource(R.drawable.foggy)
                        } else if (weatherId == "771") {
                            binding.img.setImageResource(R.drawable.foggy)
                        } else if (weatherId == "762") {
                            binding.img.setImageResource(R.drawable.foggy)
                        } else if (weatherId == "763") {
                            binding.img.setImageResource(R.drawable.foggy)
                        } else if (weatherId == "751") {
                            binding.img.setImageResource(R.drawable.snow)
                        } else if (weatherId == "761") {
                            binding.img.setImageResource(R.drawable.snow)
                        } else if (weatherId == "752") {
                            binding.img.setImageResource(R.drawable.snow)
                        } else {
                            binding.img.setImageResource(R.drawable.snow)
                        }
                    } else {
                        if (weatherId == "800") {
                            binding.img.setImageResource(R.drawable.moon)
                        } else if (weatherId == "801") {
                            binding.img.setImageResource(R.drawable.pcloudy_night)
                        } else if (weatherId == "802") {
                            binding.img.setImageResource(R.drawable.pcloudy_night)
                        } else if (weatherId == "803") {
                            binding.img.setImageResource(R.drawable.mcloudy_night)
                        } else if (weatherId == "804") {
                            binding.img.setImageResource(R.drawable.mcloudy_night)
                        } else if (weatherId == "500") {
                            binding.img.setImageResource(R.drawable.lrain_night)
                        } else if (weatherId == "501") {
                            binding.img.setImageResource(R.drawable.lrain_night)
                        } else if (weatherId == "502") {
                            binding.img.setImageResource(R.drawable.lrain_night)
                        } else if (weatherId == "503") {
                            binding.img.setImageResource(R.drawable.rain_night)
                        } else if (weatherId == "504") {
                            binding.img.setImageResource(R.drawable.rain_night)
                        } else if (weatherId == "511") {
                            binding.img.setImageResource(R.drawable.rain_night)
                        } else if (weatherId == "600") {
                            binding.img.setImageResource(R.drawable.lsnow_night)
                        } else if (weatherId == "601") {
                            binding.img.setImageResource(R.drawable.lsnow_night)
                        } else if (weatherId == "602") {
                            binding.img.setImageResource(R.drawable.snow_night)
                        } else if (weatherId == "611") {
                            binding.img.setImageResource(R.drawable.sleet_night)
                        } else if (weatherId == "612") {
                            binding.img.setImageResource(R.drawable.sleet_night)
                        } else if (weatherId == "613") {
                            binding.img.setImageResource(R.drawable.sleet_night)
                        } else if (weatherId == "614") {
                            binding.img.setImageResource(R.drawable.sleet_night)
                        } else if (weatherId == "200") {
                            binding.img.setImageResource(R.drawable.tshower_night)
                        } else if (weatherId == "201") {
                            binding.img.setImageResource(R.drawable.tshower_night)
                        } else if (weatherId == "202") {
                            binding.img.setImageResource(R.drawable.tstorm_night)
                        } else if (weatherId == "210") {
                            binding.img.setImageResource(R.drawable.sleet_night)
                        } else if (weatherId == "211") {
                            binding.img.setImageResource(R.drawable.sleet_night)
                        } else if (weatherId == "212") {
                            binding.img.setImageResource(R.drawable.sleet_night)
                        } else if (weatherId == "701") {
                            binding.img.setImageResource(R.drawable.foggy_night)
                        } else if (weatherId == "702") {
                            binding.img.setImageResource(R.drawable.foggy_night)
                        } else if (weatherId == "703") {
                            binding.img.setImageResource(R.drawable.foggy_night)
                        } else if (weatherId == "704") {
                            binding.img.setImageResource(R.drawable.foggy_night)
                        } else if (weatherId == "771") {
                            binding.img.setImageResource(R.drawable.foggy_night)
                        } else if (weatherId == "762") {
                            binding.img.setImageResource(R.drawable.foggy_night)
                        } else if (weatherId == "763") {
                            binding.img.setImageResource(R.drawable.foggy_night)
                        } else if (weatherId == "751") {
                            binding.img.setImageResource(R.drawable.snow_night)
                        } else if (weatherId == "761") {
                            binding.img.setImageResource(R.drawable.snow_night)
                        } else if (weatherId == "752") {
                            binding.img.setImageResource(R.drawable.snow_night)
                        } else {
                            binding.img.setImageResource(R.drawable.snow_night)
                        }
                    }

                    binding.tvStatus.text = weatherDescription.capitalize()
                    binding.tvTemp.text = tempInt.toString()
                    binding.tvMinTemp.text = tempMin
                    binding.tvMaxTemp.text = tempMax
                    binding.tvSunrise.text =
                        SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunrise * 1000))
                    binding.tvSunset.text =
                        SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunset * 1000))
                    binding.tvWind.text = windSpeed
                    binding.tvPressure.text = pressure
                    binding.tvHumidity.text = humidity

                    binding.progressBar.visibility = View.GONE
                    binding.mainContainer.visibility = View.VISIBLE

                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.visibility = View.VISIBLE
                    binding.imgNetwork.visibility = View.VISIBLE
                    Log.d("check", e.toString())
                }
            } else {
                binding.progressBar.visibility = View.GONE
                binding.tvError.visibility = View.VISIBLE
                binding.imgNetwork.visibility = View.VISIBLE
                Log.d("check1", "e.toString()")
            }
        }
    }

    inner class WeatherTaskHour : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            var response: String? = null
            try {
                val url =
                    URL("https://api.openweathermap.org/data/2.5/forecast?q=$CITY&units=metric&appid=$API")

                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"
                    inputStream.bufferedReader().use {
                        response = it.readText()
                    }
                }
            } catch (e: Exception) {
                Log.e("WeatherTask", "Error fetching data: ${e.message}")
            }
            return response
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result != null) {
                try {
                    val jsonObj = JSONObject(result)
                    val listJson = jsonObj.getJSONArray("list")
                    val weatherList1 = mutableListOf<HourWeather>()
                    val weatherList = mutableListOf<HourWeather>()
                    val weatherDayList = mutableListOf<DayWeather>()

                    // Define the time range for filtering
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val dateFormat12 = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val calendar = Calendar.getInstance()

                    // Start of the time range: 15:00 on the first day
                    calendar.set(Calendar.HOUR_OF_DAY, 15)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    val startTime = dateFormat.format(calendar.time)

                    // End of the time range: 12:00 on the following day
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 12)
                    val endTime = dateFormat.format(calendar.time)

                    val defaultTime12 = "12:00:00"

                    val city = jsonObj.getJSONObject("city")
                    val sunriseDay: Long = city.getLong("sunrise")
                    val sunsetDay: Long = city.getLong("sunset")

//                    var sunsetDay = sunsetLong.toString()
//                    var sunsetDay = sunsetLong.toString()
                    for (i in 0 until listJson.length()) {
                        try {
                            val item = listJson.getJSONObject(i)
                            val main = item.getJSONObject("main")
                            val wind = item.getJSONObject("wind")
                            val weather = item.getJSONArray("weather").getJSONObject(0)
                            val checkRain = weather.getString("main")
                            var rainVolumeDay = "0"
                            if (checkRain.equals("Rain")) {
                                val rain = item.getJSONObject("rain")
                                rainVolumeDay = rain.getString("3h")
                            } else {
                                rainVolumeDay = "0"
                            }

                            val temp = main.getString("temp")
                            val dtTxt = item.getString("dt_txt")
                            val maxTemp = main.getString("temp_max")
                            val minTemp = main.getString("temp_min")
                            val humidityDay = main.getString("humidity")
                            val windSpeedDay = wind.getString("speed")
                            val pressureDay = main.getString("pressure")
                            val descriptionDay = weather.getString("description")

                            val date1 = dateFormat.parse(dtTxt)
                            val timeFormat1 = SimpleDateFormat("ha", Locale.ENGLISH)
                            val timeString1 = timeFormat1.format(date1)
                                .toUpperCase(Locale.ENGLISH) // Time in "ha" format like "3PM"

                            // Determine the weather icon
                            val weatherId1 = weather.getString("id")
                            val weatherIcon1: String = when (weatherId1) {
                                "800" -> if (isDaytime(dtTxt)) "sunny" else "moon"
                                "801", "802" -> if (isDaytime(dtTxt)) "pcloudy" else "pcloudy_night"
                                "803", "804" -> if (isDaytime(dtTxt)) "mcloudy" else "mcloudy_night"
                                "500", "502" -> if (isDaytime(dtTxt)) "lrain" else "lrain_night"
                                "503", "504" -> if (isDaytime(dtTxt)) "rain" else "rain_night"
                                "511", "600", "601", "602" -> if (isDaytime(dtTxt)) "lsnow" else "lsnow_night"
                                "611", "612", "613", "614" -> if (isDaytime(dtTxt)) "sleet" else "sleet_night"
                                "200", "201" -> if (isDaytime(dtTxt)) "tshower" else "tshower_night"
                                "202", "210", "211", "212" -> if (isDaytime(dtTxt)) "tstorm" else "tstorm_night"
                                "701", "702", "703", "704", "771", "762", "763", "751", "761", "752" -> if (isDaytime(
                                        dtTxt
                                    )
                                ) "foggy" else "foggy_night"

                                else -> if (isDaytime(dtTxt)) "foggy" else "foggy_night"
                            }

                            val tempDouble = temp.toDoubleOrNull() ?: 0.0
                            val tempInt = tempDouble.toInt()

                            weatherList1.add(HourWeather("$date1","$tempInt", weatherIcon1, timeString1))
                            list1.clear()
                            list1.addAll(weatherList1)

                            if (dtTxt >= startTime && dtTxt <= endTime) {
                                val date = dateFormat.parse(dtTxt)
                                val timeFormat = SimpleDateFormat("ha", Locale.ENGLISH)
                                val timeString = timeFormat.format(date)
                                    .toUpperCase(Locale.ENGLISH) // Time in "ha" format like "3PM"

                                // Determine the weather icon
                                val weatherId = weather.getString("id")
                                val weatherIcon: String = when (weatherId) {
                                    "800" -> if (isDaytime(dtTxt)) "sunny" else "moon"
                                    "801", "802" -> if (isDaytime(dtTxt)) "pcloudy" else "pcloudy_night"
                                    "803", "804" -> if (isDaytime(dtTxt)) "mcloudy" else "mcloudy_night"
                                    "500", "502" -> if (isDaytime(dtTxt)) "lrain" else "lrain_night"
                                    "503", "504" -> if (isDaytime(dtTxt)) "rain" else "rain_night"
                                    "511", "600", "601", "602" -> if (isDaytime(dtTxt)) "lsnow" else "lsnow_night"
                                    "611", "612", "613", "614" -> if (isDaytime(dtTxt)) "sleet" else "sleet_night"
                                    "200", "201" -> if (isDaytime(dtTxt)) "tshower" else "tshower_night"
                                    "202", "210", "211", "212" -> if (isDaytime(dtTxt)) "tstorm" else "tstorm_night"
                                    "701", "702", "703", "704", "771", "762", "763", "751", "761", "752" -> if (isDaytime(
                                            dtTxt
                                        )
                                    ) "foggy" else "foggy_night"

                                    else -> if (isDaytime(dtTxt)) "foggy" else "foggy_night"
                                }

                                val tempDouble = temp.toDoubleOrNull() ?: 0.0
                                val tempInt = tempDouble.toInt()
                                weatherList.add(HourWeather("$date","$tempInt", weatherIcon, timeString))
                            }

                            val date = dateFormat.parse(dtTxt)
                            val dateTime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
                            val dateOf5Day = dateTime.format(date)

                            if (dateOf5Day == defaultTime12) {

                                val date1 = dateFormat.parse(dtTxt)
                                val dateTime1 = SimpleDateFormat("MMM, d", Locale.ENGLISH)
                                val dateOf5Day1 = dateTime1.format(date1)

                                val tempMinDouble = minTemp.toDoubleOrNull() ?: 0.0
                                val tempMinInt = tempMinDouble.toInt()
                                val tempMaxDouble = maxTemp.toDoubleOrNull() ?: 0.0
                                val tempMaxInt = tempMaxDouble.toInt()

                                val weatherId1 = weather.getString("id")
                                val weatherIcon5Day = when (weatherId1) {
                                    "800" -> "sunny"
                                    "801" -> "pcloudy"
                                    "802" -> "pcloudy"
                                    "803" -> "mcloudy"
                                    "804" -> "mcloudy"
                                    "500" -> "lrain"
                                    "502" -> "lrain"
                                    "503" -> "rain"
                                    "504" -> "rain"
                                    "511" -> "lsnow"
                                    "600" -> "lsnow"
                                    "601" -> "lsnow"
                                    "602" -> "snow"
                                    "611" -> "sleet"
                                    "612" -> "sleet"
                                    "613" -> "sleet"
                                    "614" -> "sleet"
                                    "200" -> "tshower"
                                    "201" -> "tshower"
                                    "202" -> "tstorm"
                                    "210" -> "sleet"
                                    "211" -> "sleet"
                                    "212" -> "sleet"
                                    "701" -> "foggy"
                                    "702" -> "foggy"
                                    "703" -> "foggy"
                                    "704" -> "foggy"
                                    "771" -> "foggy"
                                    "762" -> "foggy"
                                    "763" -> "foggy"
                                    "751" -> "foggy"
                                    "761" -> "foggy"
                                    "752" -> "foggy"
                                    else -> "foggy"
                                }

                                weatherDayList.add(
                                    DayWeather(
                                        dateOf5Day1,
                                        weatherIcon5Day,
                                        "$tempMaxInt",
                                        "$tempMinInt",
                                        humidityDay,
                                        rainVolumeDay,
                                        windSpeedDay,
                                        pressureDay,
                                        sunsetDay,
                                        sunriseDay,
                                        descriptionDay
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.d("checkRain", e.toString())
                        }

                    }
                    list.clear()
                    list.addAll(weatherList)
                    adapter.notifyDataSetChanged()

                    listDay.clear()
                    listDay.addAll(weatherDayList)
                    adapterDay.notifyDataSetChanged()

                } catch (e: Exception) {
                    Log.e("WeatherTask", "Error parsing JSON: ${e.message}")
                }
            } else {
                Log.d("WeatherTask", "No response from API")
            }
        }

        private fun isDaytime(dateTime: String): Boolean {
            val hour = dateTime.substring(11, 13).toInt()
            return hour in 6..17
        }
    }

    private fun fetchWeatheDays(){
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                setUpRecycleView()
            }
        }
    }
}