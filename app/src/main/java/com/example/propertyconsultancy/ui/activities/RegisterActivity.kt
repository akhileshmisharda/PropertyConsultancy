package com.example.propertyconsultancy.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.RegisterRequest
import com.example.propertyconsultancy.data.local.SessionManager
import com.example.propertyconsultancy.data.remote.RetrofitInstance
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch

class RegisterActivity : BaseActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var registerProgress: LinearProgressIndicator
    
    // Firebase Phone Auth
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var isPhoneVerified = false
    
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var rgRole: RadioGroup
    private lateinit var tvVerifyStatus: TextView
    private lateinit var ivVerified: ImageView
    
    private lateinit var layoutOtp: View
    private lateinit var etOtp: EditText
    private lateinit var btnVerifyOtp: View
    private lateinit var tvOtpCountdown: TextView
    private lateinit var btnResendOtp: View
    private lateinit var btnRegister: Button
    private var countDownTimer: android.os.CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        sessionManager = SessionManager(this)
        auth = FirebaseAuth.getInstance()
        
        initUI()
    }

    private fun initUI() {
        registerProgress = findViewById(R.id.registerProgress)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        rgRole = findViewById(R.id.rgRole)
        tvVerifyStatus = findViewById(R.id.tvVerifyStatus)
        ivVerified = findViewById(R.id.ivVerified)
        
        layoutOtp = findViewById(R.id.layoutOtp)
        etOtp = findViewById(R.id.etOtp)
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp)
        tvOtpCountdown = findViewById(R.id.tvOtpCountdown)
        btnResendOtp = findViewById(R.id.btnResendOtp)
        btnRegister = findViewById(R.id.btnRegister)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        tvVerifyStatus.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            if (phone.length < 10) {
                Toast.makeText(this, "Enter valid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            hideKeyboard()
            initiatePhoneVerification(phone)
        }

        btnVerifyOtp.setOnClickListener {
            val code = etOtp.text.toString().trim()
            if (code.length == 6) {
                hideKeyboard()
                verifyCode(code)
            } else {
                Toast.makeText(this, "Enter 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }

        btnResendOtp.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            initiatePhoneVerification(phone, isResend = true)
        }

        btnRegister.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirm = etConfirmPassword.text.toString().trim()
            val role = if (findViewById<RadioButton>(R.id.rbLandlord).isChecked) "landlord" else "tenant"

            if (phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!isPhoneVerified) {
                Toast.makeText(this, "Please verify your mobile number first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performRegister(phone, password, role)
        }
    }

    private fun initiatePhoneVerification(phone: String, isResend: Boolean = false) {
        var cleanedPhone = phone.replace(" ", "").replace("-", "")
        if (cleanedPhone.startsWith("00")) {
            cleanedPhone = "+" + cleanedPhone.substring(2)
        }
        val formattedPhone = if (cleanedPhone.startsWith("+")) cleanedPhone else "+91$cleanedPhone" 
        
        registerProgress.visibility = View.VISIBLE
        
        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    registerProgress.visibility = View.GONE
                    credential.smsCode?.let { etOtp.setText(it) }
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    registerProgress.visibility = View.GONE
                    Toast.makeText(this@RegisterActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    registerProgress.visibility = View.GONE
                    verificationId = id
                    resendToken = token
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
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val view = currentFocus ?: View(this)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun verifyCode(code: String) {
        val id = verificationId ?: return
        val credential = PhoneAuthProvider.getCredential(id, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        registerProgress.visibility = View.VISIBLE
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    isPhoneVerified = true
                    layoutOtp.visibility = View.GONE
                    countDownTimer?.cancel()
                    
                    tvVerifyStatus.text = "Verified"
                    tvVerifyStatus.setTextColor(ContextCompat.getColor(this, R.color.modern_primary))
                    ivVerified.visibility = View.VISIBLE
                    tvVerifyStatus.setOnClickListener(null)
                    btnRegister.isEnabled = true
                    
                    Toast.makeText(this, "Phone Number Verified", Toast.LENGTH_SHORT).show()
                    registerProgress.visibility = View.GONE
                } else {
                    registerProgress.visibility = View.GONE
                    Toast.makeText(this, "Incorrect OTP", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun performRegister(phone: String, password: String, role: String) {
        registerProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val request = RegisterRequest(
                    phone = phone,
                    password = password,
                    role = role,
                    mobileVerified = 1
                )
                
                val response = RetrofitInstance.api.register(request)
                if (response.status == "success" && response.user != null) {
                    sessionManager.saveUser(response.user)
                    Toast.makeText(this@RegisterActivity, "Account Created! Please complete your profile.", Toast.LENGTH_LONG).show()
                    val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                    intent.putExtra("OPEN_PROFILE", true)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@RegisterActivity, response.message, Toast.LENGTH_SHORT).show()
                    registerProgress.visibility = View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                registerProgress.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}