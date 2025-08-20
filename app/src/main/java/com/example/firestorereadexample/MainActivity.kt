package com.example.firestorereadexample

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.firestorereadexample.ui.theme.FirestoreReadExampleTheme
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FirestoreReadExampleTheme {
                FirestoreReadExampleApp()
            }
        }
    }
}

val TAG = "ReadBenchmark"

val auth: FirebaseAuth = FirebaseAuth.getInstance()
val data
    get() = FirebaseFirestore.getInstance()

val FunctionToTest: () -> Unit = { testLargeCollection() }

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@PreviewScreenSizes
@Composable
fun FirestoreReadExampleApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val settings = firestoreSettings {
        // Use persistent disk cache (default)
        setLocalCacheSettings(persistentCacheSettings {
            setSizeBytes(CACHE_SIZE_UNLIMITED)
        })
    }
    data.firestoreSettings = settings

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
            LoginComposable()
        }
    }
    CheckLifecycle {
        if (auth.currentUser != null) {
            FunctionToTest()
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    FAVORITES("Favorites", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun LoginComposable() {
    val mirrorLoginLauncher =
        rememberLauncherForActivityResult(contract = FirebaseAuthUIActivityResultContract()) { result ->
            if (result.resultCode == RESULT_OK) {
                FunctionToTest()
            }
        }
    if (auth.currentUser == null) {
        LaunchedEffect(Unit) {
            val providers = arrayListOf(
                AuthUI.IdpConfig.GoogleBuilder().build()
            )
            // Create and launch sign-in intent
            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
            mirrorLoginLauncher.launch(signInIntent)
        }
    }

}

fun testLargeCollection() {
    val userRef = if (auth.currentUser != null) data.collection("users")
        .document(auth.currentUser!!.uid) else null

    val transactionRef = userRef?.collection("transactions")

    Log.d(TAG, "Creating query")
    transactionRef?.addSnapshotListener { querySnapshot: QuerySnapshot?, exception: FirebaseFirestoreException? ->
        if (exception != null) {
            Log.i(TAG, "caught exception", exception)
        } else {
            Log.i(TAG, "Received ${querySnapshot?.size()} documents")
        }
    }

}

fun testSmallCollection() {
    val userRef = if (auth.currentUser != null) data.collection("users")
        .document(auth.currentUser!!.uid) else null

    val transactionRef = userRef?.collection("accounts")
    Log.i(TAG, "Creating query")
    transactionRef?.addSnapshotListener { querySnapshot: QuerySnapshot?, exception: FirebaseFirestoreException? ->
        if (exception != null) {
            Log.e(TAG, "caught exception", exception)
        } else {
            Log.i(TAG, "Received ${querySnapshot?.size()} documents")
        }
    }
}

var skipPossibleFirstOnResume = auth.currentUser == null

@Composable
private fun CheckLifecycle(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    LaunchedEffect(lifecycleState) {
        when (lifecycleState) {
            Lifecycle.State.RESUMED -> {
                if (!skipPossibleFirstOnResume) {
                    onResume()
                }
                skipPossibleFirstOnResume = false
            }

            else -> {
                // no-op
            }
        }

    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FirestoreReadExampleTheme {
        Greeting("Android")
    }
}