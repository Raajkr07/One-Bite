package com.example.onebite

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import com.example.onebite.adapter.PizzaAdapter
import com.example.onebite.cart.CartActivity
import com.example.onebite.cart.CartItem
import com.example.onebite.data.Pizza
import com.example.onebite.databinding.ActivityPizzaMenuBinding
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.util.*

class PizzaMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPizzaMenuBinding
    private lateinit var pizzaAdapter: PizzaAdapter
    private lateinit var firebaseAuth: FirebaseAuth
    private var cartBadge: BadgeDrawable? = null

    private val pizzaList = mutableListOf<Pizza>()
    private val cartItems = mutableListOf<CartItem>()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    private val cartLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val updatedCartItems =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        result.data?.getParcelableArrayListExtra("updated_cart_items", CartItem::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        result.data?.getParcelableArrayListExtra<CartItem>("updated_cart_items")
                    }

                updatedCartItems?.let { items ->
                    cartItems.clear()
                    cartItems.addAll(items)
                    updateCartBadge()
                    updateFabText()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.d("PizzaDebug", "=== onCreate STARTED ===")
        super.onCreate(savedInstanceState)

        android.util.Log.d("PizzaDebug", "=== Setting content view ===")
        binding = ActivityPizzaMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        android.util.Log.d("PizzaDebug", "=== FirebaseAuth init ===")
        firebaseAuth = FirebaseAuth.getInstance()

        android.util.Log.d("PizzaDebug", "=== Setting up toolbar ===")
        setupToolbar()

        android.util.Log.d("PizzaDebug", "=== Setting up recycler ===")
        setupRecycler()

        android.util.Log.d("PizzaDebug", "=== Setting up click listeners ===")
        setupClickListeners()

        android.util.Log.d("PizzaDebug", "=== Setting up FAB ===")
        setupFab()

        android.util.Log.d("PizzaDebug", "=== Loading pizza data ===")
        loadPizzaData()

        android.util.Log.d("PizzaDebug", "=== onCreate completed ===")
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { anchor ->
            showNavigationMenu(anchor)
        }
    }

    private fun showNavigationMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_logout -> {
                    logoutUser()
                    true
                }
                R.id.action_profile -> {
                    // TODO - open profile screen
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun logoutUser() {
        firebaseAuth.signOut()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun setupRecycler() {
        android.util.Log.d("PizzaDebug", "Setting up RecyclerView with ${pizzaList.size} items")

        pizzaAdapter = PizzaAdapter(
            context = this,
            pizzaList = pizzaList,
            onAddToCart = { pizza -> addToCart(pizza) },
            onPizzaClick = { pizza -> showPizzaDetails(pizza) }
        )

        binding.recyclerViewPizzas.apply {
            layoutManager = LinearLayoutManager(this@PizzaMenuActivity)
            adapter = pizzaAdapter
            setHasFixedSize(true)
        }

        android.util.Log.d("PizzaDebug", "RecyclerView setup completed")
    }

    private fun showPizzaDetails(pizza: Pizza) {
        Snackbar.make(
            binding.root,
            "Pizza details: ${pizza.name} - ${currencyFormat.format(pizza.price)}",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun setupClickListeners() {
        binding.btnRefresh.setOnClickListener { loadPizzaData() }
    }

    @OptIn(ExperimentalBadgeUtils::class)
    private fun setupFab() {
        updateFabText()
        binding.fabCart.setOnClickListener { openCart() }

        cartBadge = BadgeDrawable.create(this).apply {
            backgroundColor = getColor(R.color.red)
            badgeTextColor = getColor(R.color.white)
            isVisible = false
        }
        BadgeUtils.attachBadgeDrawable(cartBadge!!, binding.fabCart)
    }

    private fun loadPizzaData() {
        showLoadingState()
        binding.root.postDelayed({
            loadSamplePizzas()
            hideLoadingState()
        }, 1_500)
    }

    private fun loadSamplePizzas() {
        pizzaList.clear()
        pizzaList.addAll(
            listOf(
                Pizza("1", "Margherita Classic", "Fresh tomatoes, mozzarella cheese, basil leaves", 249.0, R.drawable.italian_cheesy_margherita_pizza),
                Pizza("2", "Pepperoni Delight", "Spicy pepperoni, mozzarella cheese, oregano", 349.0, R.drawable.pepperoni_pizza),
                Pizza("3", "Veggie Supreme", "Bell peppers, onions, mushrooms, olives, corn", 299.0, R.drawable.veg_pizza),
                Pizza("4", "BBQ Paneer", "Grilled paneer, BBQ sauce, red onions, cilantro", 399.0, R.drawable.margherita_pizza),
                Pizza("5", "Four Cheese", "Mozzarella, cheddar, parmesan, goat cheese", 379.0, R.drawable.italian_cheesy_margherita_pizza),
                Pizza("6", "Spicy Mexican", "Jalapenos, pepperoni, onions, spicy sauce", 359.0, R.drawable.classic_pizza)
            )
        )

        pizzaAdapter.refreshData()
        android.util.Log.d("PizzaDebug", "refreshData() called")
        android.util.Log.d("PizzaDebug", "=== loadSamplePizzas completed ===")
    }

    private fun addToCart(pizza: Pizza) {
        val existingIndex = cartItems.indexOfFirst { it.pizza.id == pizza.id }

        if (existingIndex != -1) {
            val existing = cartItems[existingIndex]
            cartItems[existingIndex] = existing.copy(quantity = existing.quantity + 1)
        } else {
            cartItems.add(
                CartItem(
                    pizza = pizza,
                    quantity = 1,
                    size = "Medium",
                    customizations = null
                )
            )
        }

        updateCartBadge()
        updateFabText()

        Snackbar.make(binding.root, "${pizza.name} added to cart", Snackbar.LENGTH_SHORT)
            .setAction("VIEW CART") { openCart() }
            .show()
    }

    private fun openCart() {
        if (cartItems.isEmpty()) {
            Snackbar.make(binding.root, "Your cart is empty!", Snackbar.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, CartActivity::class.java)
            .putParcelableArrayListExtra("cart_items", ArrayList(cartItems))
        cartLauncher.launch(intent)
    }

    private fun updateCartBadge() {
        val total = cartItems.sumOf { it.quantity }
        cartBadge?.apply {
            isVisible = total > 0
            if (isVisible) number = total
        }
    }

    private fun updateFabText() {
        val totalItems = cartItems.sumOf { it.quantity }
        val totalPrice = cartItems.sumOf { it.totalPrice }
        binding.fabCart.text = "View Cart ($totalItems) - ${currencyFormat.format(totalPrice)}"
    }

    private fun showLoadingState() {
        binding.layoutLoading.isVisible = true
        binding.recyclerViewPizzas.isVisible = false
        binding.layoutEmptyState.isVisible = false
    }

    private fun hideLoadingState() {
        binding.layoutLoading.isVisible = false

        if (pizzaList.isNotEmpty()) {
            showContentState()
        } else {
            showEmptyState()
        }
    }

    private fun showEmptyState() {
        binding.layoutEmptyState.isVisible = true
        binding.recyclerViewPizzas.isVisible = false
    }

    private fun showContentState() {
        binding.layoutEmptyState.isVisible = false
        binding.recyclerViewPizzas.isVisible = true
    }

    override fun onResume() {
        super.onResume()
        updateCartBadge()
        updateFabText()
    }
}
