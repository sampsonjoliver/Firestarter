package com.sampsonjoliver.firestarter

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.*
import com.sampsonjoliver.firestarter.service.SessionManager
import com.sampsonjoliver.firestarter.utils.IntentUtils
import com.sampsonjoliver.firestarter.utils.TAG
import kotlinx.android.synthetic.main.activity_login.*

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity() {
    val callbackManager: CallbackManager by lazy { CallbackManager.Factory.create() }
    val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val firebaseAuthListener = OnCompleteListener<AuthResult> { task ->
        Log.d(TAG, "firebase:onComplete:" + task.isSuccessful);

        if (!task.isSuccessful) {
            showSignInFailed(task.exception)
        } else {
            // The success will propagate to the AuthStateListener
        }
    }

    val facebookAuthListener = object : FacebookCallback<LoginResult> {
        override fun onSuccess(result: LoginResult) {
            Log.d(TAG, "facebook:onSuccess: " + result);
            onFacebookAccountSignin(result.accessToken);
        }

        override fun onCancel() {
            Log.d(TAG, "facebook:onCancel");
        }

        override fun onError(error: FacebookException?) {
            Log.d(TAG, "facebook:onError", error);
        }
    }

    val googleAuthFailedListener = GoogleApiClient.OnConnectionFailedListener { connResult ->
        Log.d(TAG, "google:authFailed: " + connResult.errorMessage)
    }

    val authStateListener: FirebaseAuth.AuthStateListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        if (user != null) {
            // User is signed in
            Log.d(TAG, "onAuthStateChanged:signed_in:" + user.uid);
            SessionManager.setUserDetails(this@LoginActivity, user)
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // User is signed out
            Log.d(TAG, "onAuthStateChanged:signed_out");
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        initGoogleAuth()
        initFacebookAuth()
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        firebaseAuth.removeAuthStateListener(authStateListener);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            IntentUtils.RC_SIGN_IN -> handleGoogleSignIn(Auth.GoogleSignInApi.getSignInResultFromIntent(data))
        }
    }

    fun initFacebookAuth() {
        facebookLogin.setReadPermissions("public_profile", "email")
        facebookLogin.registerCallback(callbackManager, facebookAuthListener)
    }

    fun initGoogleAuth() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        val googleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, googleAuthFailedListener)
                .addApi(Auth.GOOGLE_SIGN_IN_API, signInOptions)
                .build();

        googleLogin.setScopes(signInOptions.scopeArray)
        googleLogin.setOnClickListener {
            val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
            startActivityForResult(signInIntent, IntentUtils.RC_SIGN_IN);
        }
    }

    fun handleGoogleSignIn(result: GoogleSignInResult) {
        Log.d(TAG, "handleSignInResult: ${result.isSuccess}");
        if (result.isSuccess) {
            result.signInAccount?.let { account ->
                onGoogleAccountSignin(account)
            }
        } else {
            // Signed out, show unauthenticated UI.
            showSignInFailed()
        }
    }

    fun onGoogleAccountSignin(account: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle: ${account.id}");
        signinWithFirebase(GoogleAuthProvider.getCredential(account.idToken, null))
    }

    fun onFacebookAccountSignin(token: AccessToken) {
        Log.d(TAG, "handleFacebookAccessToken: $token")
        signinWithFirebase(FacebookAuthProvider.getCredential(token.token))
    }

    fun signinWithFirebase(credential: AuthCredential) {
        Log.d(TAG, "signinWithFirebase")
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, firebaseAuthListener)
    }

    fun showSignInFailed(exception: Exception? = null) {
        if (exception != null)
            Log.w(TAG, "signInWithCredential", exception);

        Snackbar.make(facebookLogin, "Authentication failed.", Snackbar.LENGTH_SHORT).show()
    }
}
