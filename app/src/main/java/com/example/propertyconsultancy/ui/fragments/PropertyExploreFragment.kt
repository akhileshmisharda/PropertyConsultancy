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
        viewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]
        (activity as? MainActivity)?.updateTitle("Property Explore")
        
        val property = property ?: return
        sessionManager.addActivityLog("Property Detail", "Viewed property: ${property.title}", "view")
        
        initViews(view)
        
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapExplore) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        fetchAllAmenities() // Fetch names/icons mapping
        refreshPropertyData() // Fetch latest from server immediately
        fetchInitialInteraction(property.propertyId ?: 0)
        
        if (sessionManager.isHintsEnabled()) {
            view.post { showAllStickyHints() }
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.shouldRefresh) {
            // If something was updated, we definitely need the latest data
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
                    
                    // CLEANED DEBUG LOGGING
                    Log.d("[php_debug]", "--- Property Details Refresh ---")
                    Log.d("[php_debug]", "ID: ${updated.propertyId} | Title: ${updated.title}")
                    Log.d("[php_debug]", "Media URLs Count: ${updated.mediaUrls?.size ?: 0}")
                    Log.d("[php_debug]", "Media URLs List: ${updated.mediaUrls?.joinToString(", ") ?: "NONE"}")
                    Log.d("[php_debug]", "Amenity Count: ${updated.amenityCount}")
                    Log.d("[php_debug]", "Amenity IDs: ${updated.amenityIds?.joinToString(", ") ?: "NONE"}")
                    Log.d("[php_debug]", "Furnishing: ${updated.furnishing}")
                    Log.d("[php_debug]", "Executive Info: ID=${updated.executiveId}, Name=${updated.executiveName}")
                    
                    property = updated
                    bindPropertyData(updated)
                    updateMapLocation()
                    Log.d("[php_debug]", "Refreshed Data for ${updated.title}. Exec: ${updated.executiveName}")
                }
            } catch (e: Exception) {
                Log.e("[Explore]", "Refresh Error: ${e.message}")
            } finally {
                exploreProgress.visibility = View.GONE
            }
        }
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
        if (ids.isNullOrEmpty()) return

        val context = requireContext()
        val grouped = allAmenities?.filter { ids.contains(it.amenityId) }?.groupBy { it.category ?: "General" } ?: emptyMap()
        val density = resources.displayMetrics.density
        
        // Helper to format string to Proper Case
        fun String.toProperCase() = this.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        val rootTrunk = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val entries = grouped.entries.toList()
        entries.forEachIndexed { groupIndex, entry ->
            val category = entry.key
            val amenities = entry.value
            val properCategory = category.toProperCase()

            // Category Level Branch
            val categoryBranch = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // 1. MAIN Vertical line part (RED)
            val mainVLine = View(context).apply {
                val width = (2 * density).toInt()
                val height = if (groupIndex == entries.size - 1) (18 * density).toInt() else LinearLayout.LayoutParams.MATCH_PARENT
                layoutParams = FrameLayout.LayoutParams(width, height).apply { gravity = Gravity.START; leftMargin = (8 * density).toInt() }
                setBackgroundColor(Color.RED)
            }
            categoryBranch.addView(mainVLine)

            // 2. MAIN Horizontal branch line (RED)
            val mainHLine = View(context).apply {
                layoutParams = FrameLayout.LayoutParams((15 * density).toInt(), (2 * density).toInt()).apply {
                    gravity = Gravity.START
                    leftMargin = (8 * density).toInt()
                    topMargin = (18 * density).toInt()
                }
                setBackgroundColor(Color.RED)
            }
            categoryBranch.addView(mainHLine)

            // 3. Category Header Text (Proper Case) - RED
            val tvCat = TextView(context).apply {
                text = properCategory
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.RED)
                setPadding((30 * density).toInt(), (10 * density).toInt(), 0, (10 * density).toInt())
            }
            categoryBranch.addView(tvCat)
            rootTrunk.addView(categoryBranch)

            val subGroupContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }

            amenities.forEachIndexed { index, amenity ->
                val branchItem = FrameLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // 1. Continue MAIN trunk (RED)
                if (groupIndex < entries.size - 1) {
                    val vMainCont = View(context).apply {
                        layoutParams = FrameLayout.LayoutParams((2 * density).toInt(), LinearLayout.LayoutParams.MATCH_PARENT).apply { 
                            gravity = Gravity.START; leftMargin = (8 * density).toInt() 
                        }
                        setBackgroundColor(Color.RED)
                    }
                    branchItem.addView(vMainCont)
                }

                // 2. SUB Vertical line part (RED)
                val vLine = View(context).apply {
                    val width = (1.5f * density).toInt()
                    // End line at the branch for the last item
                    val height = if (index == amenities.size - 1) (18 * density).toInt() else LinearLayout.LayoutParams.MATCH_PARENT
                    layoutParams = FrameLayout.LayoutParams(width, height).apply {
                        gravity = Gravity.START
                        leftMargin = (35 * density).toInt()
                    }
                    setBackgroundColor(Color.RED)
                    alpha = 0.7f
                }
                branchItem.addView(vLine)

                // 3. SUB Horizontal branch line (RED)
                val hLine = View(context).apply {
                    layoutParams = FrameLayout.LayoutParams((15 * density).toInt(), (2 * density).toInt()).apply {
                        gravity = Gravity.START
                        leftMargin = (35 * density).toInt()
                        topMargin = (17 * density).toInt()
                    }
                    setBackgroundColor(Color.RED)
                    alpha = 0.7f
                }
                branchItem.addView(hLine)

                // 4. Amenity Name only
                val tvName = TextView(context).apply {
                    text = amenity.name
                    textSize = 15f
                    setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.theme1_text))
                    setPadding((55 * density).toInt(), (10 * density).toInt(), 0, (10 * density).toInt())
                }
                branchItem.addView(tvName)

                subGroupContainer.addView(branchItem)
            }
            rootTrunk.addView(subGroupContainer)
        }
        llExploreAmenities.addView(rootTrunk)
    }

    private fun createAmenityItem(name: String, category: String?): View {
        // This function is no longer used by the new hierarchical branched layout
        return View(requireContext())
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
        
        // Populate Amenities Grid
        loadAmenityChips(property.amenityIds)
        
        tvInterested.text = "{ ${(5..25).random()} Interested }"
        tvDescription.text = property.description ?: "No description available."

        // Executive Info Binding with Logging
        Log.d("[php_debug]", "Executive Info Check: ID=${property.executiveId}, Name=${property.executiveName}, Mobile=${property.executiveMobile}")
        
        if (property.executiveId != null && !property.executiveName.isNullOrEmpty()) {
            cardExecutive.visibility = View.VISIBLE
            tvExecutiveName.text = property.executiveName
            tvExecutiveMobile.text = property.executiveMobile ?: "N/A"
            
            // Load Executive Image
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
            Log.d("[php_debug]", "Executive Card HIDDEN: property.executiveId is null or executiveName is empty")
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
