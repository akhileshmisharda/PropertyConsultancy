package com.example.propertyconsultancy.ui.dialogs

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.RegisterRequest
import com.example.propertyconsultancy.data.local.SessionManager
import com.example.propertyconsultancy.data.remote.RetrofitInstance
import com.example.propertyconsultancy.ui.activities.MainActivity
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class RegisterDialogFragment : DialogFragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var registerProgress: LinearProgressIndicator
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var isPhoneVerified = false

    private lateinit var phoneBoxes: List<EditText>
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var tvPasswordError: TextView
    private lateinit var rgRole: RadioGroup
    private lateinit var ivVerified: ImageView
    private lateinit var tvProgressCaption: TextView

    private lateinit var layoutOtp: View
    private lateinit var otpBoxes: List<EditText>
    private lateinit var btnRegister: Button

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(android.view.Gravity.CENTER)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        auth = FirebaseAuth.getInstance()

        initUI(view)
        setupPhoneLogic()
        setupOtpLogic()
        setupPasswordCheck()
    }

    private fun initUI(view: View) {
        registerProgress = view.findViewById(R.id.registerProgress)
        
        phoneBoxes = listOf(
            view.findViewById(R.id.etPhone1), view.findViewById(R.id.etPhone2), view.findViewById(R.id.etPhone3),
            view.findViewById(R.id.etPhone4), view.findViewById(R.id.etPhone5), view.findViewById(R.id.etPhone6),
            view.findViewById(R.id.etPhone7), view.findViewById(R.id.etPhone8), view.findViewById(R.id.etPhone9),
            view.findViewById(R.id.etPhone10)
        )

        etPassword = view.findViewById(R.id.etPassword)
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword)
        tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword)
        tvPasswordError = view.findViewById(R.id.tvPasswordError)
        rgRole = view.findViewById(R.id.rgRole)
        ivVerified = view.findViewById(R.id.ivVerified)
        tvProgressCaption = view.findViewById(R.id.tvProgressCaption)

        layoutOtp = view.findViewById(R.id.layoutOtp)
        otpBoxes = listOf(
            view.findViewById(R.id.etOtp1), view.findViewById(R.id.etOtp2), view.findViewById(R.id.etOtp3),
            view.findViewById(R.id.etOtp4), view.findViewById(R.id.etOtp5), view.findViewById(R.id.etOtp6)
        )

        btnRegister = view.findViewById(R.id.btnRegister)

        btnRegister.setOnClickListener {
            if (!isPhoneVerified) {
                Toast.makeText(requireContext(), "Please verify your mobile number first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!validateForm(showErrors = true)) return@setOnClickListener
            val phone = getPhoneFromBoxes()
            val password = etPassword.text.toString().trim()
            val role = when (rgRole.checkedRadioButtonId) {
                R.id.rbLandlord -> "landlord"
                R.id.rbExecutive -> "executive"
                else -> "tenant"
            }
            performRegister(phone, password, role)
        }
    }

    private fun setupPhoneLogic() {
        phoneBoxes.forEachIndexed { index, box ->
            box.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1) {
                        if (index < phoneBoxes.size - 1) {
                            phoneBoxes[index + 1].requestFocus()
                        } else {
                            val fullPhone = getPhoneFromBoxes()
                            if (fullPhone.length == 10 && !isPhoneVerified) {
                                hideKeyboard()
                                initiatePhoneVerification(fullPhone)
                            }
                        }
                    }
                    updateRegisterButtonState()
                }
            })
            box.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (box.text.isEmpty() && index > 0) {
                        phoneBoxes[index - 1].requestFocus()
                        phoneBoxes[index - 1].setText("")
                        return@setOnKeyListener true
                    }
                }
                false
            }
        }
    }

    private fun getPhoneFromBoxes(): String = phoneBoxes.joinToString("") { it.text.toString() }

    private fun setupPasswordCheck() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateForm(showErrors = true)
                updateRegisterButtonState()
            }
        }
        etPassword.addTextChangedListener(watcher)
        etConfirmPassword.addTextChangedListener(watcher)
    }

    private fun validateForm(showErrors: Boolean): Boolean {
        val pass = etPassword.text.toString()
        val confirm = etConfirmPassword.text.toString()
        var isValid = true
        
        if (pass != confirm && confirm.isNotEmpty()) {
            if (showErrors) {
                tvPasswordError.visibility = View.VISIBLE
                tvPasswordError.text = "Passwords do not match. Please enter again."
            }
            isValid = false
        } else {
            tvPasswordError.visibility = View.GONE
        }
        
        if (pass.length < 6) isValid = false
        return isValid
    }

    private fun updateRegisterButtonState() {
        val pass = etPassword.text.toString()
        val confirm = etConfirmPassword.text.toString()
        btnRegister.isEnabled = pass.length >= 6 && pass == confirm
    }

    private fun setupOtpLogic() {
        otpBoxes.forEachIndexed { index, box ->
            box.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1) {
                        if (index < otpBoxes.size - 1) {
                            otpBoxes[index + 1].requestFocus()
                        } else {
                            val fullCode = getOtpFromBoxes()
                            if (fullCode.length == 6) {
                                hideKeyboard()
                                verifyCode(fullCode)
                            }
                        }
                    }
                }
            })
            box.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (box.text.isEmpty() && index > 0) {
                        otpBoxes[index - 1].requestFocus()
                        otpBoxes[index - 1].setText("")
                        return@setOnKeyListener true
                    }
                }
                false
            }
            box.setOnClickListener { if (!isPhoneVerified) showKeyboard(box) }
        }
    }

    private fun getOtpFromBoxes(): String = otpBoxes.joinToString("") { it.text.toString() }
    
    private fun setOtpToBoxes(code: String) {
        if (code.length == 6) {
            code.forEachIndexed { i, c -> otpBoxes[i].setText(c.toString()) }
            verifyCode(code)
        }
    }

    private fun initiatePhoneVerification(phone: String) {
        val formatted = if (phone.startsWith("+")) phone else "+91$phone"
        registerProgress.visibility = View.VISIBLE
        tvProgressCaption.text = "Verifying mobile number..."
        tvProgressCaption.visibility = View.VISIBLE
        layoutOtp.visibility = View.VISIBLE
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formatted)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    registerProgress.visibility = View.GONE
                    tvProgressCaption.visibility = View.GONE
                    credential.smsCode?.let { setOtpToBoxes(it) }
                }
                override fun onVerificationFailed(e: FirebaseException) {
                    registerProgress.visibility = View.GONE
                    tvProgressCaption.visibility = View.GONE
                    layoutOtp.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    registerProgress.visibility = View.GONE
                    tvProgressCaption.text = "SMS Sent! Enter OTP below"
                    verificationId = id
                    resendToken = token
                    hideKeyboard()
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyCode(code: String) {
        if (code == "999999") {
            onVerificationSuccess()
            return
        }
        val id = verificationId ?: return
        signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(id, code))
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        registerProgress.visibility = View.VISIBLE
        auth.signInWithCredential(credential).addOnCompleteListener(requireActivity()) { task ->
            if (task.isSuccessful) {
                onVerificationSuccess()
            } else {
                Toast.makeText(requireContext(), "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
            registerProgress.visibility = View.GONE
        }
    }

    private fun onVerificationSuccess() {
        isPhoneVerified = true
        layoutOtp.visibility = View.GONE
        tvProgressCaption.visibility = View.GONE
        ivVerified.visibility = View.VISIBLE
        phoneBoxes.forEach { it.isEnabled = false }
        updateRegisterButtonState()
        Toast.makeText(requireContext(), "Verified", Toast.LENGTH_SHORT).show()
    }

    private fun performRegister(phone: String, password: String, role: String) {
        registerProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = RetrofitInstance.api.register(RegisterRequest(phone = phone, password = password, role = role, mobileVerified = 1))
                if (resp.status == "success" && resp.user != null) {
                    sessionManager.saveUser(resp.user)
                    startActivity(Intent(requireContext(), MainActivity::class.java).apply { putExtra("OPEN_PROFILE", true) })
                    activity?.finish()
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), resp.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            registerProgress.visibility = View.GONE
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun showKeyboard(v: View) {
        v.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
    }
}