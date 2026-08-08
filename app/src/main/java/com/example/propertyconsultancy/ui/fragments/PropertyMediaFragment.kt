package com.example.propertyconsultancy.ui.fragments

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.MediaTagDTO
import com.example.propertyconsultancy.data.dto.PropertyMediaDTO
import com.example.propertyconsultancy.data.remote.RetrofitInstance
import com.example.propertyconsultancy.utils.UrlUtils
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class PropertyMediaFragment : Fragment() {

    private lateinit var rvMediaTags: RecyclerView
    private lateinit var rvTagMedia: RecyclerView
    private lateinit var btnUploadForTag: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var tvSelectedTagName: TextView
    private lateinit var uploadProgress: View

    private var allTags = listOf<MediaTagDTO>()
    private var selectedTag: MediaTagDTO? = null
    private var propertyId: Long? = null
    
    // Map of TagID -> List of Media
    private val tagMediaMap = mutableMapOf<Int, MutableList<PropertyMediaDTO>>()

    private lateinit var tagAdapter: TagAdapter
    private lateinit var mediaAdapter: MediaAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_property_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        rvMediaTags = view.findViewById(R.id.rvMediaTags)
        rvTagMedia = view.findViewById(R.id.rvTagMedia)
        btnUploadForTag = view.findViewById(R.id.btnUploadForTag)
        tvSelectedTagName = view.findViewById(R.id.tvSelectedTagName)
        uploadProgress = view.findViewById(R.id.mediaUploadProgress)

        setupAdapters()
        fetchTags()
    }

    private fun setupAdapters() {
        tagAdapter = TagAdapter { tag ->
            selectedTag = tag
            updateRightSideContent()
        }
        rvMediaTags.layoutManager = LinearLayoutManager(requireContext())
        rvMediaTags.adapter = tagAdapter

        mediaAdapter = MediaAdapter()
        rvTagMedia.layoutManager = GridLayoutManager(requireContext(), 2)
        rvTagMedia.adapter = mediaAdapter

        btnUploadForTag.setOnClickListener {
            if (selectedTag == null) {
                Toast.makeText(requireContext(), "Please select a category first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val request = when (selectedTag?.allowedMediaType) {
                "image" -> ActivityResultContracts.PickVisualMedia.ImageOnly
                "video" -> ActivityResultContracts.PickVisualMedia.VideoOnly
                else -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
            }
            pickMedia.launch(PickVisualMediaRequest(request))
        }
    }

    private fun fetchTags() {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getMediaTags()
                if (response.status == "success") {
                    allTags = response.data
                    tagAdapter.submitList(allTags)
                    if (allTags.isNotEmpty() && selectedTag == null) {
                        selectedTag = allTags[0]
                        updateRightSideContent()
                    }
                }
            } catch (e: Exception) {
                Log.e("Media", "Tag Load Error: ${e.message}")
            }
        }
    }

    private fun updateRightSideContent() {
        val tag = selectedTag ?: return
        tvSelectedTagName.text = tag.tagName
        btnUploadForTag.isEnabled = true
        
        val media = tagMediaMap[tag.tagId] ?: mutableListOf()
        mediaAdapter.submitList(media)
        tagAdapter.notifyDataSetChanged()
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) {
            val pid = propertyId
            if (pid == null) {
                Toast.makeText(requireContext(), "Initializing property... try again in a moment", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            uploadFiles(pid, uris)
        }
    }

    private fun uploadFiles(pid: Long, uris: List<Uri>) {
        val tag = selectedTag ?: return
        uploadProgress.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            uris.forEach { uri ->
                try {
                    val file = com.example.propertyconsultancy.utils.FileUtils.uriToFile(requireContext(), uri) ?: return@forEach
                    val mimeType = requireContext().contentResolver.getType(uri) ?: "image/jpeg"
                    val mediaType = if (mimeType.startsWith("video")) "video" else "image"

                    val filePart = MultipartBody.Part.createFormData(
                        "file", 
                        file.name, 
                        file.asRequestBody(mimeType.toMediaTypeOrNull())
                    )

                    Log.d("php_debug", "Uploading file to PID: $pid, Tag: ${tag.tagId}, Type: $mediaType")
                    val response = RetrofitInstance.api.uploadPropertyMedia(
                        pid.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                        tag.tagId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                        mediaType.toRequestBody("text/plain".toMediaTypeOrNull()),
                        filePart
                    )

                    Log.d("php_debug", "Upload Response: ${response.status}, Msg: ${response.message}")
                    if (response.status == "success") {
                        val newMedia = PropertyMediaDTO(
                            mediaId = 0,
                            propertyId = pid,
                            imageTagId = tag.tagId,
                            mediaType = mediaType,
                            fileUrl = response.message ?: "", 
                            isPrimary = 0,
                            displayOrder = 0
                        )
                        if (!tagMediaMap.containsKey(tag.tagId)) tagMediaMap[tag.tagId] = mutableListOf()
                        tagMediaMap[tag.tagId]?.add(newMedia)
                        Log.d("Media", "Uploaded to ${tag.tagName}: ${response.message}")
                    } else {
                        Toast.makeText(requireContext(), "Upload failed: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("Upload", "Error: ${e.message}")
                }
            }
            uploadProgress.visibility = View.GONE
            updateRightSideContent()
        }
    }

    fun setData(pid: Long?, existingMedia: List<PropertyMediaDTO>?) {
        this.propertyId = pid
        tagMediaMap.clear()
        existingMedia?.forEach { media ->
            val tid = media.imageTagId ?: 0
            if (!tagMediaMap.containsKey(tid)) tagMediaMap[tid] = mutableListOf()
            tagMediaMap[tid]?.add(media)
        }
        updateRightSideContent()
        fetchTags() // Refresh tags to update counts
    }

    fun getSelectedImages(): List<Uri> = emptyList() // Now handled by server
    fun getSelectedVideos(): List<Uri> = emptyList()

    private inner class TagAdapter(private val onClick: (MediaTagDTO) -> Unit) : RecyclerView.Adapter<TagAdapter.TagViewHolder>() {
        private var list = listOf<MediaTagDTO>()
        fun submitList(newList: List<MediaTagDTO>) { list = newList; notifyDataSetChanged() }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
            return TagViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_media_tag, parent, false))
        }

        override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
            val tag = list[position]
            holder.tvName.text = tag.tagName
            val count = tagMediaMap[tag.tagId]?.size ?: 0
            holder.tvCount.text = if (count > 0) "($count)" else ""
            
            val isSelected = selectedTag?.tagId == tag.tagId
            
            if (isSelected) {
                holder.itemView.setBackgroundColor(Color.WHITE)
                holder.tvName.setTextColor(Color.parseColor("#1A237E"))
                holder.tvName.alpha = 1.0f
            } else {
                holder.itemView.setBackgroundColor(Color.TRANSPARENT)
                holder.tvName.setTextColor(Color.parseColor("#757575"))
                holder.tvName.alpha = 0.8f
            }

            holder.itemView.setOnClickListener { onClick(tag) }
        }

        override fun getItemCount() = list.size

        inner class TagViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvTagName)
            val tvCount: TextView = v.findViewById(R.id.tvTagCount)
        }
    }

    private inner class MediaAdapter : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {
        private var list = listOf<PropertyMediaDTO>()
        fun submitList(newList: List<PropertyMediaDTO>) { list = newList; notifyDataSetChanged() }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
            return MediaViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_media_upload, parent, false))
        }

        override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
            val media = list[position]
            val url = UrlUtils.getPropertyImageUrl(media.fileUrl)
            
            if (media.mediaType == "video") {
                holder.ivThumbnail.setImageResource(android.R.drawable.ic_menu_slideshow)
                holder.ivVideoBadge.visibility = View.VISIBLE
            } else {
                holder.ivThumbnail.load(url)
                holder.ivVideoBadge.visibility = View.GONE
            }

            holder.btnDelete.setOnClickListener {
                // Implement server-side delete if needed
                tagMediaMap[media.imageTagId ?: 0]?.remove(media)
                notifyDataSetChanged()
                tagAdapter.notifyDataSetChanged()
            }
        }

        override fun getItemCount() = list.size

        inner class MediaViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val ivThumbnail: ImageView = v.findViewById(R.id.ivThumbnail)
            val ivVideoBadge: ImageView = v.findViewById(R.id.ivVideoBadge)
            val btnDelete: View = v.findViewById(R.id.btnDelete)
        }
    }
}
