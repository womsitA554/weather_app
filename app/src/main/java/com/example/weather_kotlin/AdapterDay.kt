package com.example.weather_kotlin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.weather_kotlin.databinding.ItemDayBinding
import com.example.weather_kotlin.databinding.ItemHourBinding

class AdapterDay(private val list: List<DayWeather>) : RecyclerView.Adapter<AdapterDay.viewholder>() {

    var onClickItem: (DayWeather, Int) -> Unit = {_,_ ->}
    inner class viewholder(private val binding: ItemDayBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                onClickItem.invoke(list[position], position)
            }
        }
        fun onBind(dayWeather: DayWeather){
            binding.tvDate.text = dayWeather.date
            val context = binding.imgDay.context
            val resourceId = context.resources.getIdentifier(dayWeather.img, "drawable", context.packageName)
            Glide.with(context).load(resourceId).into(binding.imgDay)
            binding.tvMaxTemp.text = dayWeather.maxTemp + "°C"
            binding.tvMinTemp.text = dayWeather.minTemp + "°C"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterDay.viewholder {
        val binding = ItemDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return viewholder(binding)
    }

    override fun onBindViewHolder(holder: AdapterDay.viewholder, position: Int) {
        holder.onBind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}