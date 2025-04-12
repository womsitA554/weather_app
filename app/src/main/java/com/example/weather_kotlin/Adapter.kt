package com.example.weather_kotlin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.weather_kotlin.databinding.ItemHourBinding

class Adapter(private val list: List<HourWeather>) : RecyclerView.Adapter<Adapter.ViewHolder>() {
    class ViewHolder(private val binding: ItemHourBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(hourWeather: HourWeather) {
            binding.tvTemp.text = hourWeather.temp + "°C"
            val context = binding.imgDay.context
            val resourceId = context.resources.getIdentifier(hourWeather.img, "drawable", context.packageName)
            Glide.with(context).load(resourceId).into(binding.imgDay)
            binding.tvTime.text = hourWeather.time
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHourBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(list[position])
    }
}
