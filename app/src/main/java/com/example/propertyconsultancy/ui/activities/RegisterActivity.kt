package com.example.propertyconsultancy.ui.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.AuthResult
import com.google.android.gms.tasks.Task
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.RegisterRequest
import com.example.propertyconsultancy.data.local.SessionManager
import com.example.propertyconsultancy.data.remote.RetrofitInstance
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

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
    private lateinit var tvProgressCaption: TextView
    
    private lateinit var layoutOtp: View
    private lateinit var otpBoxes: List<EditText>
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
        setupOtpLogic()
    }

    private fun initUI() {
        registerProgress = findViewById(R.id.registerProgress)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        rgRole = findViewById(R.id.rgRole)
        tvVerifyStatus = findViewById(R.id.tvVerifyStatus)
        ivVerified = findViewById(R.id.ivVerified)
        tvProgressCaption = findViewById(R.id.tvProgressCaption)
        
        layoutOtp = findViewById(R.id.layoutOtp)
        otpBoxes = listOf(
            findViewById(R.id.etOtp1), findViewById(R.id.etOtp2), findViewById(R.id.etOtp3),
            findViewById(R.id.etOtp4), findViewById(R.id.etOtp5), findViewById(R.id.etOtp6)
        )
        
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
            tvProgressCaption.text = "Checking for robot..."
            tvProgressCaption.visibility = View.VISIBLE
            initiatePhoneVerification(phone)
        }

        btnVerifyOtp.setOnClickListener {
            val code = getOtpFromBoxes()
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
            
            val role = when (rgRole.checkedRadioButtonId) {
                R.id.rbLandlord -> "landlord"
                R.id.rbExecutive -> "executive"
                else -> "tenant"
            }

            if (phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!isPhoneVerified) {
                Toast.makeText(this, "First make mobile verification", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performRegister(phone, password, role)
        }
    }

    private fun setupOtpLogic() {
        otpBoxes.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && index < otpBoxes.size - 1) {
                        otpBoxes[index + 1].requestFocus()
                    }
                }
            })

            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (editText.text.isEmpty() && index > 0) {
                        otpBoxes[index - 1].requestFocus()
                        otpBoxes[index - 1].setText("")
                        return@setOnKeyListener true
                    }
                }
                false
            }
        }
    }

    private fun getOtpFromBoxes(): String {
        return otpBoxes.joinToString("") { it.text.toString() }
    }

    private fun setOtpToBoxes(code: String) {
        if (code.length == 6) {
            code.forEachIndexed { index, char ->
                otpBoxes[index].setText(char.toString())
            }
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
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    registerProgress.visibility = View.GONE
                    tvProgressCaption.visibility = View.GONE
                    credential.smsCode?.let { setOtpToBoxes(it) }
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    registerProgress.visibility = View.GONE
                    tvProgressCaption.visibility = View.GONE
                    Toast.makeText(this@RegisterActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    registerProgress.visibility = View.GONE
                    tvProgressCaption.text = "SMS Sent! Enter OTP"
                    verificationId = id
                    resendToken = token
                    layoutOtp.visibility = View.VISIBLE
                    
                    otpBoxes[0].requestFocus()
                    showKeyboard(otpBoxes[0])
                    
                    startCountdown()
                }
            })
            
        val token = resendToken
        if (isResend && token != null) {
            builder.setForceResendingToken(token)
        }
        
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (tvProgressCaption.visibility == View.VISIBLE && tvProgressCaption.text.contains("robot")) {
                tvProgressCaption.text = "Sending SMS..."
            }
        }, 2000)
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

    private fun showKeyboard(view: View) {
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun verifyCode(code: String) {
        val id = verificationId ?: return
        val credential = PhoneAuthProvider.getCredential(id, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        registerProgress.visibility = View.VISIBLE
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task: Task<AuthResult> ->
                if (task.isSuccessful) {
                    isPhoneVerified = true
                    layoutOtp.visibility = View.GONE
                    tvProgressCaption.visibility = View.GONE
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