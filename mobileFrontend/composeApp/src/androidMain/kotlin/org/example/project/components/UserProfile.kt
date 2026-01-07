package org.example.project.components

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.placeholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import org.example.project.BuildConfig
import org.example.project.R
import org.example.project.datamodels.User


class LoadUserData(private var httpClient: OkHttpClient)  {
    suspend fun getData(token: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${BuildConfig.BACKEND_URL.trim()}/api/user/me").get()
                .addHeader("Authorization", "Bearer $token").build()
            httpClient.newCall(request = request).execute().use { response ->
                if (response.isSuccessful)  {
                    val usermodel: User = Json.decodeFromString<User>(response.body.string())
                    return@withContext Result.success(usermodel)
                }
                return@withContext Result.failure(IOException("${response.code}"))
            }
        } catch(e : Exception) {
            Result.failure(e)
        }
    }
}

class FetchUserData(private val userDataLoader: LoadUserData) : ViewModel() {
    private val _response = MutableStateFlow<User?>(null)
    val response: StateFlow<User?> = _response
    fun getData(token: String)  {
        viewModelScope.launch {
            val responseUser = userDataLoader.getData(token)
            responseUser.fold(
                onSuccess = { response ->
                    _response.value = response
                },
                onFailure = { error ->
                    if (error.message == "404")  {
                        _response.value = User(id="null",username ="",profilepic="")
                    }
                    Log.d("${error.message}", "DEBUG")
                }
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfile(context: Context, navController: NavController) {
    val httpClient = remember { OkHttpClient() }
    val loadUserData = remember { LoadUserData(httpClient) }
    val fetchUserData = remember { FetchUserData(loadUserData) }
    val userData by fetchUserData.response.collectAsState()
    val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    val prefs = EncryptedSharedPreferences.create(
        context,"auth_prefs",masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    LaunchedEffect(Unit) {
        val authToken = prefs.getString("auth_token", null)
        if (authToken == null)  {
            navController.navigate("/login")
        } else {
            fetchUserData.getData(authToken)
        }
    }

    if (userData != null)  {
        if (userData!!.id == "null") {
            navController.navigate("/login")
        }
        Scaffold(topBar = {
            TopAppBar(
                title={
                    Row(modifier=Modifier.fillMaxWidth().padding(8.dp)) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data("${BuildConfig.BACKEND_URL}/uploads/profile_pictures/${userData!!.profilepic}")
                                .crossfade(true).placeholder(R.drawable.ic_launcher_background).build(),
                            contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.clip(CircleShape).height(100.dp).width(100.dp)
                        )
                        Spacer(Modifier.height(20.dp))
                        Text("@${userData!!.username}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    } },
                colors = topAppBarColors(containerColor = Color.Transparent)
            ) }, bottomBar = {
                    BottomAppBar(contentColor = Color(194, 166, 237,255)) {
                        val buttonDim = 90.dp
                        val iconDim = 45.dp
                        IconButton(onClick = {},Modifier.height(buttonDim).width(buttonDim)) {
                            Icon(imageVector = Icons.Default.Person,contentDescription = null, tint = Color.Black,modifier=Modifier.height(iconDim).width(iconDim))
                        }
                        IconButton(onClick = {},Modifier.width(buttonDim).height(buttonDim)) {
                            Icon(imageVector = Icons.Default.Search,contentDescription = null,tint = Color.Black,modifier=Modifier.height(iconDim).width(iconDim))
                        }
                        IconButton(onClick = {},Modifier.width(buttonDim).height(buttonDim)) {
                            Icon(imageVector = Icons.Default.Settings,contentDescription = null,tint=Color.Black,modifier=Modifier.height(iconDim).width(iconDim))
                        }

                    }
                }, floatingActionButton = {
                    FloatingActionButton(onClick = {
                        navController.navigate("/new-post")
                    }, containerColor = Color(194, 225, 252)) {
                        Icon(imageVector = Icons.Default.Add,contentDescription = null,tint=Color.Black)
                    }
                }
        ) { padding ->
            Text("content", modifier = Modifier.padding(padding))
        }
    }
    else {
        Text("loading...")
    }
}