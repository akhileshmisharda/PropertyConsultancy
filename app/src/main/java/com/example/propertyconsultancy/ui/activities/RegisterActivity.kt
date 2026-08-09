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
import com.google.firebase.auth.*
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.RegisterRequest
import com.example.propertyconsultancy.data.local.SessionManager
import com.example.propertyconsultancy.data.remote.RetrofitInstance
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class RegisterActivity : BaseActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var registerProgress: LinearProgressIndicator
    
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var isPhoneVerified = false
    
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var rgRole: RadioGroup
    private lateinit var ivVerified: ImageView
    private lateinit var tvProgressCaption: TextView
    private lateinit var llPhoneIndicator: LinearLayout
    
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
        setupPhoneAutoVerify()
        setupPasswordCheck()
    }

    private fun initUI() {
        registerProgress = findViewById(R.id.registerProgress)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        rgRole = findViewById(R.id.rgRole)
        ivVerified = findViewById(R.id.ivVerified)
        tvProgressCaption = findViewById(R.id.tvProgressCaption)
        llPhoneIndicator = findViewById(R.id.llPhoneIndicator)
        
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
            if (phone.length == 10) initiatePhoneVerification(phone, isResend = true)
        }

        btnRegister.setOnClickListener {
            if (!validateForm(showErrors = true)) return@setOnClickListener

            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val role = when (rgRole.checkedRadioButtonId) {
                R.id.rbLandlord -> "landlord"
                R.id.rbExecutive -> "executive"
                else -> "tenant"
            }

            performRegister(phone, password, role)
        }
    }

    private fun setupPhoneAutoVerify() {
        etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                updatePhoneIndicator(input.length)
                
                if (input.length == 10 && !isPhoneVerified) {
                    hideKeyboard()
                    initiatePhoneVerification(input)
                }
                updateRegisterButtonState()
            }
        })
    }

    private fun setupPasswordCheck() {
        val passwordWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateForm(showErrors = false)
                updateRegisterButtonState()
            }
        }
        etPassword.addTextChangedListener(passwordWatcher)
        etConfirmPassword.addTextChangedListener(passwordWatcher)
    }

    private fun validateForm(showErrors: Boolean = false): Boolean {
        val pass = etPassword.text.toString()
        val confirm = etConfirmPassword.text.toString()
        var isValid = true

        if (pass != confirm && confirm.isNotEmpty()) {
            if (showErrors) tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        } else {
            tilConfirmPassword.error = null
        }

        if (pass.length < 6 && pass.isNotEmpty()) {
            isValid = false
        }

        if (!isPhoneVerified) {
            isValid = false
        }

        return isValid
    }

    private fun updateRegisterButtonState() {
        btnRegister.isEnabled = isPhoneVerified && 
                etPassword.text.isNotEmpty() && 
                etPassword.text.toString() == etConfirmPassword.text.toString() &&
                etPassword.text.length >= 6
    }

    private fun updatePhoneIndicator(length: Int) {
        val activeColor = ContextCompat.getColor(this, R.color.modern_primary)
        val inactiveColor = android.graphics.Color.parseColor("#E0E0E0")
        
        var dotIndex = 0
        for (i in 0 until llPhoneIndicator.childCount) {
            val view = llPhoneIndicator.getChildAt(i)
            if (view.layoutParams.width > 5) {
                view.setBackgroundColor(if (dotIndex < length) activeColor else inactiveColor)
                dotIndex++
            }
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
            
            editText.setOnClickListener {
                if (!isPhoneVerified) {
                    showKeyboard(editText)
                }
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
        val formattedPhone = if (phone.startsWith("+")) phone else "+91$phone" 
        
        registerProgress.visibility = View.VISIBLE
        tvProgressCaption.text = "Verifying mobile number..."
        tvProgressCaption.visibility = View.VISIBLE
        layoutOtp.visibility = View.VISIBLE
        
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
                    layoutOtp.visibility = View.GONE
                    Toast.makeText(this@RegisterActivity, "Verification Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    registerProgress.visibility = View.GONE
                    tvProgressCaption.text = "SMS Sent! Enter OTP below"
                    verificationId = id
                    resendToken = token
                    hideKeyboard()
                    startCountdown()
                }
            })
            
        val token = resendToken
        if (isResend && token != null) {
            builder.setForceResendingToken(token)
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

    private fun showKeyboard(view: View) {
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        view.requestFocus()
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
            .addOnCompleteListener(this) { task: com.google.android.gms.tasks.Task<AuthResult> ->
                if (task.isSuccessful) {
                    isPhoneVerified = true
                    layoutOtp.visibility = View.GONE
                    tvProgressCaption.visibility = View.GONE
                    llPhoneIndicator.visibility = View.GONE
                    countDownTimer?.cancel()
                    
                    ivVerified.visibility = View.VISIBLE
                    etPhone.isEnabled = false 
                    updateRegisterButtonState()
                    
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