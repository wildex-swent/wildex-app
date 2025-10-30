package com.android.wildex.ui.animal

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.theme.WildexTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalInformationScreen(
    animalId: Id,
    animalInformationScreenViewModel: AnimalInformationScreenViewModel = viewModel(),
    onGoBack: () -> Unit = {},
) {
  val uiState by animalInformationScreenViewModel.uiState.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(Unit) { animalInformationScreenViewModel.loadAnimalInformation(animalId) }

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      animalInformationScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold { paddingValues ->
    when {
      uiState.isError -> LoadingFail()
      uiState.isLoading -> LoadingScreen()
      else -> {
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          Box(Modifier.fillMaxWidth()) {

            //            Placeholder image to preview
            //            Image(
            //                painter = painterResource(id = R.drawable.golden_retriever),
            //                contentDescription = "Golden Retriever Placeholder",
            //                contentScale = ContentScale.Crop,
            //                modifier = Modifier.height(350.dp))

            AsyncImage(
                model = uiState.pictureURL,
                contentDescription = "Animal picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.height(350.dp))

            // Back button positioned on top-left of the image
            Box(modifier = Modifier.padding(16.dp).align(Alignment.TopStart)) {
              Row(
                  modifier =
                      Modifier.clickable(onClick = onGoBack)
                          .clip(RoundedCornerShape(20.dp))
                          .background(colorScheme.background)
                          .padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colorScheme.primary,
                    )

                    Text(
                        text = "Back",
                        color = colorScheme.primary,
                    )
                  }
            }

            // Bottom gradient to transition into name and description
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(72.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to colorScheme.background,
                            )))
          }

          // Animal name and description
          Column(
              modifier =
                  Modifier.fillMaxWidth()
                      .background(colorScheme.background)
                      .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                      .padding(16.dp)
                      .weight(1f)) {
                Text(
                    text = uiState.name.ifEmpty { "Animal Name" },
                    color = colorScheme.secondary,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(bottom = 3.dp),
                )

                Text(
                    text = uiState.species.ifEmpty { "Animal Species" },
                    color = colorScheme.primary,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 10.dp))

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = uiState.description.ifEmpty { "Animal Description" },
                    color = colorScheme.onBackground,
                    fontSize = 16.sp,
                )
              }
        }
      }
    }
  }
}

@Preview
@Composable
fun AnimalInformationScreenPreview() {
  WildexTheme {
    Surface(modifier = Modifier.fillMaxSize()) { AnimalInformationScreen("fakeAnimalId") }
  }
}
