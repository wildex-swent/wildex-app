package com.android.wildex.ui.example

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.wildex.model.animaldetector.AnimalDetectRepository
import com.android.wildex.model.animaldetector.AnimalDetectResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AnimalDetectionTestScreen(repository: AnimalDetectRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope({ Dispatchers.IO })

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var detectionResult by remember { mutableStateOf<AnimalDetectResponse?>(null) }
    var description by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Launcher to select image from gallery
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri?
            ->
            selectedImageUri = uri
            uri?.let {
                val stream = context.contentResolver.openInputStream(it)
                bitmap = BitmapFactory.decodeStream(stream)
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = { launcher.launch("image/*") }) { Text("Select Image") }

        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Selected image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentScale = ContentScale.Fit,
            )
        }

        Button(
            onClick = {
                selectedImageUri?.let { uri ->
                    scope.launch {
                        try {
                            // Update loading state on main thread
                            isLoading = true

                            // Run network calls on IO dispatcher
                            val result =
                                with(Dispatchers.IO) { repository.detectAnimal(context, uri) }

                            detectionResult = result

                            /*result?.animalType?.let { type ->
                              description = with(Dispatchers.IO) { repository.getAnimalDescription(type) }
                            }*/
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            enabled = selectedImageUri != null && !isLoading,
        ) {
            Text(if (isLoading) "Detecting..." else "Detect Animal")
        }

        detectionResult?.let { result ->
            Text(
                text =
                    "Animal: ${result.animalType}\nConfidence: ${"%.2f".format(result.confidence * 100)}%",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Button(
            onClick = {
                selectedImageUri?.let { uri ->
                    scope.launch {
                        try {
                            // Update loading state on main thread
                            isLoading = true

                            detectionResult?.animalType?.let { type ->
                                description =
                                    with(Dispatchers.IO) { repository.getAnimalDescription(type) }
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            enabled = selectedImageUri != null && !isLoading && detectionResult != null,
        ) {
            Text(if (isLoading) "Getting description..." else "Get description")
        }

        description?.let {
            Text(
                text = "Description: $it",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
