package com.android.wildex.ui.animal

import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.model.utils.Id


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalInformationScreen(
    animalId: Id,
    animalInformationScreenViewModel: AnimalInformationScreenViewModel = viewModel(),
    onGoBack: () -> Unit = {},
) {
    val uiState by animalInformationScreenViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { animalInformationScreenViewModel.loadAnimalInformation(animalId)}

    LaunchedEffect(uiState.errorMsg) {
        uiState.errorMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            animalInformationScreenViewModel.clearErrorMsg()
        }
    }

    // TODO: Implement the UI
    Text("Animal Information Screen")

}