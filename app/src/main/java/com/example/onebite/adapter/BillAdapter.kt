package com.example.onebite.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.onebite.cart.CartItem
import com.example.onebite.databinding.ItemBillBinding

class BillAdapter(private val billItems: List<CartItem>) : RecyclerView.Adapter<BillAdapter.BillViewHolder>() {

    inner class BillViewHolder(private val binding: ItemBillBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CartItem) {
            binding.apply {
                // Fixed property access - using pizza.name and pizza.price
                tvItemName.text = item.pizza.name
                tvItemQuantity.text = item.quantity.toString()
                tvItemPrice.text = String.format("₹%.2f", item.pizza.price * item.quantity)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val binding = ItemBillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        holder.bind(billItems[position])
    }

    override fun getItemCount(): Int = billItems.size
}
