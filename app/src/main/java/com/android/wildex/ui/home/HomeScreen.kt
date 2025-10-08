package com.android.wildex.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.wildex.ui.theme.WildexTheme

@Composable
fun HomeScreen(){
    Text(text = "HOME")
}
@Preview
@Composable
fun HomeScreenPreview() {
    WildexTheme { Surface(modifier = Modifier.fillMaxSize()) { HomeScreen() } }
}