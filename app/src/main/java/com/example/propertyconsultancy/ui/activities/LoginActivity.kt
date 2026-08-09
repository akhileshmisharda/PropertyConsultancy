package com.example.propertyconsultancy.ui.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.LoginRequest
import com.example.propertyconsultancy.data.local.SessionManager
import com.example.propertyconsultancy.data.remote.RetrofitInstance
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var phoneBoxes: List<EditText>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sessionManager = SessionManager(this)
        
        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        phoneBoxes = listOf(
            findViewById(R.id.etPhone1), findViewById(R.id.etPhone2), findViewById(R.id.etPhone3),
            findViewById(R.id.etPhone4), findViewById(R.id.etPhone5), findViewById(R.id.etPhone6),
            findViewById(R.id.etPhone7), findViewById(R.id.etPhone8), findViewById(R.id.etPhone9),
            findViewById(R.id.etPhone10)
        )

        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnToRegister = findViewById<Button>(R.id.btnToRegister)

        setupPhoneLogic()

        btnLogin.setOnClickListener {
            val phone = getPhoneFromBoxes()
            val password = etPassword.text.toString().trim()

            if (phone.length == 10 && password.isNotEmpty()) {
                performLogin(phone, password)
            } else if (phone.length != 10) {
                Toast.makeText(this, "Please enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
            }
        }

        btnToRegister.setOnClickListener {
            com.example.propertyconsultancy.ui.dialogs.RegisterDialogFragment().show(supportFragmentManager, "register")
        }
    }

    private fun setupPhoneLogic() {
        phoneBoxes.forEachIndexed { index, box ->
            box.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && index < phoneBoxes.size - 1) {
                        phoneBoxes[index + 1].requestFocus()
                    }
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

    private fun getPhoneFromBoxes(): String {
        return phoneBoxes.joinToString("") { it.text.toString() }
    }

    private fun performLogin(phone: String, password: String) {
        Log.d("[php_debug]", "performLogin: Attempting login for $phone")
        
        lifecycleScope.launch {
            try {
                val request = LoginRequest(phone, password)
                val response = RetrofitInstance.api.login(request)
                
                Log.d("[php_debug]", "performLogin: Server response: $response")
                
                if (response.status == "success" && response.user != null) {
                    sessionManager.saveUser(response.user)
                    Toast.makeText(this@LoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, response.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("[php_debug]", "performLogin API Error: ${e.message}")
                Toast.makeText(this@LoginActivity, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
