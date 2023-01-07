package com.alpharays.calendar.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
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
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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
        val url = "https://www.freeprivacypolicy.com/live/6afbae86-1c20-4d45-bc6f-b95f565a9221"

        binding.privacyPolicy.text = Html.fromHtml(
            "By clicking here, I state that I have read and understood the " +
                    "<a href=\"$url\">privacy policy</a>. ",
            Html.FROM_HTML_MODE_LEGACY
        )
        binding.privacyPolicy.movementMethod = LinkMovementMethod.getInstance()

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

        auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)


        binding.googleSignInBtn.setOnClickListener {
            val check: CheckBox = binding.okPrivacyPolicyOk
            if(check.isChecked) signInGoogle()
            else Toast.makeText(this,"Please check privacy policy",Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, Html.fromHtml("<strong>Welcome</strong>",Html.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT)
                    .show()
                finish()
                startActivity(intent)
            } else {
                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }
}