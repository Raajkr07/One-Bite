package com.example.onebite.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.onebite.cart.CartItem
import com.example.onebite.databinding.ItemCartBinding
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private val cartItems: MutableList<CartItem>,
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onItemRemoved: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(cartItems[position])
    }

    override fun getItemCount(): Int = cartItems.size

    inner class CartViewHolder(
        private val binding: ItemCartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cartItem: CartItem) {
            binding.apply {
                // Set pizza details
                tvCartPizzaName.text = cartItem.pizza.name
                tvCartPizzaPrice.text = currencyFormat.format(cartItem.pizza.price)
                tvQuantity.text = cartItem.quantity.toString()
                ivCartPizzaImage.setImageResource(cartItem.pizza.imageResource)

                // Set pizza size information
                tvCartPizzaSize.text = "${cartItem.size} • Thin Crust"

                // Calculate and display total price for this item
                val itemTotal = cartItem.totalPrice
                tvCartItemTotal.text = "Total: ${currencyFormat.format(itemTotal)}"

                // Show/hide customizations
                if (cartItem.customizations.isNullOrBlank()) {
                    tvCartPizzaCustomizations.visibility = View.GONE
                } else {
                    tvCartPizzaCustomizations.visibility = View.VISIBLE
                    tvCartPizzaCustomizations.text = cartItem.customizations
                }

                // Decrease quantity button
                btnDecrease.setOnClickListener {
                    if (cartItem.quantity > 1) {
                        onQuantityChanged(cartItem, cartItem.quantity - 1)
                    } else {
                        // If quantity is 1, remove the item instead
                        onItemRemoved(cartItem)
                    }
                }

                // Increase quantity button
                btnIncrease.setOnClickListener {
                    onQuantityChanged(cartItem, cartItem.quantity + 1)
                }

                // Remove item button
                btnRemoveItem.setOnClickListener {
                    onItemRemoved(cartItem)
                }
            }
        }
    }
}
