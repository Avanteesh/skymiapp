package org.example.project.components

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.example.project.BuildConfig
import org.example.project.datamodels.AuthToken
import org.example.project.datamodels.ServerResponse
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.datamodels.PostAPIClient


class LoginApiClient(private var apiClient: OkHttpClient) : PostAPIClient {
    override suspend fun sendData(json: String): Result<ServerResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${BuildConfig.BACKEND_URL.trim()}/api/login").post(
                json.toRequestBody("application/json; charset=utf-8".toMediaType())
            ).addHeader("Content-Type", "application/json").build()
            apiClient.newCall(request = request).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext Result.success(ServerResponse(status=200, responseBody = response.body.string()))
                }
                return@withContext Result.failure(
                    IOException("HTTP ${response.code}")
                )
            }
        } catch (e : Exception)  {
            Result.failure(e)
        }
    }
}

class LoginUserModel(private val apiClient: LoginApiClient): ViewModel() {
    private val _response = MutableStateFlow<ServerResponse?>(null)
    val response: StateFlow<ServerResponse?> = _response
    fun sendRequest(json: String) {
        viewModelScope.launch {
            val result = apiClient.sendData(json)
            result.fold(
                onSuccess = { response ->
                    _response.value = response
                },
                onFailure = { error ->
                    Log.d("error: ${error.message}", "DEBUG")
                }
            )
        }
    }
}

@Composable
fun Login(context: Context, navController: NavController) {
    val httpClient = remember { OkHttpClient() }
    val apiClient = remember {LoginApiClient(httpClient)}
    val loginModel = remember { LoginUserModel(apiClient) }
    val serverResponse by loginModel.response.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    val prefs = EncryptedSharedPreferences.create(
        context,"auth_prefs",masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    if (prefs.getString("auth_token", null) != null)  {
        navController.navigate(("/userprofile"))
    }
    LaunchedEffect(serverResponse) {
        serverResponse?.let { response ->
            val decoded = Json.decodeFromString<AuthToken>(response.responseBody!!)
            prefs.edit { putString("auth_token", decoded.token) }
            navController.navigate("/userprofile")
        }
    }
    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Login", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = username, onValueChange = { username = it },
                placeholder = { Text("enter username...") },
                label = { Text("username") }
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                placeholder = { Text("enter password...") },
                label = { Text("password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    if (username != "") {
                        if (password.length > 8) {
                            loginModel.sendRequest("{\"username\": \"${username}\",\"password\": \"${password}\"}")
                        } else {
                            Toast.makeText(context, "Password must be more than 8 characters long!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Please fill username", Toast.LENGTH_SHORT).show()
                    }
                }, modifier = Modifier.fillMaxWidth(0.7f),
                colors = ButtonColors(
                    containerColor = Color(250, 30, 92),
                    disabledContainerColor = Color.Unspecified,
                    contentColor = Color.White,
                    disabledContentColor = Color.Unspecified
                )
            ) { Text("Login") }
            Spacer(modifier = Modifier.height(5.dp))
            Button(
                onClick = { navController.navigate("/signin") },
                modifier = Modifier.fillMaxWidth(0.7f),
                border = BorderStroke(2.dp, Color(252, 40, 100)),
                colors = ButtonColors(
                    containerColor = Color.Transparent,
                    disabledContentColor = Color.Unspecified,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.Unspecified
                )
            ) {
                Text("Create Account")
            }
        }
    }
}