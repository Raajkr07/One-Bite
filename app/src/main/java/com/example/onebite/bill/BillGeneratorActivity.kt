package com.example.onebite.bill

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onebite.adapter.BillAdapter
import com.example.onebite.cart.CartItem
import com.example.onebite.databinding.ActivityCheckoutBinding
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class BillGeneratorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding
    private lateinit var billAdapter: BillAdapter
    private var cartItems: ArrayList<CartItem> = arrayListOf()

    // Bill calculation variables
    private var subtotal = 0.0
    private var deliveryCharges = 5.0
    private var taxRate = 0.085 // 8.5%
    private var discount = 0.0
    private var totalAmount = 0.0

    // Customer details
    private var customerName = "Raj Kumar"
    private var customerPhone = "+91 790-327-XXXX"
    private var customerAddress = "Kharar, Chandigarh, India"
    private var paymentMethod = "Online Payment (Card/UPI)"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        getDataFromIntent()
        setupRecyclerView()
        setupBillDetails()
        setupClickListeners()
        calculateBill()
        updateUI()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getDataFromIntent() {
        cartItems = intent.getSerializableExtra("cart_items") as? ArrayList<CartItem> ?: arrayListOf()

        customerName = intent.getStringExtra("customer_name") ?: "Raj Kumar"
        customerPhone = intent.getStringExtra("customer_phone") ?: "+91 790-327-XXXX"
        customerAddress = intent.getStringExtra("customer_address") ?: "Kharar, Chandigarh, India"
        paymentMethod = intent.getStringExtra("payment_method") ?: "Online Payment (Card/UPI)"

        val isDelivery = intent.getBooleanExtra("is_delivery", true)
        deliveryCharges = if (isDelivery) 5.0 else 0.0

        discount = intent.getDoubleExtra("discount", 0.0)
    }

    private fun setupRecyclerView() {
        billAdapter = BillAdapter(cartItems)
        binding.recyclerViewBillItems.apply {
            layoutManager = LinearLayoutManager(this@BillGeneratorActivity)
            adapter = billAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupBillDetails() {
        // Generate bill number
        val billNumber = "#PP${System.currentTimeMillis().toString().takeLast(6)}"
        binding.tvBillNumber.text = billNumber

        // Set current date and time
        val currentDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm a", Locale.getDefault())
        binding.tvDateTime.text = currentDateTime.format(Date())

        // Set customer details
        binding.tvCustomerName.text = customerName
        binding.tvCustomerPhone.text = "Phone: $customerPhone"
        binding.tvCustomerAddress.text = customerAddress

        // Set payment details
        binding.tvPaymentMethod.text = paymentMethod
        binding.tvPaymentStatus.text = when (paymentMethod) {
            "Cash on Delivery" -> "Status: Pending"
            "Online Payment (Card/UPI)" -> "Status: Paid"
            "Digital Wallet" -> "Status: Paid"
            else -> "Status: Pending"
        }
    }

    private fun calculateBill() {
        subtotal = cartItems.sumOf { it.pizza.price * it.quantity }

        val taxAmount = (subtotal + deliveryCharges - discount) * taxRate

        totalAmount = subtotal + deliveryCharges + taxAmount - discount

        binding.tvBillSubtotal.text = String.format("₹%.2f", subtotal)
        binding.tvDeliveryCharges.text = if (deliveryCharges > 0) String.format("₹%.2f", deliveryCharges) else "Free"
        binding.tvBillTax.text = String.format("₹%.2f", taxAmount)
        binding.tvBillTotalAmount.text = String.format("₹%.2f", totalAmount)

        if (discount > 0) {
            binding.layoutDiscount.visibility = android.view.View.VISIBLE
            binding.tvDiscount.text = String.format("-₹%.2f", discount)
        } else {
            binding.layoutDiscount.visibility = android.view.View.GONE
        }
    }

    private fun updateUI() {
        binding.recyclerViewBillItems.adapter?.notifyDataSetChanged()
    }

    private fun setupClickListeners() {
        binding.btnDownloadBill.setOnClickListener {
            downloadBillAsPDF()
        }

        binding.btnShareBill.setOnClickListener {
            shareBill()
        }
    }

    private fun downloadBillAsPDF() {
        // TODO: Implement PDF generation functionality
        Snackbar.make(binding.root, "PDF feature is coming, Soon!!!", Snackbar.LENGTH_SHORT).show()
    }

    private fun shareBill() {
        val billText = generateBillText()

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, billText)
            putExtra(Intent.EXTRA_SUBJECT, "Pizza Palace - Bill ${binding.tvBillNumber.text}")
        }

        startActivity(Intent.createChooser(shareIntent, "Share Bill"))
    }

    private fun generateBillText(): String {
        val builder = StringBuilder()

        builder.append("🍕 One Bite Pizza's\n")
        builder.append("Chandigarh, India\n")
        builder.append("Phone: +91 7903287***\n\n")

        builder.append("Bill No: ${binding.tvBillNumber.text}\n")
        builder.append("Date & Time: ${binding.tvDateTime.text}\n\n")

        builder.append("Customer Details:\n")
        builder.append("Name: $customerName\n")
        builder.append("Phone: $customerPhone\n")
        builder.append("Address: $customerAddress\n\n")

        builder.append("ORDER ITEMS:\n")
        builder.append("----------------------------------------\n")
        cartItems.forEach { item ->
            // Fixed property access
            builder.append("${item.pizza.name} x${item.quantity} - ₹${String.format("%.2f", item.pizza.price * item.quantity)}\n")
        }
        builder.append("----------------------------------------\n\n")

        builder.append("PAYMENT SUMMARY:\n")
        builder.append("Subtotal: ₹${String.format("%.2f", subtotal)}\n")

        if (deliveryCharges > 0) {
            builder.append("Delivery Charges: ₹${String.format("%.2f", deliveryCharges)}\n")
        } else {
            builder.append("Delivery Charges: Free\n")
        }

        if (discount > 0) {
            builder.append("Discount: -₹${String.format("%.2f", discount)}\n")
        }

        builder.append("Tax (8.5%): ${binding.tvBillTax.text}\n")
        builder.append("TOTAL AMOUNT: ₹${String.format("%.2f", totalAmount)}\n\n")

        builder.append("Payment Method: $paymentMethod\n")
        builder.append("Status: ${binding.tvPaymentStatus.text.toString().substringAfter("Status: ")}\n\n")

        builder.append("Thank you for your order!\n")
        builder.append("We hope you enjoy your delicious pizza! 🍕")

        return builder.toString()
    }

    companion object {
        fun startActivity(
            context: android.content.Context,
            cartItems: ArrayList<CartItem>,
            customerName: String = "Raj Kumar",
            customerPhone: String = "+91 790-327-XXXX",
            customerAddress: String = "Kharar, Chandigarh, India",
            paymentMethod: String = "Online Payment (Card/UPI)",
            isDelivery: Boolean = true,
            discount: Double = 0.0
        ) {
            val intent = Intent(context, BillGeneratorActivity::class.java).apply {
                putExtra("cart_items", cartItems)
                putExtra("customer_name", customerName)
                putExtra("customer_phone", customerPhone)
                putExtra("customer_address", customerAddress)
                putExtra("payment_method", paymentMethod)
                putExtra("is_delivery", isDelivery)
                putExtra("discount", discount)
            }
            context.startActivity(intent)
        }
    }
}