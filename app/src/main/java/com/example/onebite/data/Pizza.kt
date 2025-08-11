package com.example.onebite.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Pizza(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageResource: Int = 0,
    val imageUrl: String = ""
) : Parcelable
