package com.example.propertyconsultancy.ui.fragments

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.utils.UrlUtils

class PropertyMediaFragment : Fragment() {

    private lateinit var rvMedia: RecyclerView
    private lateinit var btnAddMedia: View
    private val selectedImages = mutableListOf<Uri>()
    private val selectedVideos = mutableListOf<Uri>()
    private lateinit var adapter: MediaAdapter

    var onMediaChanged: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_property_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvMedia = view.findViewById(R.id.rvMedia)
        btnAddMedia = view.findViewById(R.id.btnAddMedia)

        adapter = MediaAdapter()
        rvMedia.layoutManager = GridLayoutManager(requireContext(), 2)
        rvMedia.adapter = adapter

        btnAddMedia.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(100)) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                val mimeType = requireContext().contentResolver.getType(uri)
                if (mimeType?.startsWith("video") == true) {
                    if (selectedVideos.size < 3) {
                        selectedVideos.add(uri)
                    } else {
                        Toast.makeText(requireContext(), "Max 3 videos allowed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (selectedImages.size < 5) {
                        selectedImages.add(uri)
                    } else {
                        Toast.makeText(requireContext(), "Max 5 images allowed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            adapter.notifyDataSetChanged()
            onMediaChanged?.invoke()
        }
    }

    fun setData(mediaUrls: List<String>?, mediaList: List<com.example.propertyconsultancy.data.dto.PropertyMediaDTO>?) {
        selectedImages.clear()
        selectedVideos.clear()
        
        mediaList?.forEach { media ->
            val url = UrlUtils.getPropertyImageUrl(media.fileUrl) ?: return@forEach
            val uri = Uri.parse(url)
            if (media.mediaType == "video") selectedVideos.add(uri) else selectedImages.add(uri)
        }
        
        if (mediaList.isNullOrEmpty()) {
            mediaUrls?.forEach { url ->
                val finalUrl = UrlUtils.getPropertyImageUrl(url) ?: return@forEach
                val uri = Uri.parse(finalUrl)
                if (url.contains(".mp4") || url.contains(".mkv")) selectedVideos.add(uri) else selectedImages.add(uri)
            }
        }
        adapter.notifyDataSetChanged()
    }

    fun getSelectedImages(): List<Uri> = selectedImages
    fun getSelectedVideos(): List<Uri> = selectedVideos

    private inner class MediaAdapter : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media_upload, parent, false)
            return MediaViewHolder(view)
        }

        override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
            val isVideo = position >= selectedImages.size
            val uri = if (isVideo) selectedVideos[position - selectedImages.size] else selectedImages[position]

            if (isVideo) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(requireContext(), uri)
                    holder.ivThumbnail.setImageBitmap(retriever.getFrameAtTime(1000000))
                    retriever.release()
                } catch (e: Exception) {
                    holder.ivThumbnail.setImageResource(android.R.drawable.ic_menu_slideshow)
                }
                holder.ivVideoBadge.visibility = View.VISIBLE
            } else {
                holder.ivThumbnail.load(uri)
                holder.ivVideoBadge.visibility = View.GONE
            }

            holder.btnDelete.setOnClickListener {
                if (isVideo) selectedVideos.remove(uri) else selectedImages.remove(uri)
                notifyDataSetChanged()
                onMediaChanged?.invoke()
            }
        }

        override fun getItemCount(): Int = selectedImages.size + selectedVideos.size

        inner class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
            val ivVideoBadge: ImageView = view.findViewById(R.id.ivVideoBadge)
            val btnDelete: View = view.findViewById(R.id.btnDelete)
        }
    }
}
