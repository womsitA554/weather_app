package com.example.weather_kotlin

import android.annotation.SuppressLint
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weather_kotlin.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val DEFAULT_CITY: String = "London,gb"
    private val API: String = "982cfb3f7fe00de65d41907bfbe382f9"

    private lateinit var adapter: Adapter
    private var list: MutableList<HourWeather> = mutableListOf()
    private var list1: MutableList<HourWeather> = mutableListOf()

    private lateinit var adapterDay: AdapterDay
    private var listDay: MutableList<DayWeather> = mutableListOf()

    private lateinit var address: String
    private var lastQueryTime: Long = 0
    private val debounceDelay: Long = 500 // 500ms để tránh gọi API quá nhanh

    override fun onCreate(savedInstanceState: Bundle?) {
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

        // Thiết lập SearchView và gợi ý
        setupSearchView()

        // Gọi GeocodingTask với thành phố mặc định
        GeocodingTask().execute(DEFAULT_CITY)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                setUpRecycleView()
            }
        }
    }

    private fun setupSearchView() {
        // Thiết lập adapter cho ListView gợi ý
        val suggestionAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        binding.suggestionList.adapter = suggestionAdapter

        // Xử lý click vào gợi ý
        binding.suggestionList.setOnItemClickListener { _, _, position, _ ->
            val selectedCity = suggestionAdapter.getItem(position) ?: return@setOnItemClickListener
            // Xóa truy vấn và ẩn gợi ý
            binding.searchView.setQuery("", false)
            binding.suggestionList.visibility = View.GONE
            binding.searchView.clearFocus()
            // Xóa dữ liệu cũ
            list.clear()
            list1.clear()
            listDay.clear()
            adapter.notifyDataSetChanged()
            adapterDay.notifyDataSetChanged()
            // Gọi GeocodingTask với thành phố được chọn
            GeocodingTask().execute(selectedCity)
        }

        // Xử lý sự kiện tìm kiếm và gợi ý
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.trim()?.let { city ->
                    if (city.isNotEmpty()) {
                        // Xóa dữ liệu cũ
                        list.clear()
                        list1.clear()
                        listDay.clear()
                        adapter.notifyDataSetChanged()
                        adapterDay.notifyDataSetChanged()
                        // Gọi GeocodingTask
                        GeocodingTask().execute(city)
                        binding.searchView.setQuery("", false)
                        binding.suggestionList.visibility = View.GONE
                        binding.searchView.clearFocus()
                    } else {
                        Toast.makeText(this@MainActivity, "Please enter a city name", Toast.LENGTH_SHORT).show()
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.trim()?.let { query ->
                    if (query.length >= 2) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastQueryTime >= debounceDelay) {
                            lastQueryTime = currentTime
                            SuggestionTask(suggestionAdapter).execute(query)
                            binding.suggestionList.visibility = View.VISIBLE
                        }
                    } else {
                        suggestionAdapter.clear()
                        suggestionAdapter.notifyDataSetChanged()
                        binding.suggestionList.visibility = View.GONE
                    }
                } ?: run {
                    suggestionAdapter.clear()
                    suggestionAdapter.notifyDataSetChanged()
                    binding.suggestionList.visibility = View.GONE
                }
                return true
            }
        })

        // Ẩn ListView khi đóng SearchView
        binding.searchView.setOnCloseListener {
            binding.suggestionList.visibility = View.GONE
            false
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
                    val sourceDateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)
                    val targetDateFormat = SimpleDateFormat("MMM, d", Locale.ENGLISH)

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

                    Log.d("SelectedDayWeatherListSize", "Size: ${selectedDayWeatherList.size}")

                    putParcelableArrayListExtra("HOUR_WEATHER_LIST", ArrayList(selectedDayWeatherList))
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

    // AsyncTask để lấy gợi ý thành phố
    inner class SuggestionTask(private val adapter: ArrayAdapter<String>) : AsyncTask<String, Void, List<String>>() {
        override fun doInBackground(vararg params: String?): List<String> {
            val suggestions = mutableListOf<String>()
            var connection: HttpURLConnection? = null
            try {
                val query = params.getOrNull(0) ?: return suggestions
                val url = URL("https://api.openweathermap.org/geo/1.0/direct?q=$query&limit=5&appid=$API")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("SuggestionTask", "Response: $response")
                    val jsonArray = JSONArray(response)
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        if (jsonObject.has("name") && jsonObject.has("country")) {
                            val city = jsonObject.getString("name")
                            val country = jsonObject.getString("country")
                            suggestions.add("$city, $country")
                        }
                    }
                } else {
                    val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e("SuggestionTask", "HTTP error: $responseCode, Message: $errorStream")
                }
            } catch (e: Exception) {
                Log.e("SuggestionTask", "Error fetching suggestions: ${e.message}", e)
            } finally {
                connection?.disconnect()
            }
            return suggestions
        }

        override fun onPostExecute(result: List<String>?) {
            super.onPostExecute(result)
            adapter.clear()
            if (result != null && result.isNotEmpty()) {
                adapter.addAll(result)
                binding.suggestionList.visibility = View.VISIBLE
            } else {
                binding.suggestionList.visibility = View.GONE
            }
            adapter.notifyDataSetChanged()
        }
    }

    inner class GeocodingTask : AsyncTask<String, Void, Pair<Double, Double>?>() {
        override fun doInBackground(vararg params: String?): Pair<Double, Double>? {
            var response: String? = null
            var connection: HttpURLConnection? = null
            try {
                val city = params.getOrNull(0) ?: DEFAULT_CITY
                val url = URL("https://api.openweathermap.org/geo/1.0/direct?q=$city&limit=1&appid=$API")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("GeocodingTask", "Response: $response")
                } else {
                    val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e("GeocodingTask", "HTTP error: $responseCode, Message: $errorStream")
                    return null
                }

                response?.let {
                    val jsonArray = JSONArray(it)
                    if (jsonArray.length() > 0) {
                        val jsonObject = jsonArray.getJSONObject(0)
                        if (jsonObject.has("lat") && jsonObject.has("lon") && jsonObject.has("name") && jsonObject.has("country")) {
                            val lat = jsonObject.getDouble("lat")
                            val lon = jsonObject.getDouble("lon")
                            address = "${jsonObject.getString("name")}, ${jsonObject.getString("country")}"
                            Log.d("GeocodingTask", "Latitude: $lat, Longitude: $lon, Address: $address")
                            return Pair(lat, lon)
                        } else {
                            Log.e("GeocodingTask", "Missing required fields in JSON: $it")
                        }
                    } else {
                        Log.e("GeocodingTask", "No results found for city: $city")
                    }
                }
            } catch (e: Exception) {
                Log.e("GeocodingTask", "Error fetching data: ${e.message}", e)
            } finally {
                connection?.disconnect()
            }
            return null
        }

        override fun onPostExecute(result: Pair<Double, Double>?) {
            super.onPostExecute(result)
            if (result != null) {
                val (lat, lon) = result
                Log.d("GeocodingTask", "Success: Calling weather tasks with lat=$lat, lon=$lon")
                weatherTask(lat, lon).execute()
                WeatherTaskHour(lat, lon).execute()
            } else {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.visibility = View.VISIBLE
                    binding.imgNetwork.visibility = View.VISIBLE
                    Toast.makeText(
                        this@MainActivity,
                        "City not found, please try again",
                        Toast.LENGTH_LONG
                    ).show()
                }
                Log.e("GeocodingTask", "Failed to get coordinates")
            }
        }
    }

    inner class weatherTask(private val lat: Double, private val lon: Double) : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            binding.imgNetwork.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            binding.tvError.visibility = View.GONE
            binding.mainContainer.visibility = View.GONE
        }

        override fun doInBackground(vararg params: String?): String? {
            var response: String? = null
            var connection: HttpURLConnection? = null
            try {
                val url = URL("https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=metric&appid=$API")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    response = connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e("weatherTask", "HTTP error: $responseCode, Message: $errorStream")
                    return null
                }
            } catch (e: Exception) {
                Log.e("weatherTask", "Error fetching data: ${e.message}", e)
            } finally {
                connection?.disconnect()
            }
            return response
        }

        @SuppressLint("SetTextI18n")
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result != null) {
                try {
                    val jsonObj = JSONObject(result)
                    val main = jsonObj.getJSONObject("main")
                    val sys = jsonObj.getJSONObject("sys")
                    val wind = jsonObj.getJSONObject("wind")
                    val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

                    val updateAt: Long = jsonObj.getLong("dt")
                    val updateAtText = SimpleDateFormat("EEE, MMM dd", Locale.ENGLISH).format(Date(updateAt * 1000))
                    val temp = main.getString("temp")
                    val tempMin = "Min.: ${main.getString("temp_min")}°C"
                    val tempMax = "Max.: ${main.getString("temp_max")}°C"
                    val pressure = "${main.getString("pressure")} hPa"
                    val humidity = "${main.getString("humidity")} %"
                    val sunrise: Long = sys.getLong("sunrise")
                    val sunset: Long = sys.getLong("sunset")
                    val windSpeed = "${wind.getString("speed")} km/h"
                    val weatherDescription = weather.getString("description")
                    val checkRain = weather.getString("main")

                    var rainVolume = "0 mm"
                    if (checkRain == "Rain") {
                        if (jsonObj.has("rain")) {
                            val rain = jsonObj.getJSONObject("rain")
                            if (rain.has("1h")) {
                                rainVolume = "${rain.getString("1h")} mm"
                            }
                        }
                    }
                    binding.tvRainVolume.text = rainVolume

                    val weatherId = weather.getString("id")
                    val tempDouble = temp.toDoubleOrNull()
                    val tempInt = tempDouble?.toInt() ?: 0

                    binding.tvUpdateAt.text = getCurrentDate()

                    val currentHour = getCurrentHour().toInt()
                    if (currentHour in 6..17) {
                        when (weatherId) {
                            "800" -> binding.img.setImageResource(R.drawable.sunny)
                            "801", "802" -> binding.img.setImageResource(R.drawable.pcloudy)
                            "803", "804" -> binding.img.setImageResource(R.drawable.mcloudy)
                            "500", "501", "502" -> binding.img.setImageResource(R.drawable.lrain)
                            "503", "504", "511" -> binding.img.setImageResource(R.drawable.rain)
                            "600", "601" -> binding.img.setImageResource(R.drawable.lsnow)
                            "602" -> binding.img.setImageResource(R.drawable.snow)
                            "611", "612", "613", "614" -> binding.img.setImageResource(R.drawable.sleet)
                            "200", "201" -> binding.img.setImageResource(R.drawable.tshower)
                            "202", "210", "211", "212" -> binding.img.setImageResource(R.drawable.tstorm)
                            else -> binding.img.setImageResource(R.drawable.foggy)
                        }
                    } else {
                        when (weatherId) {
                            "800" -> binding.img.setImageResource(R.drawable.moon)
                            "801", "802" -> binding.img.setImageResource(R.drawable.pcloudy_night)
                            "803", "804" -> binding.img.setImageResource(R.drawable.mcloudy_night)
                            "500", "501", "502" -> binding.img.setImageResource(R.drawable.lrain_night)
                            "503", "504", "511" -> binding.img.setImageResource(R.drawable.rain_night)
                            "600", "601" -> binding.img.setImageResource(R.drawable.lsnow_night)
                            "602" -> binding.img.setImageResource(R.drawable.snow_night)
                            "611", "612", "613", "614" -> binding.img.setImageResource(R.drawable.sleet_night)
                            "200", "201" -> binding.img.setImageResource(R.drawable.tshower_night)
                            "202", "210", "211", "212" -> binding.img.setImageResource(R.drawable.tstorm_night)
                            else -> binding.img.setImageResource(R.drawable.foggy_night)
                        }
                    }

                    binding.tvStatus.text = weatherDescription.replaceFirstChar { it.uppercase() }
                    binding.tvTemp.text = tempInt.toString()
                    binding.tvMinTemp.text = tempMin
                    binding.tvMaxTemp.text = tempMax
                    binding.tvSunrise.text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunrise * 1000))
                    binding.tvSunset.text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunset * 1000))
                    binding.tvWind.text = windSpeed
                    binding.tvPressure.text = pressure
                    binding.tvHumidity.text = humidity

                    binding.progressBar.visibility = View.GONE
                    binding.mainContainer.visibility = View.VISIBLE

                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.visibility = View.VISIBLE
                    binding.imgNetwork.visibility = View.VISIBLE
                    Log.e("weatherTask", "Error parsing JSON: ${e.message}", e)
                }
            } else {
                binding.progressBar.visibility = View.GONE
                binding.tvError.visibility = View.VISIBLE
                binding.imgNetwork.visibility = View.VISIBLE
                Log.e("weatherTask", "No response from API")
            }
        }
    }

    inner class WeatherTaskHour(private val lat: Double, private val lon: Double) : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            var response: String? = null
            var connection: HttpURLConnection? = null
            try {
                val url = URL("https://api.openweathermap.org/data/2.5/forecast?lat=$lat&lon=$lon&units=metric&appid=$API")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    response = connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e("WeatherTaskHour", "HTTP error: $responseCode, Message: $errorStream")
                    return null
                }
            } catch (e: Exception) {
                Log.e("WeatherTaskHour", "Error fetching data: ${e.message}", e)
            } finally {
                connection?.disconnect()
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

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val dateFormat12 = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val calendar = Calendar.getInstance()

                    calendar.set(Calendar.HOUR_OF_DAY, 15)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    val startTime = dateFormat.format(calendar.time)

                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 12)
                    val endTime = dateFormat.format(calendar.time)

                    val defaultTime12 = "12:00:00"

                    val city = jsonObj.getJSONObject("city")
                    val sunriseDay: Long = city.getLong("sunrise")
                    val sunsetDay: Long = city.getLong("sunset")

                    for (i in 0 until listJson.length()) {
                        try {
                            val item = listJson.getJSONObject(i)
                            val main = item.getJSONObject("main")
                            val wind = item.getJSONObject("wind")
                            val weather = item.getJSONArray("weather").getJSONObject(0)
                            val checkRain = weather.getString("main")
                            var rainVolumeDay = "0"
                            if (checkRain == "Rain") {
                                if (item.has("rain")) {
                                    val rain = item.getJSONObject("rain")
                                    rainVolumeDay = rain.optString("3h", "0")
                                }
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
                            val timeString1 = timeFormat1.format(date1).toUpperCase(Locale.ENGLISH)

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
                                "701", "702", "703", "704", "771", "762", "763", "751", "761", "752" ->
                                    if (isDaytime(dtTxt)) "foggy" else "foggy_night"
                                else -> if (isDaytime(dtTxt)) "foggy" else "foggy_night"
                            }

                            val tempDouble = temp.toDoubleOrNull() ?: 0.0
                            val tempInt = tempDouble.toInt()

                            weatherList1.add(HourWeather("$date1", "$tempInt", weatherIcon1, timeString1))
                            list1.clear()
                            list1.addAll(weatherList1)

                            if (dtTxt >= startTime && dtTxt <= endTime) {
                                val date = dateFormat.parse(dtTxt)
                                val timeFormat = SimpleDateFormat("ha", Locale.ENGLISH)
                                val timeString = timeFormat.format(date).toUpperCase(Locale.ENGLISH)

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
                                    "701", "702", "703", "704", "771", "762", "763", "751", "761", "752" ->
                                        if (isDaytime(dtTxt)) "foggy" else "foggy_night"
                                    else -> if (isDaytime(dtTxt)) "foggy" else "foggy_night"
                                }

                                weatherList.add(HourWeather("$date", "$tempInt", weatherIcon, timeString))
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
                                    "801", "802" -> "pcloudy"
                                    "803", "804" -> "mcloudy"
                                    "500", "502" -> "lrain"
                                    "503", "504" -> "rain"
                                    "511", "600", "601", "602" -> "lsnow"
                                    "611", "612", "613", "614" -> "sleet"
                                    "200", "201" -> "tshower"
                                    "202", "210", "211", "212" -> "tstorm"
                                    "701", "702", "703", "704", "771", "762", "763", "751", "761", "752" -> "foggy"
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
                            Log.e("WeatherTaskHour", "Error processing item: ${e.message}")
                        }
                    }

                    list.clear()
                    list.addAll(weatherList)
                    adapter.notifyDataSetChanged()

                    listDay.clear()
                    listDay.addAll(weatherDayList)
                    adapterDay.notifyDataSetChanged()

                } catch (e: Exception) {
                    Log.e("WeatherTaskHour", "Error parsing JSON: ${e.message}")
                }
            } else {
                Log.e("WeatherTaskHour", "No response from API")
            }
        }

        private fun isDaytime(dateTime: String): Boolean {
            val hour = dateTime.substring(11, 13).toInt()
            return hour in 6..17
        }
    }
}