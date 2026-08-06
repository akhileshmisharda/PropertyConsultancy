package com.example.propertyconsultancy.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import coil3.load
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.RegisterRequest
import com.example.propertyconsultancy.data.local.SessionManager
import com.example.propertyconsultancy.data.remote.RetrofitInstance
import com.example.propertyconsultancy.utils.FileUtils
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import java.util.Locale

class RegisterActivity : BaseActivity() {

    private lateinit var sessionManager: SessionManager
    private var profileImageB64: String? = null
    private lateinit var ivProfile: ImageView
    private lateinit var registerProgress: LinearProgressIndicator
    private var speechRecognizer: SpeechRecognizer? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            ivProfile.load(uri)
            profileImageB64 = FileUtils.encodeUriToBase64(this, uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        sessionManager = SessionManager(this)
        
        setupInsets()
        initRegisterUI()
        setupSpeech()
    }

    private fun setupInsets() {
        val root = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }
    }

    private fun initRegisterUI() {
        registerProgress = findViewById(R.id.registerProgress)
        
        // Hide elements that are only for updating profile
        findViewById<View>(R.id.llActive).visibility = View.GONE
        findViewById<View>(R.id.llVerified).visibility = View.GONE
        findViewById<View>(R.id.llVerifiedEmail).visibility = View.GONE
        findViewById<View>(R.id.llAccountSinceContainer).visibility = View.GONE
        findViewById<View>(R.id.btnChangePasswordToggle).visibility = View.GONE
        
        // Show back button
        findViewById<View>(R.id.btnBack).apply {
            visibility = View.VISIBLE
            setOnClickListener { finish() }
        }

        // Show password fields by default for registration
        findViewById<View>(R.id.layoutPasswordChange).visibility = View.VISIBLE

        ivProfile = findViewById(R.id.ivProfile)
        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etAddressLine1 = findViewById<EditText>(R.id.etAddressLine1)
        val etAddressLine2 = findViewById<EditText>(R.id.etAddressLine2)
        val etCity = findViewById<EditText>(R.id.etCity)
        val etState = findViewById<EditText>(R.id.etState)
        val etZipCode = findViewById<EditText>(R.id.etZipCode)
        val etPassword = findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etRepeatPassword)
        val btnRegister = findViewById<Button>(R.id.btnUpdate)

        btnRegister.text = "Create Account"

        ivProfile.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Setup Mic buttons for all fields
        setupMicButton(findViewById(R.id.btnMicFirstName), etFirstName)
        setupMicButton(findViewById(R.id.btnMicLastName), etLastName)
        setupMicButton(findViewById(R.id.btnMicEmail), etEmail)
        setupMicButton(findViewById(R.id.btnMicPhone), etPhone)

        btnRegister.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val addressLine1 = etAddressLine1.text.toString().trim()
            val addressLine2 = etAddressLine2.text.toString().trim()
            val city = etCity.text.toString().trim()
            val state = etState.text.toString().trim()
            val zipCode = etZipCode.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirm = etConfirmPassword.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performRegister(firstName, lastName, email, phone, password, addressLine1, addressLine2, city, state, zipCode)
        }
    }

    private fun setupSpeech() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMicButton(btn: View, target: EditText) {
        btn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    checkPermissionAndStart(target)
                    btn.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopSpeech()
                    btn.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }
            }
            true
        }
    }

    private fun checkPermissionAndStart(target: EditText) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            startSpeech(target)
        }
    }

    private fun startSpeech(target: EditText) {
        Log.d("[php_debug]", "startSpeech: Initializing recognizer for ${target.hint}")
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { Log.d("[php_debug]", "Speech Ready") }
            override fun onBeginningOfSpeech() { Log.d("[php_debug]", "Speech Beginning") }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { Log.d("[php_debug]", "Speech End") }
            override fun onError(error: Int) { 
                Log.e("[php_debug]", "Speech Error: $error")
                val msg = when(error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio Error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client Error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission Denied"
                    SpeechRecognizer.ERROR_NETWORK -> "Network Error"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    else -> "Speech Error $error"
                }
                Toast.makeText(this@RegisterActivity, msg, Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d("[php_debug]", "Speech Results: ${matches[0]}")
                    target.setText(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    private fun stopSpeech() { speechRecognizer?.stopListening() }

    private fun performRegister(
        firstName: String, lastName: String, email: String, phone: String, password: String,
        addressLine1: String, addressLine2: String, city: String, state: String, zipCode: String
    ) {
        registerProgress.visibility = View.VISIBLE
        Log.d("[php_debug]", "performRegister: Starting registration for $email")

        lifecycleScope.launch {
            try {
                val request = RegisterRequest(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    phone = phone,
                    password = password,
                    addressLine1 = addressLine1,
                    addressLine2 = addressLine2,
                    city = city,
                    state = state,
                    zipCode = zipCode,
                    profileImageUrl = profileImageB64?.let { "data:image/jpeg;base64,$it" }
                )
                
                Log.d("[php_debug]", "Sending Register Request: $request")
                val response = RetrofitInstance.api.register(request)
                Log.d("[php_debug]", "performRegister response: $response")

                if (response.status == "success" && response.user != null) {
                    sessionManager.saveUser(response.user)
                    Toast.makeText(this@RegisterActivity, "Welcome to Property Consultancy! Please login.", Toast.LENGTH_LONG).show()
                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Log.e("[php_debug]", "Registration failed: ${response.message}")
                    Toast.makeText(this@RegisterActivity, response.message, Toast.LENGTH_SHORT).show()
                    registerProgress.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("[php_debug]", "Registration exception: ${e.message}")
                e.printStackTrace()
                
                val errorBody = (e as? retrofit2.HttpException)?.response()?.errorBody()?.string()
                Log.e("[php_debug]", "Raw Error Body: $errorBody")
                
                val displayMsg = if (!errorBody.isNullOrEmpty()) {
                    try {
                        val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                        json.get("message").asString
                    } catch (ex: Exception) { errorBody }
                } else {
                    e.message ?: "Network Error"
                }
                
                Toast.makeText(this@RegisterActivity, "Error: $displayMsg", Toast.LENGTH_LONG).show()
                registerProgress.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}
