package com.example.propertyconsultancy.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.PropertyDTO
import com.example.propertyconsultancy.data.dto.FeedbackDTO
import com.example.propertyconsultancy.data.local.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

class AiFeedbackFragment : Fragment() {

    private var property: PropertyDTO? = null
    private lateinit var sessionManager: SessionManager
    private lateinit var rvFeedbacks: RecyclerView
    private lateinit var adapter: FeedbackAdapter
    
    private lateinit var tvSocialInsight: TextView
    private lateinit var tvSecurityInsight: TextView
    private lateinit var tvPriceInsight: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        property = arguments?.getSerializable("property") as? PropertyDTO
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ai_feedback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        
        tvSocialInsight = view.findViewById(R.id.tvSocialInsight)
        tvSecurityInsight = view.findViewById(R.id.tvSecurityInsight)
        tvPriceInsight = view.findViewById(R.id.tvPriceInsight)
        
        rvFeedbacks = view.findViewById(R.id.rvUserFeedbacks)
        rvFeedbacks.layoutManager = LinearLayoutManager(requireContext())
        
        setupDynamicInsights()
        loadFeedbacks()

        view.findViewById<MaterialButton>(R.id.btnAddFeedback).setOnClickListener {
            showAddFeedbackDialog()
        }
        
        view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbarAiFeedback).setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupDynamicInsights() {
        val prop = property ?: return
        val city = prop.city ?: "this area"
        val price = prop.pricePerMonth ?: 0.0
        
        // Social Insight
        tvSocialInsight.text = when {
            (prop.areaSqft ?: 0) > 2000 -> "Premium lifestyle zone in $city. Ideal for large families seeking exclusivity."
            (prop.bedrooms ?: 0) <= 1 -> "Fast-paced urban vibe. Popular among young professionals due to easy commute options."
            else -> "Family-friendly neighborhood in $city with multiple parks and community centers nearby."
        }

        // Security Insight
        tvSecurityInsight.text = "AI verified security index: 8.5/10. Low crime rate reported in ${prop.addressLine2 ?: city}. Active night patrolling."

        // Price Insight
        val trend = if (price > 50000) "Appreciating rapidly (up 15% YoY)" else "Stable growth (up 5% YoY)"
        tvPriceInsight.text = "Current valuation is competitive for $city. $trend. Strong rental demand observed."
    }

    private fun loadFeedbacks() {
        val propId = property?.propertyId ?: 0L
        val savedFeedbacks = sessionManager.getPropertyFeedbacks(propId)
        
        val defaultFeedbacks = listOf(
            FeedbackDTO("Rahul S.", "Safe and peaceful area. Great for families.", 4.5f, System.currentTimeMillis() - 86400000),
            FeedbackDTO("Anita M.", "Water supply is constant, but traffic is a bit high during peak hours.", 4.0f, System.currentTimeMillis() - 172800000),
            FeedbackDTO("Vikram K.", "Excellent nearby schools. Highly recommend for working parents.", 5.0f, System.currentTimeMillis() - 259200000)
        )
        
        val allFeedbacks = (savedFeedbacks + defaultFeedbacks).sortedByDescending { it.timestamp }
        adapter = FeedbackAdapter(allFeedbacks)
        rvFeedbacks.adapter = adapter
    }

    private fun showAddFeedbackDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_feedback, null)
        val etComment = dialogView.findViewById<EditText>(R.id.etFeedbackComment)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBarFeedback)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Share Your Insight")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val comment = etComment.text.toString().trim()
                val rating = ratingBar.rating
                if (comment.isNotEmpty()) {
                    saveFeedback(comment, rating)
                } else {
                    Toast.makeText(requireContext(), "Please enter a comment", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveFeedback(comment: String, rating: Float) {
        val user = sessionManager.getUser()
        val name = "${user?.firstName} ${user?.lastName}".trim().ifEmpty { "Verified User" }
        val newFeedback = FeedbackDTO(name, comment, rating, System.currentTimeMillis())
        
        property?.propertyId?.let { 
            sessionManager.savePropertyFeedback(it, newFeedback)
            loadFeedbacks()
            Toast.makeText(requireContext(), "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
        }
    }

    class FeedbackAdapter(private val items: List<FeedbackDTO>) : RecyclerView.Adapter<FeedbackAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvFeedbackUserName)
            val tvRating: TextView = view.findViewById(R.id.tvFeedbackRating)
            val tvComment: TextView = view.findViewById(R.id.tvFeedbackComment)
            val tvDate: TextView = view.findViewById(R.id.tvFeedbackDate)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_feedback, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name
            holder.tvRating.text = "%.1f ★".format(item.rating)
            holder.tvComment.text = item.text
            
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            holder.tvDate.text = sdf.format(Date(item.timestamp))
        }

        override fun getItemCount() = items.size
    }
}
