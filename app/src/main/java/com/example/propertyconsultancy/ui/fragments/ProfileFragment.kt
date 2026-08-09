package com.example.propertyconsultancy.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil3.load
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.UserDTO
import com.example.propertyconsultancy.data.local.SessionManager
import com.example.propertyconsultancy.data.remote.RetrofitInstance
import com.example.propertyconsultancy.ui.activities.LoginActivity
import com.example.propertyconsultancy.ui.activities.MainActivity
import com.example.propertyconsultancy.utils.FileUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.location.Geocoder
import java.util.Locale
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Base64
import java.io.InputStream
import androidx.activity.result.PickVisualMediaRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.chip.ChipGroup
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.auth.PhoneAuthProvider
import android.view.GestureDetector

class ProfileFragment : Fragment(), OnMapReadyCallback {

    private lateinit var sessionManager: SessionManager
    private var user: UserDTO? = null
    private var selectedImageUri: Uri? = null
    
    private var originalPhone: String = ""
    private var originalEmail: String = ""
    private var isMobileVerifiedOriginal: Int = 0
    private var isEmailVerifiedOriginal: Int = 0

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddressLine1: EditText
    private lateinit var etAddressLine2: EditText
    private lateinit var etCity: EditText
    private lateinit var etState: EditText
    private lateinit var etZipCode: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etRepeatPassword: EditText
    private lateinit var ivProfile: ImageView
    private lateinit var ivStatus: ImageView
    private lateinit var tvActiveStatus: TextView
    private lateinit var ivVerified: ImageView
    private lateinit var tvVerifiedStatus: TextView
    private lateinit var ivVerifiedEmail: ImageView
    private lateinit var tvVerifiedStatusEmail: TextView
    private lateinit var tvAccountSince: TextView
    private lateinit var btnUpdate: Button
    
    private lateinit var mapOverlay: View
    private lateinit var tvMapInstruction: TextView
    private var googleMap: GoogleMap? = null
    private var currentLat: Double = 21.1458
    private var currentLng: Double = 79.0882
    private var isUserDraggingMap = false
    private var isProgrammaticChange = false
    
    private lateinit var layoutPasswordChange: View
    private lateinit var btnChangePasswordToggle: TextView
    
    // OTP UI Elements
    private lateinit var layoutOtp: View
    private lateinit var etOtp: EditText
    private lateinit var btnVerifyOtp: View
    private lateinit var tvOtpCountdown: TextView
    private lateinit var btnResendOtp: View
    private var countDownTimer: android.os.CountDownTimer? = null

    private val colorDarkGreen = android.graphics.Color.parseColor("#1B5E20")
    private val colorRed = android.graphics.Color.parseColor("#C62828")
    private val colorGray = android.graphics.Color.parseColor("#757575")

    private var speechRecognizer: SpeechRecognizer? = null
    
    // Firebase Phone Auth
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    private val pickProfileImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            ivProfile.load(uri)
            uploadProfileImage(uri)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        user = sessionManager.getUser()
        auth = FirebaseAuth.getInstance()

        sessionManager.addActivityLog("Profile", "Viewed personal profile", "info")

        (activity as? MainActivity)?.updateTitle("Profile")

        initViews(view)
        setupUI()
        setupListeners()
        setupSpeech()
        setupMap()

