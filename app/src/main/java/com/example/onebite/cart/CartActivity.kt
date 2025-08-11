package com.example.onebite.cart

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onebite.R
import com.example.onebite.adapter.CartAdapter
import com.example.onebite.bill.BillGeneratorActivity
import com.example.onebite.databinding.ActivityCartBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var cartAdapter: CartAdapter
    private lateinit var firebaseAuth: FirebaseAuth

    private val cartItems = mutableListOf<CartItem>()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())

    // Pricing variables
    private val taxRate = 0.085 // 8.5%
    private var deliveryCharges = 5.0
    private var discountAmount = 0.0
    private var appliedPromoCode: String? = null

    // Available promo codes with their discount percentages
    private val promoCodes = mapOf(
        "SAVE10" to 0.10, // 10% off
        "WELCOME20" to 0.20, // 20% off
        "FIRST15" to 0.15, // 15% off
        "NEW25" to 0.25, // 25% off
        "PIZZA50" to 0.50 // 50% off (special offer)
    )

    companion object {
        const val REQUEST_CODE_CART = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        setupToolbar()
        setupRecyclerView()
        loadCartData()
        setupClickListeners()
        updateUI()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Shopping Cart"
        }

        binding.toolbar.setNavigationOnClickListener {
            finishWithResult()
        }
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            cartItems = cartItems,
            onQuantityChanged = { cartItem, newQuantity ->
                updateCartItemQuantity(cartItem, newQuantity)
            },
            onItemRemoved = { cartItem ->
                removeCartItem(cartItem)
            }
        )

        binding.recyclerViewCart.apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = cartAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadCartData() {
        val receivedCartItems = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("cart_items", CartItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<CartItem>("cart_items")
        }

        receivedCartItems?.let { items ->
            cartItems.clear()
            cartItems.addAll(items)
            cartAdapter.notifyDataSetChanged()
        }
    }

    private fun setupClickListeners() {
        // Continue Shopping buttons
        binding.btnContinueShopping.setOnClickListener {
            finishWithResult()
        }

        binding.btnContinueShoppingBottom.setOnClickListener {
            finishWithResult()
        }

        // Clear Cart button
        binding.btnClearCart.setOnClickListener {
            showClearCartDialog()
        }

        // Delivery options
        binding.radioGroupDelivery.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioDelivery -> {
                    deliveryCharges = 5.0
                    binding.tvDeliveryLabel.text = "Delivery Charges"
                    binding.tvEstimatedTime.text = "20-30 minutes"
                }
                R.id.radioPickup -> {
                    deliveryCharges = 0.0
                    binding.tvDeliveryLabel.text = "Pickup"
                    binding.tvEstimatedTime.text = "Ready in 15-20 minutes"
                }
            }
            updateBillSummary()
        }

        // Promo code buttons
        binding.btnApplyPromo.setOnClickListener {
            applyPromoCode()
        }

        binding.btnRemovePromo.setOnClickListener {
            removePromoCode()
        }

        // Main action buttons
        binding.btnProceedToCheckout.setOnClickListener {
            if (cartItems.isNotEmpty()) {
                proceedToDirectCheckout()
            } else {
                showEmptyCartMessage()
            }
        }

        binding.btnGoToCheckoutActivity.setOnClickListener {
            if (cartItems.isNotEmpty()) {
                openCheckoutActivity()
            } else {
                showEmptyCartMessage()
            }
        }

        // Quick action buttons
        binding.btnSaveForLater.setOnClickListener {
            saveCartForLater()
        }

        binding.btnShareCart.setOnClickListener {
            shareCart()
        }

        binding.btnViewBill.setOnClickListener {
            if (cartItems.isNotEmpty()) {
                viewBill()
            } else {
                showEmptyCartMessage()
            }
        }
    }

    private fun updateCartItemQuantity(cartItem: CartItem, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeCartItem(cartItem)
        } else {
            val index = cartItems.indexOf(cartItem)
            if (index != -1) {
                cartItems[index] = cartItem.copy(quantity = newQuantity)
                cartAdapter.notifyItemChanged(index)
                updateUI()
            }
        }
    }

    private fun removeCartItem(cartItem: CartItem) {
        val index = cartItems.indexOf(cartItem)
        if (index != -1) {
            cartItems.removeAt(index)
            cartAdapter.notifyItemRemoved(index)
            updateUI()

            Snackbar.make(
                binding.root,
                "${cartItem.pizza.name} removed from cart",
                Snackbar.LENGTH_LONG
            ).setAction("UNDO") {
                cartItems.add(index, cartItem)
                cartAdapter.notifyItemInserted(index)
                updateUI()
            }.show()
        }
    }

    private fun showClearCartDialog() {
        if (cartItems.isEmpty()) {
            showEmptyCartMessage()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Clear Cart")
            .setMessage("Are you sure you want to remove all items from your cart?")
            .setPositiveButton("Clear") { _, _ ->
                clearCart()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearCart() {
        val tempItems = cartItems.toList()
        cartItems.clear()
        cartAdapter.notifyDataSetChanged()
        updateUI()

        Snackbar.make(
            binding.root,
            "Cart cleared",
            Snackbar.LENGTH_LONG
        ).setAction("UNDO") {
            cartItems.addAll(tempItems)
            cartAdapter.notifyDataSetChanged()
            updateUI()
        }.show()
    }

    private fun applyPromoCode() {
        val promoCode = binding.etPromoCode.text.toString().trim().uppercase()

        if (promoCode.isEmpty()) {
            binding.textInputLayoutPromo.error = "Please enter a promo code"
            return
        }

        if (promoCodes.containsKey(promoCode)) {
            // Check if promo code is already applied
            if (appliedPromoCode == promoCode) {
                binding.textInputLayoutPromo.error = "This promo code is already applied"
                return
            }

            appliedPromoCode = promoCode
            val discountPercentage = promoCodes[promoCode]!!
            val subtotal = cartItems.sumOf { it.totalPrice }
            discountAmount = subtotal * discountPercentage

            // Show applied promo
            binding.layoutAppliedPromo.visibility = View.VISIBLE
            binding.tvAppliedPromo.text = "$promoCode Applied - ${(discountPercentage * 100).toInt()}% Off"

            // Clear input and error
            binding.etPromoCode.text?.clear()
            binding.textInputLayoutPromo.error = null

            updateBillSummary()

            Snackbar.make(binding.root, "Promo code applied successfully!", Snackbar.LENGTH_SHORT).show()
        } else {
            binding.textInputLayoutPromo.error = "Invalid promo code"
        }
    }

    private fun removePromoCode() {
        appliedPromoCode = null
        discountAmount = 0.0
        binding.layoutAppliedPromo.visibility = View.GONE
        binding.textInputLayoutPromo.error = null
        updateBillSummary()

        Snackbar.make(binding.root, "Promo code removed", Snackbar.LENGTH_SHORT).show()
    }

    private fun updateUI() {
        val isEmpty = cartItems.isEmpty()

        // Show/hide empty cart vs cart content
        binding.layoutEmptyCart.isVisible = isEmpty
        binding.layoutCartContent.isVisible = !isEmpty

        if (!isEmpty) {
            updateBillSummary()
            updateCartItemsCount()
        }
    }

    private fun updateCartItemsCount() {
        val totalItems = cartItems.sumOf { it.quantity }
        supportActionBar?.title = "Shopping Cart ($totalItems)"
    }

    private fun updateBillSummary() {
        val subtotal = cartItems.sumOf { it.totalPrice }
        val discountedSubtotal = subtotal - discountAmount
        val tax = (discountedSubtotal + deliveryCharges) * taxRate
        val total = discountedSubtotal + deliveryCharges + tax

        // Update UI
        binding.tvSubtotal.text = currencyFormat.format(subtotal)
        binding.tvDeliveryCharges.text = if (deliveryCharges > 0) {
            currencyFormat.format(deliveryCharges)
        } else {
            "Free"
        }
        binding.tvTax.text = currencyFormat.format(tax)
        binding.tvTotalAmount.text = currencyFormat.format(total)

        // Show/hide discount
        binding.layoutDiscount.isVisible = discountAmount > 0
        if (discountAmount > 0) {
            binding.tvDiscount.text = "-${currencyFormat.format(discountAmount)}"
        }
    }

    private fun proceedToDirectCheckout() {
        val subtotal = cartItems.sumOf { it.totalPrice }
        val tax = (subtotal - discountAmount + deliveryCharges) * taxRate
        val total = subtotal - discountAmount + deliveryCharges + tax

        val orderSummary = buildOrderSummary()

        AlertDialog.Builder(this)
            .setTitle("🍕 Confirm Your Order")
            .setMessage(orderSummary)
            .setPositiveButton("Place Order") { _, _ ->
                placeOrder(total)
            }
            .setNegativeButton("Review Cart", null)
            .show()
    }

    private fun openCheckoutActivity() {
        val intent = Intent(this, BillGeneratorActivity::class.java).apply {
            putParcelableArrayListExtra("cart_items", ArrayList(cartItems))
            putExtra("delivery_charges", deliveryCharges)
            putExtra("discount_amount", discountAmount)
            putExtra("promo_code", appliedPromoCode)
            putExtra("is_delivery", binding.radioDelivery.isChecked)
        }
        startActivity(intent)
    }

    private fun viewBill() {
        val currentUser = firebaseAuth.currentUser
        BillGeneratorActivity.startActivity(
            context = this,
            cartItems = ArrayList(cartItems),
            customerName = currentUser?.displayName ?: "Guest Customer",
            customerPhone = "+1 XXX-XXX-XXXX",
            customerAddress = if (binding.radioDelivery.isChecked) "Delivery Address" else "Store Pickup",
            paymentMethod = "Cash on Delivery",
            isDelivery = binding.radioDelivery.isChecked,
            discount = discountAmount
        )
    }

    private fun saveCartForLater() {
        // TODO: Implement save for later functionality with SharedPreferences or database
        if (cartItems.isEmpty()) {
            showEmptyCartMessage()
            return
        }

        Snackbar.make(binding.root, "Cart saved for later (feature coming soon)", Snackbar.LENGTH_SHORT).show()
    }

    private fun shareCart() {
        if (cartItems.isEmpty()) {
            showEmptyCartMessage()
            return
        }

        val cartText = buildCartShareText()
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, cartText)
            putExtra(Intent.EXTRA_SUBJECT, "My Pizza Palace Cart")
        }
        startActivity(Intent.createChooser(shareIntent, "Share Cart"))
    }

    private fun buildCartShareText(): String {
        val builder = StringBuilder()
        builder.append("🍕 Check out my Pizza Palace cart!\n\n")

        cartItems.forEach { item ->
            builder.append("• ${item.pizza.name} (${item.size})\n")
            builder.append("  Quantity: ${item.quantity}\n")
            builder.append("  Price: ${currencyFormat.format(item.totalPrice)}\n")
            if (!item.customizations.isNullOrBlank()) {
                builder.append("  Special: ${item.customizations}\n")
            }
            builder.append("\n")
        }

        val subtotal = cartItems.sumOf { it.totalPrice }
        val total = subtotal - discountAmount + deliveryCharges + ((subtotal - discountAmount + deliveryCharges) * taxRate)

        builder.append("Subtotal: ${currencyFormat.format(subtotal)}\n")
        if (discountAmount > 0) {
            builder.append("Discount: -${currencyFormat.format(discountAmount)}\n")
        }
        if (deliveryCharges > 0) {
            builder.append("Delivery: ${currencyFormat.format(deliveryCharges)}\n")
        }
        builder.append("Total: ${currencyFormat.format(total)}\n")
        builder.append("\nOrder from Pizza Palace app!")

        return builder.toString()
    }

    private fun showEmptyCartMessage() {
        Snackbar.make(binding.root, "Your cart is empty!", Snackbar.LENGTH_SHORT).show()
    }

    private fun buildOrderSummary(): String {
        val currentUser = firebaseAuth.currentUser
        val subtotal = cartItems.sumOf { it.totalPrice }
        val tax = (subtotal - discountAmount + deliveryCharges) * taxRate
        val total = subtotal - discountAmount + deliveryCharges + tax

        return buildString {
            append("Customer: ${currentUser?.email?.substringBefore("@") ?: "Guest"}\n\n")
            append("Order Details:\n")
            cartItems.forEach { cartItem ->
                append("• ${cartItem.pizza.name} (${cartItem.size})\n")
                append("  Qty: ${cartItem.quantity} × ${currencyFormat.format(cartItem.pizza.price)}\n")
                append("  Subtotal: ${currencyFormat.format(cartItem.totalPrice)}\n")
                if (!cartItem.customizations.isNullOrBlank()) {
                    append("  Special: ${cartItem.customizations}\n")
                }
                append("\n")
            }
            append("-------------------\n")
            append("Subtotal: ${currencyFormat.format(subtotal)}\n")

            if (discountAmount > 0) {
                append("Discount ($appliedPromoCode): -${currencyFormat.format(discountAmount)}\n")
            }

            if (deliveryCharges > 0) {
                append("Delivery: ${currencyFormat.format(deliveryCharges)}\n")
            } else {
                append("Pickup: Free\n")
            }

            append("Tax (8.5%): ${currencyFormat.format(tax)}\n")
            append("Total: ${currencyFormat.format(total)}\n\n")

            val deliveryTime = if (binding.radioDelivery.isChecked) "30-45 minutes" else "15-20 minutes"
            append("Estimated time: $deliveryTime")
        }
    }

    private fun placeOrder(totalAmount: Double) {
        // Show loading state
        binding.btnProceedToCheckout.text = "Placing Order..."
        binding.btnProceedToCheckout.isEnabled = false

        // Simulate order processing delay
        binding.btnProceedToCheckout.postDelayed({
            try {
                // Generate order receipt
                val receipt = generateReceipt(totalAmount)

                // Show success message
                showOrderSuccess(receipt)

                // Clear cart and finish
                cartItems.clear()
                finishWithResult()
            } catch (e: Exception) {
                // Handle error
                binding.btnProceedToCheckout.text = "Proceed to Checkout"
                binding.btnProceedToCheckout.isEnabled = true
                Snackbar.make(binding.root, "Order placement failed. Please try again.", Snackbar.LENGTH_LONG).show()
            }
        }, 2000)
    }

    private fun showOrderSuccess(receipt: OrderReceipt) {
        AlertDialog.Builder(this)
            .setTitle("🎉 Order Placed Successfully!")
            .setMessage("Order ID: ${receipt.orderId}\nTotal: ${currencyFormat.format(receipt.total)}\n\nEstimated ${receipt.estimatedDelivery}\n\nThank you for choosing Pizza Palace!")
            .setPositiveButton("OK") { _, _ ->
                finishWithResult()
            }
            .setCancelable(false)
            .show()
    }

    private fun generateReceipt(totalAmount: Double): OrderReceipt {
        val currentUser = firebaseAuth.currentUser
        val subtotal = cartItems.sumOf { it.totalPrice }
        val tax = (subtotal - discountAmount + deliveryCharges) * taxRate
        val orderDate = Date()

        return OrderReceipt(
            orderId = "PP${System.currentTimeMillis()}",
            customerEmail = currentUser?.email ?: "guest@pizzapalace.com",
            orderDate = orderDate,
            items = cartItems.toList(),
            subtotal = subtotal,
            tax = tax,
            total = totalAmount,
            estimatedDelivery = if (binding.radioDelivery.isChecked) "delivery in 30-45 minutes" else "pickup ready in 15-20 minutes"
        )
    }

    private fun finishWithResult() {
        setResult(RESULT_OK, Intent().apply {
            putParcelableArrayListExtra("updated_cart_items", ArrayList(cartItems))
        })
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finishWithResult()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
