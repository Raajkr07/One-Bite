package com.example.onebite.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.onebite.R
import com.example.onebite.data.Pizza
import com.example.onebite.databinding.ItemPizzaBinding
import java.text.NumberFormat
import java.util.*

class PizzaAdapter(
    private val context: Context,
    private val pizzaList: MutableList<Pizza>,
    private val onAddToCart: (Pizza) -> Unit,
    private val onPizzaClick: (Pizza) -> Unit
) : RecyclerView.Adapter<PizzaAdapter.PizzaViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
    private val filteredPizzaList = mutableListOf<Pizza>().apply { addAll(pizzaList) }

    inner class PizzaViewHolder(val binding: ItemPizzaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PizzaViewHolder {
        val binding = ItemPizzaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PizzaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PizzaViewHolder, position: Int) {
        val pizza = filteredPizzaList[position]
        android.util.Log.d("PizzaDebug", "Binding pizza at position $position: ${pizza.name}")
        holder.binding.bind(pizza)
    }

    override fun getItemCount(): Int {
        android.util.Log.d("PizzaDebug", "getItemCount() called: ${filteredPizzaList.size}")
        return filteredPizzaList.size
    }

    private fun ItemPizzaBinding.bind(pizza: Pizza) {
        tvPizzaName.text = pizza.name
        tvPizzaDescription.text = pizza.description
        tvPizzaPrice.text = currencyFormat.format(pizza.price)

        if (pizza.imageResource != 0) {
            ivPizzaImage.setImageResource(pizza.imageResource)
        } else {
            // Fallback to ID-based mapping
            when (pizza.id) {
                "1" -> ivPizzaImage.setImageResource(R.drawable.italian_cheesy_margherita_pizza)
                "2" -> ivPizzaImage.setImageResource(R.drawable.pepperoni_pizza)
                "3" -> ivPizzaImage.setImageResource(R.drawable.veg_pizza)
                "4" -> ivPizzaImage.setImageResource(R.drawable.margherita_pizza)
                "5" -> ivPizzaImage.setImageResource(R.drawable.italian_cheesy_margherita_pizza)
                "6" -> ivPizzaImage.setImageResource(R.drawable.classic_pizza)
                else -> ivPizzaImage.setImageResource(R.drawable.pizza_margherita)
            }
        }

        btnAddToCart.setOnClickListener { view ->
            onAddToCart(pizza)

            // Button animation
            view.animate()
                .scaleX(0.95f).scaleY(0.95f).setDuration(100)
                .withEndAction {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }
                .start()
        }

        root.setOnClickListener {
            onPizzaClick(pizza)
        }
    }

    fun refreshData() {
        android.util.Log.d("PizzaDebug", "refreshData() called - pizzaList size: ${pizzaList.size}")
        filteredPizzaList.apply {
            clear()
            addAll(pizzaList)
        }
        android.util.Log.d("PizzaDebug", "filteredPizzaList updated - size: ${filteredPizzaList.size}")
        notifyDataSetChanged()
    }

    fun updatePizzaList(newList: List<Pizza>) {
        pizzaList.clear()
        pizzaList.addAll(newList)
        refreshData()
    }

    fun filterPizzas(query: String) {
        filteredPizzaList.clear()
        if (query.isBlank()) {
            filteredPizzaList.addAll(pizzaList)
        } else {
            filteredPizzaList.addAll(
                pizzaList.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.description.contains(query, ignoreCase = true)
                }
            )
        }
        notifyDataSetChanged()
    }

    fun clearFilters() = refreshData()
}
