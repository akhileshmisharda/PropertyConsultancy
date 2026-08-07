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
import java.util.Locale
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Base64
import java.io.InputStream
import androidx.activity.result.PickVisualMediaRequest

class ProfileFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var user: UserDTO? = null
    private var selectedImageUri: Uri? = null

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
    
    private lateinit var layoutPasswordChange: View
    private lateinit var btnChangePasswordToggle: TextView

    private val colorDarkGreen = android.graphics.Color.parseColor("#1B5E20")
    private val colorRed = android.graphics.Color.parseColor("#C62828")
    private val colorGray = android.graphics.Color.parseColor("#757575")

    private var speechRecognizer: SpeechRecognizer? = null

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

        sessionManager.addActivityLog("Profile", "Viewed personal profile", "info")

        (activity as? MainActivity)?.updateTitle("Profile")

        initViews(view)
        setupUI()
        setupSpeech()

        ivProfile.setOnClickListener {
            pickProfileImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnUpdate.setOnClickListener { updateProfile() }
        
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
        
        etAddressLine1 = view.findViewById(R.id.etAddressLine1)
        etAddressLine2 = view.findViewById(R.id.etAddressLine2)
        etCity = view.findViewById(R.id.etCity)
        etState = view.findViewById(R.id.etState)
        etZipCode = view.findViewById(R.id.etZipCode)
        
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

    private fun setupUI() {
        user?.let {
            etFirstName.setText(it.firstName)
            etLastName.setText(it.lastName)
            etEmail.setText(it.email)
            etPhone.setText(it.phone)
            etAddressLine1.setText(it.addressLine1)
            etAddressLine2.setText(it.addressLine2)
            etCity.setText(it.city)
            etState.setText(it.state)
            etZipCode.setText(it.zipCode)
            
            val profileUrl = it.profileImageUrl
            val finalUrl = if (profileUrl != null && !profileUrl.startsWith("http") && !profileUrl.startsWith("data:")) {
                "http://fabkraft.in/property/$profileUrl"
            } else {
                profileUrl
            }
            ivProfile.load(finalUrl ?: "https://via.placeholder.com/150")
            
            if (it.status == "active") {
                ivStatus.setImageResource(R.drawable.ic_tick); ivStatus.setColorFilter(colorDarkGreen)
                tvActiveStatus.text = "Active"; tvActiveStatus.setTextColor(colorDarkGreen)
            } else {
                ivStatus.setImageResource(R.drawable.ic_cancel); ivStatus.setColorFilter(colorRed)
                tvActiveStatus.text = "Inactive"; tvActiveStatus.setTextColor(colorRed)
            }

            if (it.mobileVerified == 1) {
                ivVerified.setImageResource(R.drawable.ic_tick); ivVerified.setColorFilter(colorDarkGreen)
                tvVerifiedStatus.text = "Verified"; tvVerifiedStatus.setTextColor(colorDarkGreen)
                tvVerifiedStatus.setOnClickListener(null)
            } else {
                ivVerified.setImageResource(R.drawable.ic_pending); ivVerified.setColorFilter(colorGray)
                tvVerifiedStatus.text = "Verify Now"; tvVerifiedStatus.setTextColor(colorRed)
                tvVerifiedStatus.setOnClickListener { showVerificationDialog("phone") }
            }

            if (it.emailVerified == 1) {
                ivVerifiedEmail.setImageResource(R.drawable.ic_tick); ivVerifiedEmail.setColorFilter(colorDarkGreen)
                tvVerifiedStatusEmail.text = "Verified"; tvVerifiedStatusEmail.setTextColor(colorDarkGreen)
                tvVerifiedStatusEmail.setOnClickListener(null)
            } else {
                ivVerifiedEmail.setImageResource(R.drawable.ic_pending); ivVerifiedEmail.setColorFilter(colorGray)
                tvVerifiedStatusEmail.text = "Verify Now"; tvVerifiedStatusEmail.setTextColor(colorRed)
                tvVerifiedStatusEmail.setOnClickListener { showVerificationDialog("email") }
            }
            
            tvAccountSince.text = it.createdAt ?: "N/A"
        }
    }

    private fun showVerificationDialog(type: String) {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Verify ${if (type == "phone") "Mobile" else "Email"}")
        
        val input = EditText(requireContext())
        input.hint = "Enter 4-digit OTP (Mock: 1234)"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        builder.setView(input)

        builder.setPositiveButton("Verify") { _, _ ->
            if (input.text.toString() == "1234") {
                performVerification(type)
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
        val updatedUser = user?.copy(
            firstName = etFirstName.text.toString(),
            lastName = etLastName.text.toString(),
            email = etEmail.text.toString(),
            phone = etPhone.text.toString(),
            addressLine1 = etAddressLine1.text.toString(),
            addressLine2 = etAddressLine2.text.toString(),
            city = etCity.text.toString(),
            state = etState.text.toString(),
            zipCode = etZipCode.text.toString()
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
