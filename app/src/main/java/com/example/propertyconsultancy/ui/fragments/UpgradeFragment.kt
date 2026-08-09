package com.example.propertyconsultancy.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.ProjectDTO

class UpgradeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_upgrade, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val rvProjects = view.findViewById<RecyclerView>(R.id.rvUpcomingProjects)
        rvProjects.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        
        val projects = listOf(
            ProjectDTO(1, "Emerald Heights", "Skyline Builders", "Civil Lines, Nagpur", "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?q=80&w=1000", "Luxury living redefined", "Dec 2026", "₹ 45 L"),
            ProjectDTO(2, "Ocean Breeze", "Coastal Developers", "Worli, Mumbai", "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?q=80&w=1000", "Sea facing apartments", "Jan 2027", "₹ 2.5 Cr"),
            ProjectDTO(3, "Forest Edge", "Green Habitats", "Hinjewadi, Pune", "https://images.unsplash.com/photo-1448630360428-6e23437f71ad?q=80&w=1000", "Live with nature", "Oct 2026", "₹ 65 L")
        )
        
        rvProjects.adapter = ProjectAdapter(projects)
    }

    private class ProjectAdapter(private val items: List<ProjectDTO>) : RecyclerView.Adapter<ProjectAdapter.VH>() {
        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val ivImage: ImageView = view.findViewById(R.id.ivProjectImage)
            val tvTitle: TextView = view.findViewById(R.id.tvProjectTitle)
            val tvDeveloper: TextView = view.findViewById(R.id.tvDeveloperName)
            val tvLocation: TextView = view.findViewById(R.id.tvProjectLocation)
            val tvPrice: TextView = view.findViewById(R.id.tvStartingPrice)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_project_ad, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.ivImage.load(item.imageUrl)
            holder.tvTitle.text = item.title
            holder.tvDeveloper.text = "by ${item.developer}"
            holder.tvLocation.text = item.location
            holder.tvPrice.text = item.priceStarting
        }

        override fun getItemCount() = items.size
    }
}
