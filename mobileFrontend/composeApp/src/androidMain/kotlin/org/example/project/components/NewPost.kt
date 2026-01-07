package org.example.project.components

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.BufferedSink
import org.example.project.BuildConfig
import org.example.project.datamodels.ServerResponse
import kotlin.collections.listOf
import okio.IOException
import okio.source
import kotlin.time.Clock

class CreateNewPost(private var apiClient: OkHttpClient) {
    suspend fun sendData(multipartRequest: MultipartBody, authToken: String): Result<ServerResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${BuildConfig.BACKEND_URL.trim()}/api/newpost").addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json").post(multipartRequest).build()
            apiClient.newCall(request = request).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext Result.success(ServerResponse(status=200, responseBody = response.body.string()))
                }
                return@withContext Result.failure(
                    IOException("HTTP ${response.code}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class NewPostModel(private val apiClient: CreateNewPost): ViewModel() {
    private val responseref = MutableStateFlow<ServerResponse?>(null)
    val response: StateFlow<ServerResponse?> = responseref

    fun fetch(context: Context, postTitle: String, postDescription: String, imageList: List<Uri>, bortleScale: Float, moonPhase: String, authToken: String) {
        val currentTime: String = Clock.System.now().toString()
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("post_title",postTitle)
            .addFormDataPart("post_description", postDescription).addFormDataPart("post_date", currentTime)
            .addFormDataPart("bortle_scale", bortleScale.toInt().toString()).addFormDataPart("moon_phase", moonPhase)
        imageList.forEachIndexed { index, imageUri ->
            val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
            val requestBody = object : RequestBody() {
                override fun contentType() = mimeType.toMediaType()
                override fun writeTo(sink: BufferedSink) {
                    context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                        sink.writeAll(inputStream.source())
                    }
                }
            }
            builder.addFormDataPart("image_data[]", "postimage_$index.jpg", requestBody)
        }
        viewModelScope.launch {
            val result = apiClient.sendData(builder.build(), authToken = authToken)
            result.fold(
                onSuccess = { response ->
                    responseref.value = response
                },
                onFailure = { error ->
                    Log.d("Error: ${error.message}", "DEBUG")
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPost(context: Context, navController: NavController) {
    val httpClient = remember { OkHttpClient() }
    val newPostClient = remember { CreateNewPost(httpClient) }
    val newPostMaker = remember { NewPostModel(newPostClient) }
    val serverResponse by newPostMaker.response.collectAsState()
    var postTitle by remember { mutableStateOf("") }
    var postDescription by remember { mutableStateOf("") }
    var imageList by remember { mutableStateOf<List<Uri>>(listOf()) }
    var bortleScale by remember {mutableFloatStateOf(8f)}
    val moonPhases = listOf("New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous", "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent")
    var moonPhaseChoice by remember { mutableStateOf("New Moon") }
    var askForExitPage by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            imageList = imageList + listOf(uri)
        }
    }
    //var isUserExiting by remember { mutableStateOf(false) }
    val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    val prefs = EncryptedSharedPreferences.create(
        context,"auth_prefs",masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    LaunchedEffect(serverResponse) {
        serverResponse?.let { res ->
            navController.navigate("/userprofile")
        }
    }
    BackHandler(enabled=true) {
        askForExitPage = true
    }
    Scaffold(topBar = {
        TopAppBar(title = { Row(Modifier.fillMaxWidth().height(60.dp)) {
            Row(Modifier.fillMaxWidth().fillMaxHeight(0.9f), horizontalArrangement = Arrangement.Center){
                IconButton(onClick = {
                    if (!askForExitPage) {
                        askForExitPage = true
                    }
                }, Modifier.height(100.dp).width(50.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack, tint = Color(25, 25, 26),
                        contentDescription = null, modifier = Modifier.height(90.dp).width(90.dp)
                    )
                }
                Spacer(modifier = Modifier.fillMaxWidth(0.64f))
                Button(onClick={
                    val authToken = prefs.getString("auth_token", null)
                    if (authToken != null) {
                        newPostMaker.fetch(context, postTitle, postDescription, imageList, bortleScale, moonPhaseChoice, authToken)
                    }}, enabled = (postTitle.trim() != " " && postDescription.trim() != " " && imageList.isNotEmpty())) {
                    Text("upload")
                }
            }
        }}, colors = topAppBarColors(containerColor = Color.Transparent))
    }, bottomBar = {
        BottomAppBar(contentColor = Color(194, 166, 237, 255), modifier = Modifier.height(220.dp)) {
            Row(Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.Center) {
                val imageScroller = rememberScrollState()
                IconButton(onClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }, modifier = Modifier.fillMaxHeight(0.9f).width(150.dp).padding(4.dp).border(2.dp, Color(102,161,255))) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        tint = Color(102, 161, 255),
                        contentDescription = null, modifier = Modifier.size(40.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(190.dp).padding(2.dp)
                        .horizontalScroll(imageScroller)
                ) {
                    imageList.forEach { imageUri ->
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(imageUri).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.clip(RectangleShape).fillMaxHeight(0.9f).fillMaxHeight(0.88f).border(2.dp, Color.Black)
                        )
                        Spacer(Modifier.padding(5.dp))
                    }
                }
            }
        }
    }) { innerPadding ->
        val fieldScrollbar = rememberScrollState()
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Column(
                modifier = Modifier.padding(innerPadding).fillMaxWidth().fillMaxHeight()
                    .verticalScroll(fieldScrollbar),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = postTitle, onValueChange = { postTitle = it },
                    modifier = Modifier.fillMaxWidth(0.95f).height(100.dp),
                    placeholder = { Text("Enter title...") }, label = { Text("title") }
                )
                Spacer(modifier = Modifier.fillMaxWidth(0.95f).height(20.dp))
                OutlinedTextField(
                    value = postDescription, onValueChange = { postDescription = it },
                    modifier = Modifier.fillMaxWidth(0.95f).height(350.dp),
                    placeholder = { Text("write something..") }, label = { Text("description") }
                )
                if (askForExitPage) {
                    Dialog({}) {
                        Card(
                            modifier = Modifier.fillMaxWidth(0.9f).height(170.dp).padding(10.dp),
                            shape = RoundedCornerShape(30.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight().fillMaxWidth(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Confirm to exit, changes won't be saved!", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    TextButton(onClick = { askForExitPage = false }) {
                                        Text("Cancel")
                                    }
                                    TextButton(onClick = { navController.navigate("/userprofile") }) {
                                        Text("Discard")
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.fillMaxWidth(0.95f).height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = bortleScale,
                        valueRange = 1f..9f,
                        onValueChange = { bortleScale = it },
                        steps = 7,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )
                    Spacer(modifier = Modifier.width(40.dp))
                    Text(
                        when (bortleScale) {
                            in 1f..2f -> "Excellent"
                            in 3f..4f -> "Good"
                            in 5f..6f -> "Moderate"
                            in 7f..8f -> "Poor"
                            9f -> "Severe"
                            else -> { "INVALID" }
                        }, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.fillMaxWidth(0.95f).height(20.dp))
                Select("Moon phase",value=moonPhaseChoice,moonPhases, onSelect = { option -> moonPhaseChoice = option },modifier = Modifier.fillMaxWidth(0.9f))
            }
        }
    }
}
