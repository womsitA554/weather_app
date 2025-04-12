package com.example.weather_kotlin

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weather_kotlin.databinding.ActivityDetailBinding
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class Detail : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var adapter: Adapter
    private var list: MutableList<HourWeather> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve the passed data
        val date = intent.getStringExtra("DATE")
        val weatherIcon = intent.getStringExtra("WEATHER_ICON")
        val maxTemp = intent.getStringExtra("MAX_TEMP")
        val minTemp = intent.getStringExtra("MIN_TEMP")
        val address = intent.getStringExtra("ADDRESS")
        val humidity = intent.getStringExtra("HUMIDITY")
        val rainVolume = intent.getStringExtra("RAIN_VOLUME")
        val windSpeed = intent.getStringExtra("WIND_SPEED")
        val pressure = intent.getStringExtra("PRESSURE")
        val sunset = intent.getLongExtra("SUNSET", 0L)
        val sunrise = intent.getLongExtra("SUNRISE", 0L)
        val description = intent.getStringExtra("DESCRIPTION")

        // Display the retrieved data
        binding.tvAddress.text = address
        binding.img.setImageResource(getWeatherIconResource(weatherIcon))
        binding.tvMaxTemp.text = "Max.: $maxTemp°C"
        binding.tvMinTemp.text = "Min.: $minTemp°C"
        binding.tvHumidity.text = "$humidity %"
        binding.tvRainVolume.text = "$rainVolume mm"
        binding.tvWind.text = "$windSpeed km/h"
        binding.tvPressure.text = "$pressure hPa"
        binding.tvSunset.text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunset * 1000))
        binding.tvSunrise.text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunrise * 1000))
        binding.tvStatus.text = description

        // Retrieve the list of HourWeather objects
        val hourWeatherList: ArrayList<HourWeather>? = intent.getParcelableArrayListExtra("HOUR_WEATHER_LIST")

        // Log the size and content of the received list
        if (hourWeatherList != null) {
            Log.d("hourWeatherList", "Size: ${hourWeatherList.size}")
            hourWeatherList.forEach {
                Log.d("hourWeatherListItem", "Date: ${it.date}, Temp: ${it.temp}")
            }
        } else {
            Log.d("hourWeatherListItem", "Size: 0")
        }

        // Display the list of HourWeather objects
        hourWeatherList?.let {
            list.clear()
            list.addAll(it)
            adapter.notifyDataSetChanged()
        }


        setUpRecycleView()
    }

    private fun setUpRecycleView() {
        adapter = Adapter(list)
        binding.recyleviewDayWeather.adapter = adapter
        binding.recyleviewDayWeather.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    // Map weather icon strings to drawable resources
    private fun getWeatherIconResource(iconName: String?): Int {
        return when (iconName) {
            "sunny" -> R.drawable.sunny
            "pcloudy" -> R.drawable.pcloudy
            "mcloudy" -> R.drawable.mcloudy
            "lrain" -> R.drawable.lrain
            "rain" -> R.drawable.rain
            "lsnow" -> R.drawable.lsnow
            "sleet" -> R.drawable.sleet
            "tshower" -> R.drawable.tshower
            "tstorm" -> R.drawable.tstorm
            "foggy" -> R.drawable.foggy
            else -> R.drawable.snow // Provide a default icon
        }
    }
}
