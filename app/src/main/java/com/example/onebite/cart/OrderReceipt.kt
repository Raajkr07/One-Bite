package com.example.onebite.cart

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class OrderReceipt(
    val orderId: String,
    val customerEmail: String,
    val orderDate: Date,
    val items: List<CartItem>,
    val subtotal: Double,
    val tax: Double,
    val total: Double,
    val estimatedDelivery: String
) : Parcelable
