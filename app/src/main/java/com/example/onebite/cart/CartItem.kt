package com.example.onebite.cart

import android.os.Parcelable
import com.example.onebite.data.Pizza
import kotlinx.parcelize.Parcelize

@Parcelize
data class CartItem(
    val pizza: Pizza,
    val quantity: Int,
    val size: String = "Medium",
    val customizations: String? = null
) : Parcelable {

    val totalPrice: Double
        get() = pizza.price * quantity
}
