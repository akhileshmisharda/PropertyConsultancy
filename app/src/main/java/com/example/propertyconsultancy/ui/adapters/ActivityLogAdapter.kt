package com.example.propertyconsultancy.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.ActivityLogDTO
import java.text.SimpleDateFormat
import java.util.*

class ActivityLogAdapter(private val logs: List<ActivityLogDTO>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_DATE = 0
    private val VIEW_TYPE_LOG = 1

    private val items = mutableListOf<Any>()

    init {
        groupLogsByDate()
    }

    private fun groupLogsByDate() {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        var lastDate = ""
        logs.forEach { log ->
            val dateStr = dateFormat.format(Date(log.timestamp))
            if (dateStr != lastDate) {
                items.add(dateStr)
                lastDate = dateStr
            }
            items.add(log)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) VIEW_TYPE_DATE else VIEW_TYPE_LOG
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_DATE) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_activity_date_header, parent, false)
            DateViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_activity_log, parent, false)
            LogViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DateViewHolder) {
            holder.tvDate.text = items[position] as String
        } else if (holder is LogViewHolder) {
            val log = items[position] as ActivityLogDTO
            holder.bind(log)
        }
    }

    override fun getItemCount(): Int = items.size

    class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDateHeader)
    }

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivLogIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvLogTitle)
        val tvDetail: TextView = view.findViewById(R.id.tvLogDetail)
        val tvTime: TextView = view.findViewById(R.id.tvLogTime)

        fun bind(log: ActivityLogDTO) {
            tvTitle.text = log.title
            tvDetail.text = log.detail
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            tvTime.text = timeFormat.format(Date(log.timestamp))

            val iconRes = when (log.type) {
                "search" -> R.drawable.ic_search_modern
                "map" -> R.drawable.ic_location_pin
                "view" -> R.drawable.ic_dashboard_modern
                "chat" -> android.R.drawable.stat_notify_chat
                "add" -> android.R.drawable.ic_menu_add
                else -> R.drawable.ic_settings_modern
            }
            ivIcon.setImageResource(iconRes)
        }
    }
}
