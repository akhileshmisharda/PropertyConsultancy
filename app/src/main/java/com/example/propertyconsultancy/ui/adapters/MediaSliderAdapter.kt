package com.example.propertyconsultancy.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.example.propertyconsultancy.R

class MediaSliderAdapter(
    private val mediaUrls: List<String>,
    private val onMediaClick: (Int) -> Unit
) : RecyclerView.Adapter<MediaSliderAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivSlideItem)
        val ivVideoIcon: ImageView = view.findViewById(R.id.ivVideoIconItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media_slide, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val url = mediaUrls[position]
        val isVideo = url.endsWith(".mp4") || url.endsWith(".mkv")
        
        val fullUrl = if (!url.startsWith("http")) "http://fabkraft.in/property/$url" else url
        holder.imageView.load(fullUrl)
        holder.ivVideoIcon.visibility = if (isVideo) View.VISIBLE else View.GONE
        
        holder.itemView.setOnClickListener { onMediaClick(position) }
    }

    override fun getItemCount() = mediaUrls.size
}
