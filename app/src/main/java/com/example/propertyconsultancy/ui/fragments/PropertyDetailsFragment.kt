package com.example.propertyconsultancy.ui.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import com.example.propertyconsultancy.ui.dialogs.SelectionDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.EditText
import android.graphics.Typeface
import com.google.android.material.color.MaterialColors

class PropertyDetailsFragment : Fragment() {

    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etPropertyType: MaterialAutoCompleteTextView
    private lateinit var etFacing: MaterialAutoCompleteTextView
    private lateinit var etRoadSize: MaterialAutoCompleteTextView
    private lateinit var etFloor: MaterialAutoCompleteTextView
    private lateinit var etStatus: MaterialAutoCompleteTextView

    private var selectedProTypeId: Int? = null
    private var selectedFacingId: Int? = null
    private var selectedRoadSizeId: Int? = null
    private var selectedFloorId: Int? = null
    private var selectedStatusId: Int? = null
    private var statusDate: String? = null

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_property_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etTitle = view.findViewById(R.id.etTitle)
        etDescription = view.findViewById(R.id.etDescription)
        etPropertyType = view.findViewById(R.id.etPropertyType)
        etFacing = view.findViewById(R.id.etFacing)
        etRoadSize = view.findViewById(R.id.etRoadSize)
        etFloor = view.findViewById(R.id.etFloor)
        etStatus = view.findViewById(R.id.etStatus)

        setupSelectionDialogs()
        initSpeechRecognizer()
        setupVoiceHoldAndSpeak(view.findViewById(R.id.tilTitle), etTitle)
        setupVoiceHoldAndSpeak(view.findViewById(R.id.tilDescription), etDescription)
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

    private fun setupSelectionDialogs() {
        etPropertyType.setOnClickListener { showDialog("Property Type", "PropertyType", etPropertyType, selectedProTypeId) }
        etFacing.setOnClickListener { showDialog("Facing", "Facing", etFacing, selectedFacingId) }
        etRoadSize.setOnClickListener { showDialog("Road Size", "Road Size", etRoadSize, selectedRoadSizeId) }
        etFloor.setOnClickListener { showDialog("Floor", "Floor", etFloor, selectedFloorId) }
        etStatus.setOnClickListener { showDialog("Status", "Status", etStatus, selectedStatusId) }
    }

    private fun showDialog(title: String, groupName: String, target: EditText, currentId: Int?) {
        val categories = CategoryCache.getCategories(requireContext())
        val options = categories?.find { it.name.equals(groupName, true) }?.options ?: emptyList()
        SelectionDialogFragment(
            title = title,
            options = options,
            initialSelectedId = currentId,
            onSelected = { selectedValue ->
                // Update the target EditText with the selected value (possibly with extra data)
                target.setText(selectedValue)
                target.setTypeface(null, Typeface.BOLD)
                
                // Store the ID based on the groupName
                val selectedOption = options.find { option -> 
                    selectedValue.startsWith(option.option) 
                }
                
                when (groupName) {
                    "PropertyType" -> selectedProTypeId = selectedOption?.categoryId
                    "Facing" -> selectedFacingId = selectedOption?.categoryId
                    "Road Size" -> selectedRoadSizeId = selectedOption?.categoryId
                    "Floor" -> selectedFloorId = selectedOption?.categoryId
                    "Status" -> {
                        selectedStatusId = selectedOption?.categoryId
                        if (selectedValue.contains(" - ")) {
                            statusDate = selectedValue.substringAfter(" - ")
                        }
                    }
                }
                
                // Find parent TextInputLayout and update its background color from theme
                (target.parent.parent as? TextInputLayout)?.let { til ->
                    val color = MaterialColors.getColor(requireActivity(), com.google.android.material.R.attr.colorPrimaryContainer, 0)
                    til.boxBackgroundColor = color
                }
            }
        ).show(parentFragmentManager, "SelectionDialog")
    }

    fun setData(property: com.example.propertyconsultancy.data.dto.PropertyDTO) {
        etTitle.setText(property.title)
        etDescription.setText(property.description)
        
        selectedProTypeId = property.proTypeId
        selectedFacingId = property.facingId
        selectedRoadSizeId = property.roadSizeId
        selectedFloorId = property.floorId
        selectedStatusId = property.statusId
        statusDate = property.statusDate

        // Pre-fill selection dialogs if labels are available (Simulation)
        val categories = CategoryCache.getCategories(requireContext())
        
        fun setLabel(target: EditText, group: String, id: Int?) {
            if (id == null) return
            val option = categories?.find { it.name.equals(group, true) }?.options?.find { it.categoryId == id }
            option?.let { 
                target.setText(it.option)
                target.setTypeface(null, Typeface.BOLD)
                (target.parent.parent as? TextInputLayout)?.let { til ->
                    til.boxBackgroundColor = MaterialColors.getColor(requireActivity(), com.google.android.material.R.attr.colorPrimaryContainer, 0)
                }
            }
        }

        setLabel(etPropertyType, "PropertyType", selectedProTypeId)
        setLabel(etFacing, "Facing", selectedFacingId)
        setLabel(etRoadSize, "Road Size", selectedRoadSizeId)
        setLabel(etFloor, "Floor", selectedFloorId)
        setLabel(etStatus, "Status", selectedStatusId)
    }

    fun getData(): Map<String, Any?> {
        return mapOf(
            "title" to etTitle.text.toString(),
            "description" to etDescription.text.toString(),
            "protype_id" to selectedProTypeId,
            "facing_id" to selectedFacingId,
            "roadsize_id" to selectedRoadSizeId,
            "floor_id" to selectedFloorId,
            "status_id" to selectedStatusId,
            "status_date" to statusDate
        )
    }
}
