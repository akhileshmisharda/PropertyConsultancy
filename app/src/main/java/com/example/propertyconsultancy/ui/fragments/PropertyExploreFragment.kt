package com.example.propertyconsultancy.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.widget.*
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import coil3.load
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.PropertyDTO
import com.example.propertyconsultancy.data.dto.PropertyInteractionDTO
import com.example.propertyconsultancy.ui.activities.MainActivity
import com.google.android.material.button.MaterialButton
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.example.propertyconsultancy.ui.viewmodels.SearchViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PropertyExploreFragment : Fragment(), OnMapReadyCallback {

    private var property: PropertyDTO? = null
    private var isFavoriteLocal = false
    private var preferredVisitDate: String? = null
    private var existingNotes: String? = null
    
    private var googleMap: GoogleMap? = null

    private lateinit var btnEditVisit: com.google.android.material.button.MaterialButton
    private lateinit var switchFavorite: com.google.android.material.materialswitch.MaterialSwitch
    private lateinit var ivFavoriteStatus: ImageView
    private lateinit var tvVisitCurrent: TextView
    private lateinit var etExploreRemarks: android.widget.EditText
    private lateinit var btnSaveRemarks: View
    private lateinit var btnAiMap: View
    private lateinit var btnAiFeedback: View
    private lateinit var ivHintExploreAiMap: ImageView
    private lateinit var ivHintExploreAiFeedback: ImageView
    private lateinit var exploreProgress: com.google.android.material.progressindicator.LinearProgressIndicator
    private lateinit var llExploreAmenities: android.widget.LinearLayout
    private lateinit var tvAmenitiesSectionTitle: TextView

    private lateinit var viewModel: SearchViewModel
    private lateinit var cardExecutive: View
    private lateinit var tvExecutiveName: TextView
    private lateinit var tvExecutiveMobile: TextView
    private lateinit var ivExecutiveImage: ImageView
    private lateinit var btnCallExecutive: com.google.android.material.button.MaterialButton

    private lateinit var sessionManager: com.example.propertyconsultancy.data.local.SessionManager
    private var allAmenities: List<com.example.propertyconsultancy.data.dto.AmenityDTO>? = null

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
        
        val prefixes = listOf("PROPERTY_IMAGE", "PROPERTY_TITLE", "PROPERTY_PRICE", "PROPERTY_LOCATION", "PROPERTY_BHK", "PROPERTY_AREA", "PROPERTY_FACING", "PROPERTY_ROADSIZE", "PROPERTY_FURNISHED", "PROPERTY_BATH", "PROPERTY_TYPE", "PROPERTY_INTERESTED", "PROPERTY_AMENITIES", "PROPERTY_FAVORITE")
        val viewIds = listOf(R.id.vpExploreMedia, R.id.tvExploreTitle, R.id.tvExplorePrice, R.id.tvExploreLocation, R.id.tvExploreBhk, R.id.tvExploreArea, R.id.tvExploreFacing, R.id.tvExploreRoadSize, R.id.tvExploreFurnished, R.id.tvExploreBath, R.id.tvExplorePropertyType, R.id.tvExploreInterested, R.id.tvAmenitiesSectionTitle, R.id.ivFavoriteStatus)
        
        prefixes.forEachIndexed { index, prefix ->
            val transitionName = arguments?.getString("TRANSITION_${prefix}_NAME")
            if (transitionName != null) {
                view.findViewById<View>(viewIds[index])?.transitionName = transitionName
            }
        }
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = com.example.propertyconsultancy.data.local.SessionManager(requireContext())
        viewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]
        (activity as? MainActivity)?.updateTitle("Property Explore")
        
        val property = property ?: return
        sessionManager.addActivityLog("Property Detail", "Viewed property: ${property.title}", "view")
        
        initViews(view)
        
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapExplore) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        fetchAllAmenities() 
        refreshPropertyData() 
        fetchInitialInteraction(property.propertyId ?: 0)
        
        if (sessionManager.isHintsEnabled()) {
            view.post { showAllStickyHints() }
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.shouldRefresh) {
            refreshPropertyData()
        }
    }

    private fun refreshPropertyData() {
        val pid = property?.propertyId ?: return
        val user = sessionManager.getUser()
        exploreProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = com.example.propertyconsultancy.data.remote.RetrofitInstance.api.getPropertyDetail(pid, user?.userId)
                if (response.status == "success" && !response.data.isNullOrEmpty()) {
                    val updated = response.data[0]
                    property = updated
                    bindPropertyData(updated)
                    updateMapLocation()
                }
            } catch (e: Exception) {
                Log.e("[Explore]", "Refresh Error: ${e.message}")
            } finally {
                exploreProgress.visibility = View.GONE
            }
        }
    }

    private fun initViews(view: View) {
        switchFavorite = view.findViewById(R.id.switchFavorite)
        ivFavoriteStatus = view.findViewById(R.id.ivFavoriteStatus)
        btnEditVisit = view.findViewById(R.id.btnEditVisit)
        tvVisitCurrent = view.findViewById(R.id.tvVisitCurrent)
        etExploreRemarks = view.findViewById(R.id.etExploreRemarks)
        btnSaveRemarks = view.findViewById(R.id.btnSaveRemarks)
        exploreProgress = view.findViewById(R.id.exploreProgress)
        llExploreAmenities = view.findViewById(R.id.llExploreAmenities)
        tvAmenitiesSectionTitle = view.findViewById(R.id.tvAmenitiesSectionTitle)
        btnAiMap = view.findViewById(R.id.btnAiMap)
        btnAiFeedback = view.findViewById(R.id.btnAiFeedback)
        ivHintExploreAiMap = view.findViewById(R.id.ivHintExploreAiMap)
        ivHintExploreAiFeedback = view.findViewById(R.id.ivHintExploreAiFeedback)
        
        cardExecutive = view.findViewById(R.id.cardExecutive)
        tvExecutiveName = view.findViewById(R.id.tvExecutiveName)
        tvExecutiveMobile = view.findViewById(R.id.tvExecutiveMobile)
        ivExecutiveImage = view.findViewById(R.id.ivExecutiveImage)
        btnCallExecutive = view.findViewById(R.id.btnCallExecutive)

        btnAiMap.setOnClickListener {
            property?.let { (activity as? MainActivity)?.openAiMap(it) }
        }

        btnAiFeedback.setOnClickListener {
            property?.let { (activity as? MainActivity)?.openAiFeedback(it) }
        }

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

    private fun fetchAllAmenities() {
        lifecycleScope.launch {
            try {
                val response = com.example.propertyconsultancy.data.remote.RetrofitInstance.api.getAmenities()
                if (response.status == "success") {
                    allAmenities = response.data
                    property?.amenityIds?.let { loadAmenityChips(it) }
                }
            } catch (e: Exception) {}
        }
    }

    private fun loadAmenityChips(ids: List<Int>?) {
        llExploreAmenities.removeAllViews()
        llExploreAmenities.background = null 
        llExploreAmenities.setPadding(0, 0, 0, 0)
        
        if (ids.isNullOrEmpty()) return

        val context = requireContext()
        val grouped = allAmenities?.filter { ids.contains(it.amenityId) }?.groupBy { it.category ?: "General" } ?: emptyMap()
        val density = resources.displayMetrics.density
        
        fun String.toProperCase() = this.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        val rootTrunk = LinearLayout(context).apply { 
            orientation = LinearLayout.VERTICAL 
            setPadding(0, 0, 0, 0)
        }

        val entries = grouped.entries.toList()
        entries.forEachIndexed { groupIndex, entry ->
            val category = entry.key
            val amenities = entry.value
            val properCategory = category.toProperCase()

            // 1. Category Row - NO PADDING ON ROW
            val categoryRow = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            // Main Trunk Segment - Covers 100% of height
            val isAbsoluteLastEntry = groupIndex == entries.size - 1 && amenities.isEmpty()
            val trunkHeight = if (isAbsoluteLastEntry) (24 * density).toInt() else ViewGroup.LayoutParams.MATCH_PARENT
            
            categoryRow.addView(View(context).apply {
                layoutParams = FrameLayout.LayoutParams((2.5f * density).toInt(), trunkHeight).apply {
                    leftMargin = (20 * density).toInt()
                }
                setBackgroundColor(Color.RED)
            })

            // Horizontal Branch for Category
            categoryRow.addView(View(context).apply {
                layoutParams = FrameLayout.LayoutParams((20 * density).toInt(), (2 * density).toInt()).apply {
                    leftMargin = (20 * density).toInt()
                    topMargin = (32 * density).toInt() // Exact center of (Pill 40dp + margins)
                }
                setBackgroundColor(Color.RED)
            })

            // Category Pill - Margin here instead of Row padding
            val tvCat = TextView(context).apply {
                text = properCategory
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding((16 * density).toInt(), (8 * density).toInt(), (16 * density).toInt(), (8 * density).toInt())
                background = GradientDrawable().apply {
                    setColor(Color.RED)
                    cornerRadius = 25 * density
                }
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    leftMargin = (40 * density).toInt()
                    topMargin = (12 * density).toInt()
                    bottomMargin = (12 * density).toInt()
                }
            }
            categoryRow.addView(tvCat)
            rootTrunk.addView(categoryRow)

            // 2. Amenities List
            amenities.forEachIndexed { index, amenity ->
                val amenityRow = FrameLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }

                // Main Trunk Continuation - Covers 100% height
                val isFinalItem = groupIndex == entries.size - 1 && index == amenities.size - 1
                val mainTrunkH = if (isFinalItem) (24 * density).toInt() else ViewGroup.LayoutParams.MATCH_PARENT
                
                amenityRow.addView(View(context).apply {
                    layoutParams = FrameLayout.LayoutParams((2.5f * density).toInt(), mainTrunkH).apply {
                        leftMargin = (20 * density).toInt()
                    }
                    setBackgroundColor(Color.RED)
                })

                // Sub-Trunk (Transparent) - Covers 100% height
                val subTrunkH = if (index == amenities.size - 1) (24 * density).toInt() else ViewGroup.LayoutParams.MATCH_PARENT
                amenityRow.addView(View(context).apply {
                    layoutParams = FrameLayout.LayoutParams((1.5f * density).toInt(), subTrunkH).apply {
                        leftMargin = (55 * density).toInt()
                    }
                    setBackgroundColor(Color.RED)
                    alpha = 0.5f
                })

                // Sub-Horizontal Branch
                amenityRow.addView(View(context).apply {
                    layoutParams = FrameLayout.LayoutParams((20 * density).toInt(), (1.5f * density).toInt()).apply {
                        leftMargin = (55 * density).toInt()
                        topMargin = (24 * density).toInt() // Center of Amenity Box
                    }
                    setBackgroundColor(Color.RED)
                    alpha = 0.5f
                })

                // Amenity Label Box - Margins here
                val tvName = TextView(context).apply {
                    text = amenity.name
                    textSize = 14f
                    setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.theme1_text))
                    setPadding((16 * density).toInt(), (10 * density).toInt(), (16 * density).toInt(), (10 * density).toInt())
                    background = GradientDrawable().apply {
                        setColor(Color.WHITE)
                        setStroke((1 * density).toInt(), Color.parseColor("#FFCDD2"))
                        cornerRadius = 15 * density
                    }
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        leftMargin = (75 * density).toInt()
                        topMargin = (6 * density).toInt()
                        bottomMargin = (6 * density).toInt()
                    }
                }
                amenityRow.addView(tvName)
                rootTrunk.addView(amenityRow)
            }
        }
        llExploreAmenities.addView(rootTrunk)
    }

    private fun bindPropertyData(property: PropertyDTO) {
        val view = requireView()
        val vpMedia = view.findViewById<ViewPager2>(R.id.vpExploreMedia)
        val tabLayout = view.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabMediaDots)
        
        val tvTitle = view.findViewById<TextView>(R.id.tvExploreTitle)
        val tvPrice = view.findViewById<TextView>(R.id.tvExplorePrice)
        val tvDeposit = view.findViewById<TextView>(R.id.tvExploreDeposit)
        val tvMaintenance = view.findViewById<TextView>(R.id.tvExploreMaintenance)
        
        val tvLocation = view.findViewById<TextView>(R.id.tvExploreLocation)
        val tvBhk = view.findViewById<TextView>(R.id.tvExploreBhk)
        val tvArea = view.findViewById<TextView>(R.id.tvExploreArea)
        val tvFacing = view.findViewById<TextView>(R.id.tvExploreFacing)
        val tvRoadSize = view.findViewById<TextView>(R.id.tvExploreRoadSize)
        val tvFurnished = view.findViewById<TextView>(R.id.tvExploreFurnished)
        val tvBath = view.findViewById<TextView>(R.id.tvExploreBath)
        val tvPropertyType = view.findViewById<TextView>(R.id.tvExplorePropertyType)
        val tvInterested = view.findViewById<TextView>(R.id.tvExploreInterested)
        val tvFloor = view.findViewById<TextView>(R.id.tvExploreFloor)
        val tvDescription = view.findViewById<TextView>(R.id.tvExploreDescription)

        tvTitle.text = property.title?.uppercase()
        val formatter = java.text.DecimalFormat("#,###")
        tvPrice.text = "Rs. ${formatter.format(property.pricePerMonth ?: 0.0)} / Month"
        
        if (property.securityDeposit != null && property.securityDeposit > 0) {
            tvDeposit.visibility = View.VISIBLE
            tvDeposit.text = "Security Deposit: Rs. ${formatter.format(property.securityDeposit)}"
        } else {
            tvDeposit.visibility = View.GONE
        }

        if (property.cleaningFee != null && property.cleaningFee > 0) {
            tvMaintenance.visibility = View.VISIBLE
            tvMaintenance.text = "+ Rs. ${formatter.format(property.cleaningFee)} Maintenance/Mo"
        } else {
            tvMaintenance.visibility = View.GONE
        }

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
        tvFurnished.text = property.furnishing ?: "Unfurnished"
        
        val count = property.amenityCount ?: 0
        tvAmenitiesSectionTitle.text = "Amenities [$count] :-"
        
        loadAmenityChips(property.amenityIds)
        
        tvInterested.text = "{ ${(5..25).random()} Interested }"
        tvDescription.text = property.description ?: "No description available."

        if (property.executiveId != null && !property.executiveName.isNullOrEmpty()) {
            cardExecutive.visibility = View.VISIBLE
            tvExecutiveName.text = property.executiveName
            tvExecutiveMobile.text = property.executiveMobile ?: "N/A"
            val imageUrl = com.example.propertyconsultancy.utils.UrlUtils.getPropertyImageUrl(property.executiveImage)
            if (imageUrl != null) {
                ivExecutiveImage.load(imageUrl)
            } else {
                ivExecutiveImage.setImageResource(R.drawable.ic_profile_modern)
            }
            btnCallExecutive.setOnClickListener {
                property.executiveMobile?.let { mobile ->
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                        intent.data = android.net.Uri.parse("tel:$mobile")
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Unable to make call", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            cardExecutive.visibility = View.GONE
        }

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
        } catch (e: Exception) { "Requested" }
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
            } else { selectedDate = it; etDate.setText(selectedDate) }
        }
        etDate.setOnClickListener { showDatePicker { date -> selectedDate = date; etDate.setText(date) } }
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
            } else { Toast.makeText(requireContext(), "Please select a date", Toast.LENGTH_SHORT).show() }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun toggleFavoriteOnServer(userId: Long, propertyId: Long, isFavorite: Boolean) {
        exploreProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val request = PropertyInteractionDTO(customerId = userId, propertyId = propertyId, isFavorite = if (isFavorite) 1 else 0)
                com.example.propertyconsultancy.data.remote.RetrofitInstance.api.submitFavorite(request)
            } catch (e: Exception) { Log.e("[Favorite]", "Error: ${e.message}")
            } finally { exploreProgress.visibility = View.GONE }
        }
    }

    private fun submitVisitToServer(userId: Long, propertyId: Long, date: String, remarks: String) {
        exploreProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val request = PropertyInteractionDTO(customerId = userId, propertyId = propertyId, isInterestedInVisit = if (date.isNotEmpty()) 1 else 0, preferredVisitDate = date.ifEmpty { null }, notes = remarks)
                val response = com.example.propertyconsultancy.data.remote.RetrofitInstance.api.submitVisitRequest(request)
                if (response.status == "success") {
                    existingNotes = remarks
                    Toast.makeText(requireContext(), "Interaction Saved", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { Log.e("[Visit]", "Error: ${e.message}")
            } finally { exploreProgress.visibility = View.GONE }
        }
    }

    private fun updateFavoriteUI() {
        switchFavorite.isChecked = isFavoriteLocal
        if (isFavoriteLocal) {
            ivFavoriteStatus.setImageResource(R.drawable.ic_favorite_filled)
            ivFavoriteStatus.setColorFilter(android.graphics.Color.parseColor("#2196F3"))
        } else {
            ivFavoriteStatus.setImageResource(R.drawable.ic_favorite_border)
            ivFavoriteStatus.setColorFilter(android.graphics.Color.GRAY)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        updateMapLocation()
    }

    private fun updateMapLocation() {
        val map = googleMap ?: return
        val prop = property ?: return
        val lat = prop.latitude ?: return
        val lng = prop.longitude ?: return
        val pos = LatLng(lat, lng)
        map.clear()
        map.addMarker(MarkerOptions().position(pos).title(prop.title))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePicker = android.app.DatePickerDialog(requireContext(), { _, year, month, day ->
            val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
            onDateSelected(formattedDate)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }

    fun showAllStickyHints() {
        showStickyHint(btnAiMap, ivHintExploreAiMap, "AI Proximity Map", true)
        showStickyHint(btnAiFeedback, ivHintExploreAiFeedback, "AI Area Insights", true)
    }

    fun hideAllStickyHints() {
        ivHintExploreAiMap.visibility = View.GONE
        ivHintExploreAiFeedback.visibility = View.GONE
    }

    private fun showStickyHint(target: View, hintView: ImageView, message: String, isAbove: Boolean) {
        val width = 500
        val height = 200
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.color = android.graphics.Color.parseColor("#CC000000")
        canvas.drawRoundRect(0f, 40f, width.toFloat(), 140f, 20f, 20f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 28f
        paint.typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
        canvas.drawText(message, 30f, 100f, paint)
        hintView.setImageBitmap(bitmap)
        hintView.visibility = View.VISIBLE
        target.post {
            if (!isAdded) return@post
            val loc = IntArray(2)
            target.getLocationInWindow(loc)
            val rootLoc = IntArray(2)
            view?.getLocationInWindow(rootLoc)
            hintView.translationX = (loc[0] - rootLoc[0] + (target.width / 2) - (width / 2)).toFloat()
            hintView.translationY = if (isAbove) (loc[1] - rootLoc[1] - 120).toFloat() else (loc[1] - rootLoc[1] + target.height + 20).toFloat()
        }
    }
}
