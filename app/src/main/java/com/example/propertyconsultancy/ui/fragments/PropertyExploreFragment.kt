package com.example.propertyconsultancy.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import coil3.load
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.PropertyDTO
import com.example.propertyconsultancy.data.dto.PropertyInteractionDTO
import com.example.propertyconsultancy.ui.activities.MainActivity
import com.google.android.material.button.MaterialButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PropertyExploreFragment : Fragment() {

    private var property: PropertyDTO? = null
    private var isFavoriteLocal = false
    private var preferredVisitDate: String? = null
    private var existingNotes: String? = null
    
    private lateinit var btnEditVisit: com.google.android.material.button.MaterialButton
    private lateinit var switchFavorite: com.google.android.material.materialswitch.MaterialSwitch
    private lateinit var ivFavoriteStatus: ImageView
    private lateinit var tvVisitCurrent: TextView
    private lateinit var etExploreRemarks: android.widget.EditText
    private lateinit var btnSaveRemarks: View
    private lateinit var exploreProgress: com.google.android.material.progressindicator.LinearProgressIndicator

    private lateinit var sessionManager: com.example.propertyconsultancy.data.local.SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        property = arguments?.getSerializable("property") as? PropertyDTO
        
        val transition = android.transition.TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
            .setDuration(500)
            
        sharedElementEnterTransition = transition
        sharedElementReturnTransition = transition
        
        parentFragmentManager.setFragmentResultListener("media_position", this) { _, bundle ->
            val position = bundle.getInt("current_position")
            view?.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.vpExploreMedia)?.setCurrentItem(position, false)
        }

        postponeEnterTransition()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_property_explore, container, false)
        
        val prefixes = listOf("IMAGE", "TITLE", "PRICE", "LOCATION", "BHK", "AREA", "FACING", "ROADSIZE", "FURNISHED", "BATH", "TYPE", "INTERESTED", "AMENITIES")
        val viewIds = listOf(R.id.vpExploreMedia, R.id.tvExploreTitle, R.id.tvExplorePrice, R.id.tvExploreLocation, R.id.tvExploreBhk, R.id.tvExploreArea, R.id.tvExploreFacing, R.id.tvExploreRoadSize, R.id.tvExploreFurnished, R.id.tvExploreBath, R.id.tvExplorePropertyType, R.id.tvExploreInterested, R.id.tvExploreAmenities)
        
        prefixes.forEachIndexed { index, prefix ->
            val transitionName = arguments?.getString("TRANSITION_PROPERTY_${prefix}_NAME")
            if (transitionName != null) {
                view.findViewById<View>(viewIds[index])?.transitionName = transitionName
            }
        }
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = com.example.propertyconsultancy.data.local.SessionManager(requireContext())
        (activity as? MainActivity)?.updateTitle("Property Explore")
        
        val property = property ?: return
        sessionManager.addActivityLog("Property Detail", "Viewed property: ${property.title}", "view")
        
        initViews(view)
        bindPropertyData(property)
        fetchInitialInteraction(property.propertyId ?: 0)
    }

    private fun initViews(view: View) {
        // Interaction Center
        switchFavorite = view.findViewById(R.id.switchFavorite)
        ivFavoriteStatus = view.findViewById(R.id.ivFavoriteStatus)
        btnEditVisit = view.findViewById(R.id.btnEditVisit)
        tvVisitCurrent = view.findViewById(R.id.tvVisitCurrent)
        etExploreRemarks = view.findViewById(R.id.etExploreRemarks)
        btnSaveRemarks = view.findViewById(R.id.btnSaveRemarks)
        exploreProgress = view.findViewById(R.id.exploreProgress)

        switchFavorite.setOnCheckedChangeListener { _, isChecked ->
            val user = sessionManager.getUser() ?: return@setOnCheckedChangeListener
            if (isChecked != isFavoriteLocal) {
                isFavoriteLocal = isChecked
                updateFavoriteUI()
                toggleFavoriteOnServer(user.userId, property?.propertyId ?: 0, isFavoriteLocal)
            }
        }

        btnEditVisit.setOnClickListener {
            showVisitRequestDialog { date, remarks ->
                val user = sessionManager.getUser() ?: return@showVisitRequestDialog
                preferredVisitDate = date
                updateVisitUI(date)
                submitVisitToServer(user.userId, property?.propertyId ?: 0, date, remarks)
            }
        }

        btnSaveRemarks.setOnClickListener {
            val user = sessionManager.getUser() ?: return@setOnClickListener
            val remarks = etExploreRemarks.text.toString().trim()
            submitVisitToServer(user.userId, property?.propertyId ?: 0, preferredVisitDate ?: "", remarks)
        }
    }

    private fun bindPropertyData(property: PropertyDTO) {
        val view = requireView()
        val vpMedia = view.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.vpExploreMedia)
        val tabLayout = view.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabMediaDots)
        
        val tvTitle = view.findViewById<TextView>(R.id.tvExploreTitle)
        val tvPrice = view.findViewById<TextView>(R.id.tvExplorePrice)
        val tvLocation = view.findViewById<TextView>(R.id.tvExploreLocation)
        val tvBhk = view.findViewById<TextView>(R.id.tvExploreBhk)
        val tvArea = view.findViewById<TextView>(R.id.tvExploreArea)
        val tvFacing = view.findViewById<TextView>(R.id.tvExploreFacing)
        val tvRoadSize = view.findViewById<TextView>(R.id.tvExploreRoadSize)
        val tvFurnished = view.findViewById<TextView>(R.id.tvExploreFurnished)
        val tvBath = view.findViewById<TextView>(R.id.tvExploreBath)
        val tvPropertyType = view.findViewById<TextView>(R.id.tvExplorePropertyType)
        val tvAmenities = view.findViewById<TextView>(R.id.tvExploreAmenities)
        val tvInterested = view.findViewById<TextView>(R.id.tvExploreInterested)
        val tvFloor = view.findViewById<TextView>(R.id.tvExploreFloor)
        val tvDescription = view.findViewById<TextView>(R.id.tvExploreDescription)

        tvTitle.text = property.title?.uppercase()
        val formatter = java.text.DecimalFormat("#,###")
        tvPrice.text = "₹ ${formatter.format(property.pricePerMonth ?: 0.0)}/mo"
        tvLocation.text = if (property.state.isNullOrEmpty()) "${property.city}" else "${property.city}, ${property.state}"
        
        tvBhk.text = "${property.bedrooms} BHK"
        tvArea.text = "${property.areaSqft} Sqft"
        tvBath.text = "${property.bathrooms?.toInt() ?: 0} BathRoom"

        val categories = com.example.propertyconsultancy.data.cache.CategoryCache.getCategories(requireContext())
        fun getOptionName(ids: List<Int>?, group: String): String {
            if (ids.isNullOrEmpty()) return "N/A"
            val cleanGroup = group.replace(" ", "").lowercase()
            val options = categories?.find {
                val name = it.name.replace(" ", "").lowercase()
                name.contains(cleanGroup) || cleanGroup.contains(name)
            }?.options
            val found = options?.find { it.categoryId == ids.first() }?.option
            return found ?: categories?.flatMap { it.options }?.find { it.categoryId == ids.first() }?.option ?: "ID: ${ids.first()}"
        }

        tvFacing.text = "Facing: ${getOptionName(property.facingId?.let { listOf(it) }, "Facing")}"
        tvRoadSize.text = "Road: ${getOptionName(property.roadSizeId?.let { listOf(it) }, "Road Size")}"
        tvPropertyType.text = getOptionName(property.proTypeId?.let { listOf(it) }, "Property Type")
        tvFloor.text = "Floor: ${getOptionName(property.floorId?.let { listOf(it) }, "Floor")}"

        val isFurnished = property.description?.contains("furnished", true) == true || 
                          property.amenities?.any { it.name.contains("furnished", true) } == true
        tvFurnished.text = if (isFurnished) "Furnished" else "Unfurnished"
        tvAmenities.text = "{ ${property.amenityCount ?: 0} Amenities }"
        tvInterested.text = "{ ${(5..25).random()} Interested }"
        tvDescription.text = property.description ?: "No description available."

        val mediaUrls = (property.media?.map { it.fileUrl } ?: emptyList()) + (property.mediaUrls ?: emptyList()).distinct()
        vpMedia.adapter = com.example.propertyconsultancy.ui.adapters.MediaSliderAdapter(mediaUrls) { position ->
            val fullScreen = FullScreenMediaFragment()
            val args = Bundle()
            args.putStringArrayList("URLS", ArrayList(mediaUrls))
            args.putInt("START_INDEX", position)
            args.putString("TRANSITION_NAME", vpMedia.transitionName)
            fullScreen.arguments = args
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .addSharedElement(vpMedia, vpMedia.transitionName)
                .replace(R.id.nav_host_fragment, fullScreen, "FullScreenMedia")
                .addToBackStack(null)
                .commit()
        }
        
        view.viewTreeObserver.addOnPreDrawListener {
            startPostponedEnterTransition()
            true
        }

        if (mediaUrls.size > 1) {
            com.google.android.material.tabs.TabLayoutMediator(tabLayout, vpMedia) { _, _ -> }.attach()
        } else {
            tabLayout.visibility = View.GONE
        }
    }

    private fun fetchInitialInteraction(propertyId: Long) {
        val user = sessionManager.getUser() ?: return
        exploreProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = com.example.propertyconsultancy.data.remote.RetrofitInstance.api.getInteraction(user.userId, propertyId)
                if (response.status == "success" && response.data != null) {
                    val interaction = response.data
                    isFavoriteLocal = interaction.isFavorite == 1
                    preferredVisitDate = interaction.preferredVisitDate
                    existingNotes = interaction.notes
                    
                    updateFavoriteUI()
                    if (!preferredVisitDate.isNullOrEmpty()) {
                        updateVisitUI(preferredVisitDate!!)
                    }
                    etExploreRemarks.setText(existingNotes ?: "")
                }
            } catch (e: Exception) {
                Log.e("[Interaction]", "Fetch Error: ${e.message}")
            } finally {
                exploreProgress.visibility = View.GONE
            }
        }
    }

    private fun updateVisitUI(dateTime: String) {
        val status = getVisitStatus(dateTime)
        tvVisitCurrent.text = "Visit $status: $dateTime"
        if (status == "Passed") {
            tvVisitCurrent.setTextColor(android.graphics.Color.RED)
        } else {
            tvVisitCurrent.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.theme1_primary))
        }
        btnEditVisit.text = "Change"
    }

    private fun getVisitStatus(dateTimeStr: String): String {
        return try {
            val dateOnly = if (dateTimeStr.contains(" ")) dateTimeStr.substringBefore(" ") else dateTimeStr
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val visitDate = sdf.parse(dateOnly)
            val today = sdf.parse(sdf.format(Date()))
            
            when {
                visitDate == null -> "Requested"
                visitDate.before(today) -> "Passed"
                visitDate == today -> "Due Today"
                else -> "Scheduled"
            }
        } catch (e: Exception) {
            "Requested"
        }
    }

    private fun showVisitRequestDialog(onConfirm: (String, String) -> Unit) {
        val builder = android.app.AlertDialog.Builder(requireContext())
        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val p = (20 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }

        val tvTitle = TextView(requireContext()).apply {
            text = "Request a Property Visit"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, (16 * resources.displayMetrics.density).toInt())
            setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.theme1_text))
        }
        container.addView(tvTitle)

        val layoutDate = com.google.android.material.textfield.TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox).apply {
            hint = "Select Visit Date"
            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
            setPadding(0, 0, 0, (12 * resources.displayMetrics.density).toInt())
        }
        val etDate = com.google.android.material.textfield.TextInputEditText(layoutDate.context).apply {
            isFocusable = false; isClickable = true
            setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_my_calendar, 0, 0, 0)
            compoundDrawablePadding = 20
        }
        layoutDate.addView(etDate)
        container.addView(layoutDate)

        val layoutTime = com.google.android.material.textfield.TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox).apply {
            hint = "Select Preferred Time"
            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
            setPadding(0, 0, 0, (12 * resources.displayMetrics.density).toInt())
        }
        val etTime = com.google.android.material.textfield.TextInputEditText(layoutTime.context).apply {
            isFocusable = false; isClickable = true
            setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_recent_history, 0, 0, 0)
            compoundDrawablePadding = 20
        }
        layoutTime.addView(etTime)
        container.addView(layoutTime)

        builder.setView(container)
        
        var selectedDate = ""
        var selectedTime = ""

        preferredVisitDate?.let {
            if (it.contains(" ")) {
                selectedDate = it.substringBefore(" "); selectedTime = it.substringAfter(" ")
                etDate.setText(selectedDate); etTime.setText(selectedTime)
            } else {
                selectedDate = it; etDate.setText(selectedDate)
            }
        }

        etDate.setOnClickListener {
            showDatePicker { date -> selectedDate = date; etDate.setText(date) }
        }

        etTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            android.app.TimePickerDialog(requireContext(), { _, hour, minute ->
                selectedTime = String.format(Locale.US, "%02d:%02d", hour, minute)
                etTime.setText(selectedTime)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        builder.setPositiveButton("Set Time") { _, _ ->
            if (selectedDate.isNotEmpty()) {
                val fullDateTime = if (selectedTime.isNotEmpty()) "$selectedDate $selectedTime" else selectedDate
                onConfirm(fullDateTime, etExploreRemarks.text.toString().trim())
            } else {
                Toast.makeText(requireContext(), "Please select a date", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun toggleFavoriteOnServer(userId: Long, propertyId: Long, isFavorite: Boolean) {
        exploreProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val request = PropertyInteractionDTO(
                    customerId = userId,
                    propertyId = propertyId,
                    isFavorite = if (isFavorite) 1 else 0
                )
                com.example.propertyconsultancy.data.remote.RetrofitInstance.api.submitFavorite(request)
            } catch (e: Exception) {
                Log.e("[Favorite]", "Error: ${e.message}")
            } finally {
                exploreProgress.visibility = View.GONE
            }
        }
    }

    private fun submitVisitToServer(userId: Long, propertyId: Long, date: String, remarks: String) {
        exploreProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val request = PropertyInteractionDTO(
                    customerId = userId,
                    propertyId = propertyId,
                    isInterestedInVisit = if (date.isNotEmpty()) 1 else 0,
                    preferredVisitDate = date.ifEmpty { null },
                    notes = remarks
                )
                val response = com.example.propertyconsultancy.data.remote.RetrofitInstance.api.submitVisitRequest(request)
                if (response.status == "success") {
                    existingNotes = remarks
                    Toast.makeText(requireContext(), "Interaction Saved", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("[Visit]", "Error: ${e.message}")
            } finally {
                exploreProgress.visibility = View.GONE
            }
        }
    }

    private fun updateFavoriteUI() {
        switchFavorite.isChecked = isFavoriteLocal
        if (isFavoriteLocal) {
            ivFavoriteStatus.setImageResource(R.drawable.ic_favorite_filled)
            ivFavoriteStatus.setColorFilter(android.graphics.Color.parseColor("#E53935"))
        } else {
            ivFavoriteStatus.setImageResource(R.drawable.ic_favorite_border)
            ivFavoriteStatus.setColorFilter(android.graphics.Color.GRAY)
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePicker = android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                onDateSelected(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }
}
