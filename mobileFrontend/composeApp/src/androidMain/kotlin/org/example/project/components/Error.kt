package org.example.project.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun ErrorPage(navigator: NavController)  {
    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(imageVector= Icons.Default.Error, contentDescription = null, modifier = Modifier.height(140.dp).width(140.dp), tint = Color.Red)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Oops! Some Error occurred!", fontSize = 30.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick={navigator.popBackStack()},
                modifier = Modifier.fillMaxWidth(0.7f),
                colors= ButtonColors(containerColor = Color(98,174,245), disabledContainerColor = Color.Unspecified, contentColor = Color.White,disabledContentColor = Color.Unspecified)
            ) {
                Text("Retry")
            }
        }
    }
}