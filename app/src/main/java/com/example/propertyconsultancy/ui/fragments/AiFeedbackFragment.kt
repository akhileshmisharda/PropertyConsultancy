package com.example.propertyconsultancy.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.propertyconsultancy.R
import com.google.android.material.button.MaterialButton

class AiFeedbackFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ai_feedback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val rvFeedbacks = view.findViewById<RecyclerView>(R.id.rvUserFeedbacks)
        rvFeedbacks.layoutManager = LinearLayoutManager(requireContext())
        
        val feedbacks = listOf(
            FeedbackItem("Rahul S.", "Safe and peaceful area. Great for families.", "4.5"),
            FeedbackItem("Anita M.", "Water supply is constant, but traffic is a bit high during peak hours.", "4.0"),
            FeedbackItem("Vikram K.", "Excellent nearby schools. Highly recommend for working parents.", "5.0")
        )
        
        rvFeedbacks.adapter = FeedbackAdapter(feedbacks)

        view.findViewById<MaterialButton>(R.id.btnAddFeedback).setOnClickListener {
            // Logic to add user feedback
        }
    }

    data class FeedbackItem(val name: String, val text: String, val rating: String)

    class FeedbackAdapter(private val items: List<FeedbackItem>) : RecyclerView.Adapter<FeedbackAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(android.R.id.text1)
            val tvText: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = "${item.name} - Rating: ${item.rating}★"
            holder.tvText.text = item.text
        }

        override fun getItemCount() = items.size
    }
}