        ivProfile.setOnClickListener {
            pickProfileImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnUpdate.setOnClickListener { updateProfile() }
        
        btnVerifyOtp.setOnClickListener {
            val code = etOtp.text.toString().trim()
            if (code.length == 6) {
                hideKeyboard()
                val id = verificationId ?: return@setOnClickListener
                val credential = PhoneAuthProvider.getCredential(id, code)
                verifyAndSignIn(credential)
            } else {
                Toast.makeText(requireContext(), "Enter 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }

        btnResendOtp.setOnClickListener {
            user?.let { initiatePhoneVerification(it.phone, isResend = true) }
        }

        view.findViewById<View>(R.id.btnBack)?.visibility = View.GONE // Hide back button if in tab

        btnChangePasswordToggle.setOnClickListener {
            if (layoutPasswordChange.visibility == View.GONE) {
                layoutPasswordChange.visibility = View.VISIBLE
                btnChangePasswordToggle.text = "Cancel Password Change"
            } else {
                layoutPasswordChange.visibility = View.GONE
                btnChangePasswordToggle.text = "Change Password"
                etNewPassword.text.clear()
                etRepeatPassword.text.clear()
            }
        }
    }

    private fun uploadProfileImage(uri: Uri) {
        val b64 = FileUtils.encodeUriToBase64(requireContext(), uri) ?: return
        val profileImageB64 = "data:image/jpeg;base64,$b64"
        
        val updatedUser = user?.copy(profileImageUrl = profileImageB64) ?: return
        
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.updateProfile(updatedUser)
                if (response.status == "success" && response.user != null) {
                    sessionManager.saveUser(response.user)
                    user = response.user
                    Toast.makeText(requireContext(), "Profile Image Uploaded", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("[Profile]", "Upload Error: ${e.message}")
            }
        }
    }

    private fun initViews(view: View) {
        etFirstName = view.findViewById(R.id.etFirstName)
        etLastName = view.findViewById(R.id.etLastName)
        etEmail = view.findViewById(R.id.etEmail)
        etPhone = view.findViewById(R.id.etPhone)
        etNewPassword = view.findViewById(R.id.etNewPassword)
        etRepeatPassword = view.findViewById(R.id.etRepeatPassword)
        ivProfile = view.findViewById(R.id.ivProfile)
        ivStatus = view.findViewById(R.id.ivStatus)
        tvActiveStatus = view.findViewById(R.id.tvActiveStatus)
        ivVerified = view.findViewById(R.id.ivVerified)
        tvVerifiedStatus = view.findViewById(R.id.tvVerifiedStatus)
        ivVerifiedEmail = view.findViewById(R.id.ivVerifiedEmail)
        tvVerifiedStatusEmail = view.findViewById(R.id.tvVerifiedStatusEmail)
        tvAccountSince = view.findViewById(R.id.tvAccountSince)
        btnUpdate = view.findViewById(R.id.btnUpdate)
        
        mapOverlay = view.findViewById(R.id.mapOverlay)
        tvMapInstruction = view.findViewById(R.id.tvMapInstruction)
        
        etAddressLine1 = view.findViewById(R.id.etAddressLine1)
        etAddressLine2 = view.findViewById(R.id.etAddressLine2)
        etCity = view.findViewById(R.id.etCity)
        etState = view.findViewById(R.id.etState)
        etZipCode = view.findViewById(R.id.etZipCode)
        
        layoutOtp = view.findViewById(R.id.layoutOtp)
        etOtp = view.findViewById(R.id.etOtp)
        btnVerifyOtp = view.findViewById(R.id.btnVerifyOtp)
        tvOtpCountdown = view.findViewById(R.id.tvOtpCountdown)
        btnResendOtp = view.findViewById(R.id.btnResendOtp)

        layoutPasswordChange = view.findViewById(R.id.layoutPasswordChange)
        btnChangePasswordToggle = view.findViewById(R.id.btnChangePasswordToggle)
        
        setupMicButton(view.findViewById(R.id.btnMicFirstName), etFirstName)
        setupMicButton(view.findViewById(R.id.btnMicLastName), etLastName)
        setupMicButton(view.findViewById(R.id.btnMicEmail), etEmail)
        setupMicButton(view.findViewById(R.id.btnMicPhone), etPhone)
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

    private fun setupSpeech() {
        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        }
    }

    private fun checkPermissionAndStart(target: EditText) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            startSpeech(target)
        }
    }

    private fun startSpeech(target: EditText) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    target.setText(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    private fun stopSpeech() { speechRecognizer?.stopListening() }

    private fun setupListeners() {
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateVerificationUI()
            }
        }
        etPhone.addTextChangedListener(textWatcher)
        etEmail.addTextChangedListener(textWatcher)
    }

