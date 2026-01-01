package org.example.project

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.example.project.components.Login
import org.example.project.components.SignIn
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.example.project.components.ErrorPage
import org.example.project.components.NewPost
import org.example.project.components.UserProfile

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            App(context)
        }
    }
}

@Composable
fun App(context: Context) {
    val navcontroller: NavHostController = rememberNavController()
    val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    val prefs = EncryptedSharedPreferences.create(
        context,"auth_prefs",masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    NavHost(navController = navcontroller, startDestination = "/login") {
        composable(route = "/login") {
            Login(context,navcontroller)
        }
        composable(route="/signin") {
            SignIn(context, navcontroller)
        }
        composable(route="/userprofile") {
            UserProfile(context, navController = navcontroller)
        }
        composable(route="/error") {
            ErrorPage(navcontroller)
        }
        composable(route="/new-post") {
            NewPost(context,navcontroller)
        }
    }
}