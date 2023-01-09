package com.alpharays.calendar.ui

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.text.method.HideReturnsTransformationMethod
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.alpharays.calendar.R
import com.alpharays.calendar.SplashScreenVM
import com.alpharays.calendar.databinding.ActivitySignInBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.SignInMethodQueryResult
import kotlin.system.exitProcess


class ActivitySignIn : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private val viewModel: SplashScreenVM by viewModels()
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var sharedPref: SharedPreferences

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            startActivity(Intent(this, ActivityHome::class.java))
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().apply {
            setKeepOnScreenCondition {
                viewModel.isLoading.value
            }
        }
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the activity to be in portrait mode
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAffinity()
                exitProcess(0)
            }
        })
        auth = FirebaseAuth.getInstance()


        binding.forgotPassword.setOnClickListener {
            val emailID = binding.emailID.text.toString()
            if (emailID.isEmpty()) {
                Toast.makeText(this, "Enter Email Address", Toast.LENGTH_SHORT).show()
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailID).matches()) {
                Toast.makeText(this, "Enter correct Email Address", Toast.LENGTH_SHORT).show()
            } else {
                auth.sendPasswordResetEmail(emailID)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Email sent", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        binding.passwordCheck.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!isChecked) {
                binding.password.transformationMethod = PasswordTransformationMethod.getInstance()
            } else {
                binding.password.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
            }
            binding.password.setSelection(binding.password.text.length)
        }

        val url = "https://www.freeprivacypolicy.com/live/6afbae86-1c20-4d45-bc6f-b95f565a9221"
        // new url

        binding.okPrivacyPolicyOk.text = Html.fromHtml(
            "By clicking here, I state that I have read and understood the " +
                    "<a href=\"$url\">privacy policy</a>. ",
            Html.FROM_HTML_MODE_LEGACY
        )
        binding.okPrivacyPolicyOk.movementMethod = LinkMovementMethod.getInstance()

        binding.googleSignInBtn.text = Html.fromHtml(
            "<strong><font color=${Color.argb(1, 66, 133, 244)}>G</font></strong>" +
                    "<strong><font color=${Color.argb(1, 219, 68, 55)}>o</font></strong>" +
                    "<strong><font color=${Color.argb(1, 244, 180, 0)}>o</font></strong>" +
                    "<strong><font color=${Color.argb(1, 66, 133, 244)}>g</font></strong>" +
                    "<strong><font color=${Color.argb(1, 15, 157, 88)}>l</font></strong>" +
                    "<strong><font color=${
                        Color.argb(
                            1,
                            219,
                            68,
                            55
                        )
                    }>e</font></strong>" + " Sign In", Html.FROM_HTML_MODE_LEGACY
        )

        googleAuth()
        emailAuth()
    }

    // ************************************************* GOOGLE AUTH *************************************************
    private fun googleAuth() {
        binding.googleSignInBtn.setOnClickListener {
            // auth google only when gBtn is clicked
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
            // Client Ready
            val check: CheckBox = binding.okPrivacyPolicyOk
            if (check.isChecked) signInGoogle()
            else Toast.makeText(this, "Please check privacy policy", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleResults(task)
            }
        }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            val account: GoogleSignInAccount? = task.result
            if (account != null) {
                updateUI(account)
            }
        } else {
            Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                val intent = Intent(this, ActivityHome::class.java)
                sharedPref = getSharedPreferences("sharingUSER", MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putString("email", account.email)
                editor.putString("displayName", account.displayName)
                editor.apply()
                googleSignInClient.signOut()
                Toast.makeText(
                    this,
                    Html.fromHtml("<strong>Welcome</strong>", Html.FROM_HTML_MODE_LEGACY),
                    Toast.LENGTH_SHORT
                )
                    .show()
                finish()
                startActivity(intent)
            } else {
                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }


    // ************************************************* EMAIL AUTH *************************************************
    private fun emailAuth() {
        binding.loginBtn.setOnClickListener {
            val check: CheckBox = binding.okPrivacyPolicyOk
            // auth email only when loginBtn is clicked
            val userEmailAddress = binding.emailID.text.toString()
            var userPassword = binding.password.text.toString()
            var userName = binding.userNameRqd.text.toString()
            userPassword = reorderPassword(userPassword)
            Log.i(userPassword, userPassword.length.toString())
            userName = reorderStrings(userName)
            if (!checkStrings(userName)) {
                Toast.makeText(this, "User Name can not be empty", Toast.LENGTH_SHORT).show()
            } else if (userPassword.isEmpty() || userEmailAddress.isEmpty() || userName.isEmpty()) {
                Toast.makeText(this, "Email or Password can not be empty", Toast.LENGTH_SHORT)
                    .show()
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(userEmailAddress).matches()) {
                Toast.makeText(this, "Email is not correct", Toast.LENGTH_SHORT).show()
            } else if (userPassword.length < 6) {
                Toast.makeText(this, "Password should be at-least of 6 length", Toast.LENGTH_SHORT)
                    .show()
            } else if (!check.isChecked) {
                Toast.makeText(this, "Please check privacy policy", Toast.LENGTH_SHORT).show()
            } else {
                Log.i("Email Auth Successful", "STATUS: OK")
                signInEmail(userEmailAddress, userPassword, userName)
            }
        }
    }

    private fun reorderPassword(userPassword: String): String {
        var s = ""
        for (u in userPassword) {
            if (u != ' ') {
                s += u;
            }
        }
        return s
    }

    private fun reorderStrings(name: String): String {
        val ls = name.split(" ")
        var s = ""
        for (each in ls) {
            s += "$each "
        }
        return s
    }

    private fun checkStrings(userName: String): Boolean {
        if (userName.isEmpty()) return false
        for (i in userName.indices) {
            if (userName[i] != ' ') {
                Log.i("OK char", i.toString())
                return true
            } else {
                Log.i("NULL char", i.toString())
            }
        }
        return false
    }

    private fun signInEmail(userEmailAddress: String, userPassword: String, userName: String) {
        auth.fetchSignInMethodsForEmail(userEmailAddress)
            .addOnCompleteListener(OnCompleteListener<SignInMethodQueryResult> { task ->
                val isNewUser = task.result.signInMethods!!.isEmpty()
                if (isNewUser) {
                    newUser(userEmailAddress, userPassword, userName)
                    Log.e("TAG", "Is New User!")
                } else {
                    oldUser(userEmailAddress, userPassword, userName)
                    Log.e("TAG", "Is Old User!")
                }
            })
            .addOnFailureListener {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun oldUser(userEmailAddress: String, userPassword: String, userName: String) {
        auth.signInWithEmailAndPassword(userEmailAddress, userPassword)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val intent = Intent(this, ActivityHome::class.java)
                    sharedPref = getSharedPreferences("sharingUSER", MODE_PRIVATE)
                    val editor = sharedPref.edit()
                    editor.putString("email", userEmailAddress)
                    editor.putString("displayName", userName)
                    editor.apply()
                    Toast.makeText(
                        this,
                        Html.fromHtml("<strong>Welcome</strong>", Html.FROM_HTML_MODE_LEGACY),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    startActivity(intent)
                    Log.i("1", "SIGN IN : SUCCESS")
                } else {
                    Toast.makeText(this, it.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun newUser(userEmailAddress: String, userPassword: String, userName: String) {
        auth.createUserWithEmailAndPassword(userEmailAddress, userPassword)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val intent = Intent(this, ActivityHome::class.java)
                    sharedPref = getSharedPreferences("sharingUSER", MODE_PRIVATE)
                    val editor = sharedPref.edit()
                    editor.putString("email", userEmailAddress)
                    editor.putString("displayName", userName)
                    editor.apply()
                    Toast.makeText(
                        this,
                        Html.fromHtml("<strong>Welcome</strong>", Html.FROM_HTML_MODE_LEGACY),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    startActivity(intent)
                    Log.i("1", "Registered and Sign In : SUCCESS")
                } else {
                    Toast.makeText(this, it.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }
}