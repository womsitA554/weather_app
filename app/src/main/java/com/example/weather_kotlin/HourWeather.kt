package com.example.weather_kotlin

import android.os.Parcel
import android.os.Parcelable

data class HourWeather (
    val date: String,
    val temp: String,
    val img: String,
    val time: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(date)
        parcel.writeString(temp)
        parcel.writeString(img)
        parcel.writeString(time)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<HourWeather> {
        override fun createFromParcel(parcel: Parcel): HourWeather {
            return HourWeather(parcel)
        }

        override fun newArray(size: Int): Array<HourWeather?> {
            return arrayOfNulls(size)
        }
    }
}
