package com.example.propertyconsultancy.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.propertyconsultancy.R

import com.example.propertyconsultancy.ui.activities.MainActivity

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.propertyconsultancy.data.local.SessionManager
import com.example.propertyconsultancy.ui.adapters.ActivityLogAdapter
import android.widget.TextView

class ActivityFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_activity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.updateTitle("User Activity")
        
        val sessionManager = SessionManager(requireContext())
        val logs = sessionManager.getActivityLogs()
        
        val rvLogs = view.findViewById<RecyclerView>(R.id.rvActivityLogs)
        val tvNoActivity = view.findViewById<TextView>(R.id.tvNoActivity)
        
        if (logs.isEmpty()) {
            tvNoActivity.visibility = View.VISIBLE
            rvLogs.visibility = View.GONE
        } else {
            tvNoActivity.visibility = View.GONE
            rvLogs.visibility = View.VISIBLE
            rvLogs.layoutManager = LinearLayoutManager(requireContext())
            rvLogs.adapter = ActivityLogAdapter(logs)
        }
    }
}
