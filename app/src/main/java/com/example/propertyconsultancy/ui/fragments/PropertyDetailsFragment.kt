package com.example.propertyconsultancy.ui.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.cache.CategoryCache
import com.example.propertyconsultancy.data.dto.CategoryGroupDTO
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.EditText
import android.graphics.Typeface
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import android.text.InputType
import android.text.Editable
import android.text.TextWatcher
import com.example.propertyconsultancy.data.dto.CategoryOptionDTO

class PropertyDetailsFragment : Fragment() {

    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    
    private lateinit var cgOptions: ChipGroup
    private lateinit var tvOptionCaption: TextView
    private lateinit var tilExtraValue: TextInputLayout
    private lateinit var etExtraValue: TextInputEditText
    
    private lateinit var tabType: TextView
    private lateinit var tabFacing: TextView
    private lateinit var tabRoad: TextView
    private lateinit var tabFloor: TextView
    private lateinit var tabStatus: TextView

    private var selectedProTypeId: Int? = null
    private var selectedFacingId: Int? = null
    private var selectedRoadSizeId: Int? = null
    private var selectedFloorId: Int? = null
    private var selectedStatusId: Int? = null
    
    private val dynamicValues = mutableMapOf<String, String>()
    private val tabToActiveField = mutableMapOf<String, String?>()

    private var currentTab = "PropertyType"
    private var categories: List<CategoryGroupDTO>? = null

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_property_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etTitle = view.findViewById(R.id.etTitle)
        etDescription = view.findViewById(R.id.etDescription)
        
        cgOptions = view.findViewById(R.id.cgOptions)
        tvOptionCaption = view.findViewById(R.id.tvOptionCaption)
        tilExtraValue = view.findViewById(R.id.tilExtraValue)
        etExtraValue = view.findViewById(R.id.etExtraValue)
        
        etExtraValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val activeField = tabToActiveField[currentTab]
                if (activeField != null && tilExtraValue.visibility == View.VISIBLE) {
                    dynamicValues[activeField] = s.toString()
                }
            }
        })
        
        tabType = view.findViewById(R.id.tabType)
        tabFacing = view.findViewById(R.id.tabFacing)
        tabRoad = view.findViewById(R.id.tabRoad)
        tabFloor = view.findViewById(R.id.tabFloor)
        tabStatus = view.findViewById(R.id.tabStatus)

        categories = CategoryCache.getCategories(requireContext())
        
        setupTabs()
        initSpeechRecognizer()
        setupVoiceHoldAndSpeak(view.findViewById(R.id.tilTitle), etTitle)
        setupVoiceHoldAndSpeak(view.findViewById(R.id.tilDescription), etDescription)
        
        // Initial Tab
        switchTab("PropertyType", tabType)
    }

    private fun setupTabs() {
        tabType.setOnClickListener { switchTab("PropertyType", it as TextView) }
        tabFacing.setOnClickListener { switchTab("Facing", it as TextView) }
        tabRoad.setOnClickListener { switchTab("Road Size", it as TextView) }
        tabFloor.setOnClickListener { switchTab("Floor", it as TextView) }
        tabStatus.setOnClickListener { switchTab("Status", it as TextView) }
    }

    private fun switchTab(categoryName: String, tabView: TextView) {
        currentTab = categoryName
        
        // Update UI for all tabs
        val tabs = listOf(tabType, tabFacing, tabRoad, tabFloor, tabStatus)
        val density = resources.displayMetrics.density
        val overlap = (1 * density).toInt()

        tabs.forEach { 
            it.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            it.setTypeface(null, Typeface.NORMAL)
            it.setTextColor(MaterialColors.getColor(it, com.google.android.material.R.attr.colorOnSurfaceVariant))
            it.gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            it.textSize = 16f
            it.translationX = 0f
        }
        
        tabView.setBackgroundColor(android.graphics.Color.WHITE)
        tabView.setTypeface(null, Typeface.BOLD)
        tabView.setTextColor(MaterialColors.getColor(tabView, com.google.android.material.R.attr.colorPrimary))
        tabView.gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
        tabView.textSize = 18f
        tabView.translationX = overlap.toFloat() // Overlap the 1dp divider
        
        tvOptionCaption.text = "SELECT ${categoryName.uppercase()}"
        
        loadOptions(categoryName)
    }

    private fun loadOptions(categoryName: String) {
        cgOptions.removeAllViews()
        val category = categories?.find { it.name.contains(categoryName, true) }
        val options = category?.options ?: emptyList()
        
        val currentSelectedId = when (categoryName) {
            "PropertyType" -> selectedProTypeId
            "Facing" -> selectedFacingId
            "Road Size" -> selectedRoadSizeId
            "Floor" -> selectedFloorId
            "Status" -> selectedStatusId
            else -> null
        }

        options.forEach { option ->
            val chip = LayoutInflater.from(requireContext()).inflate(R.layout.layout_selection_chip, cgOptions, false) as Chip
            chip.text = option.option
            chip.id = option.categoryId
            chip.isChecked = option.categoryId == currentSelectedId
            
            if (chip.isChecked) {
                updateExtraFieldVisibility(option)
            }

            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    updateExtraFieldVisibility(option)

                    when (currentTab) {
                        "PropertyType" -> selectedProTypeId = option.categoryId
                        "Facing" -> selectedFacingId = option.categoryId
                        "Road Size" -> selectedRoadSizeId = option.categoryId
                        "Floor" -> selectedFloorId = option.categoryId
                        "Status" -> selectedStatusId = option.categoryId
                    }
                }
            }
            cgOptions.addView(chip)
        }
    }

    private fun updateExtraFieldVisibility(option: CategoryOptionDTO) {
        val hasField = option.hasField
        if (option.hasValue == 1 && hasField != null) {
            tabToActiveField[currentTab] = hasField
            tilExtraValue.visibility = View.VISIBLE
            tilExtraValue.hint = option.hasCaption ?: "Enter Value"
            setEtInputType(option.hasType)
            etExtraValue.setText(dynamicValues[hasField] ?: "")
        } else {
            tabToActiveField[currentTab] = null
            tilExtraValue.visibility = View.GONE
        }
    }

    private fun setEtInputType(type: String?) {
        etExtraValue.inputType = when (type?.lowercase()) {
            "number" -> InputType.TYPE_CLASS_NUMBER
            "decimal" -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            "date" -> InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_DATE
            "phone" -> InputType.TYPE_CLASS_PHONE
            else -> InputType.TYPE_CLASS_TEXT
        }
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        }
    }

    private fun setupVoiceHoldAndSpeak(til: TextInputLayout, editText: TextInputEditText) {
        val endIconView = til.findViewById<View>(com.google.android.material.R.id.text_input_end_icon)
        endIconView?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (checkAudioPermission()) {
                        v.isPressed = true
                        startListening(editText)
                    } else {
                        requestAudioPermission()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    stopListening()
                    true
                }
                else -> false
            }
        }
    }

    private fun checkAudioPermission() = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    private fun requestAudioPermission() = ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), 2000)

    private fun startListening(target: EditText) {
        if (isListening) return
        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { Toast.makeText(requireContext(), "Listening...", Toast.LENGTH_SHORT).show() }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)?.let { text ->
                    val current = target.text.toString()
                    target.setText(if (current.isEmpty()) text else "$current $text")
                    target.setSelection(target.length())
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }

    fun setData(property: com.example.propertyconsultancy.data.dto.PropertyDTO) {
        Log.d("[php_debug]", "PropertyDetailsFragment: Setting data. Title=${property.title}, Type=${property.proTypeId}, Status=${property.statusId}")
        etTitle.setText(property.title)
        etDescription.setText(property.description)
        
        selectedProTypeId = property.proTypeId
        selectedFacingId = property.facingId
        selectedRoadSizeId = property.roadSizeId
        selectedFloorId = property.floorId
        selectedStatusId = property.statusId
        
        property.statusDate?.let { dynamicValues["status_date"] = it }

        // Refresh current options to show selection
        loadOptions(currentTab)
    }

    fun getData(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>(
            "title" to etTitle.text.toString(),
            "description" to etDescription.text.toString(),
            "protype_id" to selectedProTypeId,
            "facing_id" to selectedFacingId,
            "roadsize_id" to selectedRoadSizeId,
            "floor_id" to selectedFloorId,
            "status_id" to selectedStatusId
        )
        
        // Add all dynamic values that belong to currently selected options
        tabToActiveField.values.filterNotNull().distinct().forEach { fieldName ->
            dynamicValues[fieldName]?.let { result[fieldName] = it }
        }
        
        return result
    }
}
