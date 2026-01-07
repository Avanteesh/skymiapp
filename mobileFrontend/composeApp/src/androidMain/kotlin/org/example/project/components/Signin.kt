package org.example.project.components

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import okhttp3.Request
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.example.project.BuildConfig
import org.example.project.R
import java.io.IOException


fun sendDateToServer(context: Context, username: String, email: String, password: String, uri: Uri): Boolean {
    val inputStream = context.contentResolver.openInputStream(uri) ?: error("Failed to load!!")
    val bytes = inputStream.readBytes()
    val multipartImage: RequestBody = bytes.toRequestBody(contentType = "image/*".toMediaType())
    val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("username",username)
            .addFormDataPart("email", email).addFormDataPart("password", password)
            .addFormDataPart("profile_picture", "profile_picture.jpg", multipartImage).build()
    val request = Request.Builder().url("${BuildConfig.BACKEND_URL.trim()}/api/signin").post(requestBody)
        .addHeader("Content-Type", "application/json").build()
    val client = OkHttpClient()
    var status = false
    client.newCall(request = request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("Error occurred: ${e.message}", "ERROR")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 201) {
                    status = true
                }
            }
    })
    return status
}


@Composable
fun SignIn(context: Context, navController: NavController) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailIsValid by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }
    var loadedProfilePicture by remember { mutableStateOf<Uri?>(null)}
    val scope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> loadedProfilePicture = uri }
    Box(modifier = Modifier) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("sign up", fontSize = 60.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(40.dp))
            AsyncImage(
               model = ImageRequest.Builder(LocalContext.current).data(loadedProfilePicture).crossfade(true).build(),
               placeholder = painterResource(R.drawable.ic_launcher_background),
               contentDescription = null,
               contentScale = ContentScale.Crop,
               modifier = Modifier.clip(CircleShape).height(140.dp).width(140.dp).border(5.dp, Color.Black)
            )
            IconButton(onClick = {imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},
                modifier = Modifier.height(70.dp).width(70.dp)) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier= Modifier.height(40.dp).width(40.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = username, onValueChange = { username = it },
                placeholder = {Text("enter username...")},
                label = {Text("username")}
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = email, onValueChange = {
                    email = it
                    emailIsValid = (email == "" || Patterns.EMAIL_ADDRESS.matcher(email).matches())
                },
                placeholder = {Text("enter email...")},
                isError = !emailIsValid,
                label = {Text("email")}
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value=password, onValueChange = { password = it },
                placeholder = {Text("enter password...")},
                label = {Text("password")},
                visualTransformation = if (!passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick={passwordVisible = !passwordVisible}) {
                        Icon(imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,contentDescription = null)
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick={
                if (emailIsValid && email != "" && password.length > 8 && username != "" && loadedProfilePicture != null) {
                    scope.launch {
                        val status = sendDateToServer(context = context, username,email,password,(loadedProfilePicture as Uri))
                        if (status) {
                            Toast.makeText(context, "signed successfully", Toast.LENGTH_SHORT).show()
                            navController.navigate("/login")
                        } else {
                            navController.navigate("/error")
                        }
                    }
                } else {
                    Toast.makeText(context, "All fields are compulsory and must be valid!", Toast.LENGTH_SHORT).show()
                }
            }, modifier = Modifier.fillMaxWidth(0.7f),
                colors= ButtonColors(containerColor = Color(111, 130, 252), disabledContainerColor = Color.Unspecified, contentColor = Color.White, disabledContentColor = Color.Unspecified)) {
                Text("Create Account")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = {navController.navigate("/login")}, modifier = Modifier.fillMaxWidth(0.7f), border = BorderStroke(2.dp,Color.Black),
                colors= ButtonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Unspecified, contentColor = Color.Unspecified, disabledContentColor = Color.Unspecified)) {
                Text("Already have account? login")
            }
        }
    }
}