    private fun updateVerificationUI() {
        val currentPhone = etPhone.text.toString().trim()
        val currentEmail = etEmail.text.toString().trim()

        // Phone Verification UI
        if (currentPhone == originalPhone && isMobileVerifiedOriginal == 1) {
            ivVerified.setImageResource(R.drawable.ic_tick)
            ivVerified.setColorFilter(colorDarkGreen)
            tvVerifiedStatus.text = "Verified"
            tvVerifiedStatus.setTextColor(colorDarkGreen)
            tvVerifiedStatus.setOnClickListener(null)
        } else {
            ivVerified.setImageResource(R.drawable.ic_pending)
            ivVerified.setColorFilter(colorGray)
            tvVerifiedStatus.text = "Verify Now"
            tvVerifiedStatus.setTextColor(colorRed)
            tvVerifiedStatus.setOnClickListener { initiatePhoneVerification(currentPhone) }
        }

        // Email Verification UI
        if (currentEmail == originalEmail && isEmailVerifiedOriginal == 1) {
            ivVerifiedEmail.setImageResource(R.drawable.ic_tick)
            ivVerifiedEmail.setColorFilter(colorDarkGreen)
            tvVerifiedStatusEmail.text = "Verified"
            tvVerifiedStatusEmail.setTextColor(colorDarkGreen)
            tvVerifiedStatusEmail.setOnClickListener(null)
        } else {
            ivVerifiedEmail.setImageResource(R.drawable.ic_pending)
            ivVerifiedEmail.setColorFilter(colorGray)
            tvVerifiedStatusEmail.text = "Verify Now"
            tvVerifiedStatusEmail.setTextColor(colorRed)
            tvVerifiedStatusEmail.setOnClickListener { showEmailVerificationDialog() }
        }
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapProfile) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                googleMap?.uiSettings?.setAllGesturesEnabled(true)
                mapOverlay.visibility = View.GONE
                tvMapInstruction.visibility = View.GONE
                Toast.makeText(requireContext(), "Map editing enabled", Toast.LENGTH_SHORT).show()
                return true
            }
        })

        mapOverlay.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.parent.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.parent.requestDisallowInterceptTouchEvent(false)
            }
            true
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.setAllGesturesEnabled(false)
        
        val pos = LatLng(currentLat, currentLng)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))

        googleMap?.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                isUserDraggingMap = true
            }
        }

        googleMap?.setOnCameraIdleListener {
            val target = googleMap?.cameraPosition?.target ?: return@setOnCameraIdleListener
            currentLat = target.latitude
            currentLng = target.longitude
            
            if (isUserDraggingMap) {
                isUserDraggingMap = false
                reverseGeocode(currentLat, currentLng)
            }
        }
    }

    private fun reverseGeocode(lat: Double, lng: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    withContext(Dispatchers.Main) {
                        isProgrammaticChange = true
                        etAddressLine1.setText(addr.featureName ?: addr.thoroughfare ?: "")
                        etAddressLine2.setText(addr.subLocality ?: "")
                        etCity.setText(addr.locality ?: addr.subAdminArea ?: "")
                        etState.setText(addr.adminArea ?: "")
                        etZipCode.setText(addr.postalCode ?: "")
                        isProgrammaticChange = false
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun setupUI() {
        user?.let { userInfo ->
            originalPhone = userInfo.phone
            originalEmail = userInfo.email
            isMobileVerifiedOriginal = userInfo.mobileVerified
            isEmailVerifiedOriginal = userInfo.emailVerified

            etFirstName.setText(userInfo.firstName)
            etLastName.setText(userInfo.lastName)
            etEmail.setText(userInfo.email)
            etPhone.setText(userInfo.phone)
            etAddressLine1.setText(userInfo.addressLine1)
            etAddressLine2.setText(userInfo.addressLine2)
            etCity.setText(userInfo.city)
            etState.setText(userInfo.state)
            etZipCode.setText(userInfo.zipCode)
            
            val profileUrl = userInfo.profileImageUrl
            val finalUrl = if (profileUrl != null && !profileUrl.startsWith("http") && !profileUrl.startsWith("data:")) {
                "http://fabkraft.in/property/$profileUrl"
            } else {
                profileUrl
            }
            ivProfile.load(finalUrl ?: "https://via.placeholder.com/150")
            
            if (userInfo.status == "active") {
                ivStatus.setImageResource(R.drawable.ic_tick); ivStatus.setColorFilter(colorDarkGreen)
                tvActiveStatus.text = "Active"; tvActiveStatus.setTextColor(colorDarkGreen)
            } else {
                ivStatus.setImageResource(R.drawable.ic_cancel); ivStatus.setColorFilter(colorRed)
                tvActiveStatus.text = "Inactive"; tvActiveStatus.setTextColor(colorRed)
            }

            updateVerificationUI()
            
            tvAccountSince.text = userInfo.createdAt ?: "N/A"

            // Move map to existing address if available
            val fullAddress = listOfNotNull(userInfo.addressLine1, userInfo.city, userInfo.state, userInfo.zipCode).joinToString(", ")
            if (fullAddress.isNotEmpty()) {
                geocodeAddress(fullAddress)
            }
        }
    }

    private fun geocodeAddress(address: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocationName(address, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    withContext(Dispatchers.Main) {
                        currentLat = addr.latitude
                        currentLng = addr.longitude
                        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(currentLat, currentLng), 15f))
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun initiatePhoneVerification(phone: String, isResend: Boolean = false) {
        var cleanedPhone = phone.replace(" ", "").replace("-", "")
        if (cleanedPhone.startsWith("00")) {
            cleanedPhone = "+" + cleanedPhone.substring(2)
        }
        val formattedPhone = if (cleanedPhone.startsWith("+")) cleanedPhone else "+91$cleanedPhone"
        
        Log.d("sms_debug", "Profile Initiating verification for: $formattedPhone (Resend: $isResend)")
        
        Toast.makeText(requireContext(), "Sending OTP...", Toast.LENGTH_SHORT).show()
        
        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d("sms_debug", "Profile onVerificationCompleted")
                    
                    credential.smsCode?.let { 
                        Log.d("sms_debug", "Profile smsCode auto-retrieved: $it")
                        etOtp.setText(it)
                    }
                    
                    verifyAndSignIn(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e("sms_debug", "Profile onVerificationFailed: ${e.message}", e)
                    Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    Log.d("sms_debug", "Profile onCodeSent: $id")
                    verificationId = id
                    resendToken = token
                    
                    // Show inline OTP layout
                    layoutOtp.visibility = View.VISIBLE
                    startCountdown()
                }
            })
            
        if (isResend && resendToken != null) {
            builder.setForceResendingToken(resendToken!!)
        }
        
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    private fun startCountdown() {
        countDownTimer?.cancel()
        btnResendOtp.visibility = View.GONE
        tvOtpCountdown.visibility = View.VISIBLE
        
        countDownTimer = object : android.os.CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvOtpCountdown.text = "Resend in ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                tvOtpCountdown.visibility = View.GONE
                btnResendOtp.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun verifyAndSignIn(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    layoutOtp.visibility = View.GONE
                    countDownTimer?.cancel()
                    performVerification("phone")
                } else {
                    Toast.makeText(requireContext(), "Verification Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showEmailVerificationDialog() {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Verify Email")
        builder.setMessage("Enter the 4-digit code sent to your email (Mock: 1234)")
        
        val input = EditText(requireContext())
        input.hint = "4-digit OTP"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        builder.setView(input)

        builder.setPositiveButton("Verify") { _, _ ->
            if (input.text.toString() == "1234") {
                performVerification("email")
            } else {
                Toast.makeText(requireContext(), "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun performVerification(type: String) {
        val updatedUser = if (type == "phone") {
            user?.copy(mobileVerified = 1)
        } else {
            user?.copy(emailVerified = 1)
        } ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.updateProfile(updatedUser)
                if (response.status == "success") {
                    // Update local session since server doesn't return full user
                    sessionManager.saveUser(updatedUser)
                    user = updatedUser
                    setupUI()
                    Toast.makeText(requireContext(), "Verification Successful", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateProfile() {
        val currentPhone = etPhone.text.toString().trim()
        val currentEmail = etEmail.text.toString().trim()

        val updatedUser = user?.copy(
            firstName = etFirstName.text.toString(),
            lastName = etLastName.text.toString(),
            email = currentEmail,
            phone = currentPhone,
            mobileVerified = if (currentPhone == originalPhone) isMobileVerifiedOriginal else 0,
            emailVerified = if (currentEmail == originalEmail) isEmailVerifiedOriginal else 0,
            addressLine1 = etAddressLine1.text.toString(),
            addressLine2 = etAddressLine2.text.toString(),
            city = etCity.text.toString(),
            state = etState.text.toString(),
            zipCode = etZipCode.text.toString(),
            latitude = currentLat,
            longitude = currentLng
        ) ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.updateProfile(updatedUser)
                if (response.status == "success") {
                    // Update image URL if returned
                    val finalUser = if (response.user != null) response.user else {
                        // If PHP didn't return user, use the one we sent but update image if provided
                        val newImg = if (response.status == "success" && !response.message.contains("error")) {
                            // The PHP returns "image_url" in the JSON root
                            // Since our AuthResponseDTO.user is mapped to 'user', we might need to handle raw response
                            // But for now, let's assume if it's success, we update local.
                            updatedUser
                        } else updatedUser
                        newImg
                    }
                    sessionManager.saveUser(finalUser)
                    user = finalUser
                    Toast.makeText(requireContext(), "Profile Updated", Toast.LENGTH_SHORT).show()
                    setupUI()
                } else {
                    Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer?.destroy()
    }
}
