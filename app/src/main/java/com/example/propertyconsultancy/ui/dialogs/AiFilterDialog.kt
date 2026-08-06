package com.example.propertyconsultancy.ui.dialogs

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.propertyconsultancy.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AiFilterDialog(
    private val onSearch: (String) -> Unit
) : DialogFragment() {

    private lateinit var etAiInput: EditText
    private lateinit var tilAiInput: com.google.android.material.textfield.TextInputLayout
    private lateinit var btnApplyAi: com.google.android.material.button.MaterialButton
    private lateinit var aiProgress: com.google.android.material.progressindicator.LinearProgressIndicator
    private lateinit var tvAiStatus: TextView
    private lateinit var tvAiHint: TextView
    private lateinit var layoutFields: LinearLayout
    private lateinit var fabMic: FloatingActionButton
    private lateinit var viewPulse: View
    
    private val capturedValues = mutableMapOf<String, String>()
    private var speechRecognizer: SpeechRecognizer? = null
    private var isUserSpeaking = false
    private var textBeforeSpeech = ""
    private var pulseAnimator: ObjectAnimator? = null

    private val fieldConfig = listOf(
        FieldDef("City", "Speak city name...", "Bhilwara, Nagpur, Mumbai, Pune..."),
        FieldDef("Type", "House/Apartment/Villa...", "Apartment, Villa, Plot, Office..."),
        FieldDef("BHK", "How many bedrooms?", "1 BHK, 2 BHK, 3 BHK, 4 BHK..."),
        FieldDef("Budget", "Price range (e.g. under 30k)", "Under 20k, 50k to 1 Lakh..."),
        FieldDef("Facing", "East/West preference?", "East, West, North, South..."),
        FieldDef("Road Size", "30ft, 40ft road...", "30ft, 40ft, 60ft, 80ft..."),
        FieldDef("Furnished", "Furnished/Semi/Un...", "Furnished, Semi-furnished, Unfurnished")
    )

    data class FieldDef(val label: String, val hint: String, val choices: String)

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout((resources.displayMetrics.widthPixels * 0.95).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_ai_filter, container, false)
        initViews(view)
        setupSpeechRecognizer()
        setupInteractions()
        setupTextWatcher()
        updateFieldsUI()
        return view
    }

    private fun setupTextWatcher() {
        etAiInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                analyzeText(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupInteractions() {
        tilAiInput.setStartIconOnClickListener {
            etAiInput.setText("")
            capturedValues.clear()
            updateFieldsUI()
            updateDynamicHint()
        }

        fabMic.setOnClickListener {
            if (isUserSpeaking) {
                stopListening()
            } else {
                if (checkAudioPermission()) startListening()
                else requestAudioPermission()
            }
        }

        btnApplyAi.setOnClickListener {
            val input = etAiInput.text.toString().trim()
            if (input.isNotEmpty()) {
                stopListening()
                onSearch(input)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Please speak your requirement", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { aiProgress.visibility = View.VISIBLE }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) { if (isUserSpeaking) startListeningInternal() }
                override fun onResults(results: Bundle?) {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)?.let { text ->
                        appendSpokenText(text)
                    }
                    if (isUserSpeaking) startListeningInternal()
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)?.let { text ->
                        updatePreviewText(text)
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startListening() {
        isUserSpeaking = true
        textBeforeSpeech = etAiInput.text.toString().trim()
        tvAiStatus.text = "Listening... Tap to Stop"
        tvAiStatus.setTextColor(Color.RED)
        
        fabMic.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        fabMic.backgroundTintList = ColorStateList.valueOf(Color.RED)
        startPulseAnimation()
        startListeningInternal()
    }

    private fun startListeningInternal() {
        if (!isUserSpeaking) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        isUserSpeaking = false
        speechRecognizer?.stopListening()
        aiProgress.visibility = View.GONE
        tvAiStatus.text = "Tap mic to start"
        tvAiStatus.setTextColor(Color.GRAY)
        
        fabMic.setImageResource(android.R.drawable.ic_btn_speak_now)
        fabMic.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.modern_primary))
        stopPulseAnimation()
    }

    private fun startPulseAnimation() {
        viewPulse.visibility = View.VISIBLE
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
            viewPulse,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.6f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.6f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f)
        ).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        viewPulse.visibility = View.GONE
    }

    private fun appendSpokenText(newText: String) {
        val updated = if (textBeforeSpeech.isEmpty()) newText else "$textBeforeSpeech $newText"
        etAiInput.setText(updated); etAiInput.setSelection(updated.length)
        textBeforeSpeech = updated
    }

    private fun updatePreviewText(partialText: String) {
        val display = if (textBeforeSpeech.isEmpty()) partialText else "$textBeforeSpeech $partialText"
        etAiInput.setText(display); etAiInput.setSelection(display.length)
        analyzeText(display)
    }

    private fun initViews(view: View) {
        etAiInput = view.findViewById(R.id.etAiInput)
        tilAiInput = view.findViewById(R.id.tilAiInput)
        btnApplyAi = view.findViewById(R.id.btnApplyAi)
        aiProgress = view.findViewById(R.id.aiProgress)
        tvAiStatus = view.findViewById(R.id.tvAiStatus)
        tvAiHint = view.findViewById(R.id.tvAiHint)
        layoutFields = view.findViewById(R.id.layoutFields)
        fabMic = view.findViewById(R.id.fabMic)
        viewPulse = view.findViewById(R.id.viewPulse)
    }

    private fun analyzeText(input: String) {
        val text = input.lowercase()
        val cities = listOf("Bhilwara", "Nagpur", "Mumbai", "Pune", "Delhi", "Bangalore")
        cities.forEach { if (text.contains(it.lowercase())) capturedValues["City"] = it }
        val typeMap = mapOf("ghar" to "House", "makan" to "House", "house" to "House", "flat" to "Apartment", "apartment" to "Apartment", "villa" to "Villa", "office" to "Office", "shop" to "Shop")
        val found = mutableSetOf<String>(); typeMap.forEach { (k, v) -> if (text.contains(k)) found.add(v) }; if (found.isNotEmpty()) capturedValues["Type"] = found.joinToString(", ")
        Regex("(\\d+)\\s*bhk").find(text)?.groupValues?.get(1)?.let { capturedValues["BHK"] = it }
        fun parsePrice(s: String): Double? {
            val n = Regex("(\\d+\\.?\\d*)").find(s.replace(",","").replace(" ","").replace("hazaar","000").replace("thousand","000"))?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
            return when { s.contains("cr") -> n * 1e7; s.contains("lakh") || s.contains("lac") -> n * 1e5; s.contains("k") -> n * 1e3; else -> n }
        }
        val budgetTriggers = listOf("under", "below", "budget", "rent", "kiraya", "kiraye", "price")
        budgetTriggers.forEach { if (text.contains(it)) parsePrice(text.substringAfter(it))?.let { capturedValues["Budget"] = "Max ₹${it.toInt()}" } }
        val facings = listOf("East", "West", "North", "South"); facings.forEach { if (text.contains(it.lowercase())) capturedValues["Facing"] = it }
        Regex("(\\d+)\\s*(ft|feet|road)").find(text)?.groupValues?.get(1)?.let { capturedValues["Road Size"] = "${it}ft" }
        if (text.contains("unfurnished")) capturedValues["Furnished"] = "Unfurnished" else if (text.contains("semi-furnished") || text.contains("semi furnished")) capturedValues["Furnished"] = "Semi-furnished" else if (text.contains("furnished")) capturedValues["Furnished"] = "Furnished"
        updateFieldsUI(); updateDynamicHint()
    }

    private fun updateFieldsUI() {
        layoutFields.removeAllViews(); val inflater = LayoutInflater.from(requireContext())
        val (found, remaining) = fieldConfig.partition { capturedValues.containsKey(it.label) }
        remaining.forEach { addFieldRow(inflater, it, false) }; found.forEach { addFieldRow(inflater, it, true) }
    }

    private fun addFieldRow(inflater: LayoutInflater, config: FieldDef, isFound: Boolean) {
        val row = inflater.inflate(R.layout.item_ai_field, layoutFields, false)
        val tvValue = row.findViewById<TextView>(R.id.tvFieldValue)
        row.findViewById<TextView>(R.id.tvFieldLabel).text = config.label
        if (isFound) {
            tvValue.text = capturedValues[config.label]; tvValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.modern_primary)); tvValue.setTypeface(null, Typeface.BOLD)
            row.findViewById<View>(R.id.tvFieldChoices).visibility = View.GONE
            row.findViewById<ImageButton>(R.id.btnClearField).apply { visibility = View.VISIBLE; setOnClickListener { capturedValues.remove(config.label); updateFieldsUI(); updateDynamicHint() } }
        } else {
            tvValue.text = config.hint; tvValue.setTextColor(Color.parseColor("#BBBBBB"))
            row.findViewById<TextView>(R.id.tvFieldChoices).apply { text = "Choices: ${config.choices}"; visibility = View.VISIBLE }
        }
        layoutFields.addView(row)
    }

    private fun updateDynamicHint() {
        val hint = when {
            !capturedValues.containsKey("City") -> "Which city are you looking in?"
            !capturedValues.containsKey("Type") -> "What type? House, Flat, Office..."
            !capturedValues.containsKey("BHK") && capturedValues["Type"]?.contains("Plot", true) != true -> "How many BHK?"
            !capturedValues.containsKey("Budget") -> "Any budget or rent preference?"
            else -> "Ready to search? Or add more preferences."
        }
        tvAiHint.text = hint
    }

    private fun checkAudioPermission() = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    private fun requestAudioPermission() = ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), 2000)
    override fun onDestroy() { super.onDestroy(); speechRecognizer?.destroy() }
}
