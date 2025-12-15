package com.android.wildex.ui.authentication

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.ui.utils.CountryDropdown

object OptionalInfoScreenTestTags {
  const val OPTIONAL_INFO_SCREEN = "optional_info_screen"
  const val PROFILE_PICTURE = "profile_picture"
  const val BIO_FIELD = "bio_field"
  const val BACK_BUTTON = "back_button"
  const val NEXT_BUTTON = "next_button"
}

@Composable
fun OptionalInfoScreen(
    data: OnBoardingData,
    updateData: (OnBoardingData) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean,
) {
  val pickImageLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { updateData(data.copy(profilePicture = it)) }
      }

  Column(
      modifier =
          Modifier.fillMaxSize()
              .padding(24.dp)
              .testTag(OptionalInfoScreenTestTags.OPTIONAL_INFO_SCREEN),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Box(
        modifier =
            Modifier.align(Alignment.CenterHorizontally)
                .padding(bottom = 70.dp)
                .testTag(OptionalInfoScreenTestTags.PROFILE_PICTURE)) {
          AsyncImage(
              model = data.profilePicture,
              contentDescription = "Profile picture",
              modifier =
                  Modifier.width(96.dp)
                      .aspectRatio(1f)
                      .clip(CircleShape)
                      .border(1.dp, colorScheme.outline, CircleShape)
                      .clickable(onClick = { pickImageLauncher.launch("image/*") }),
              contentScale = ContentScale.Crop,
          )
          Icon(
              imageVector = Icons.Filled.Create,
              contentDescription = "Change profile picture",
              tint = colorScheme.onPrimary,
              modifier =
                  Modifier.align(Alignment.TopEnd)
                      .size(20.dp)
                      .clip(CircleShape)
                      .background(colorScheme.secondary)
                      .padding(4.dp),
          )
        }

    CountryDropdown(
        selectedCountry = data.country,
        onCountrySelected = { updateData(data.copy(country = it)) },
        modifier = Modifier.fillMaxWidth(),
        fieldShape = RoundedCornerShape(16.dp),
    )

    Spacer(modifier = Modifier.height(16.dp))

    val maxCharacters = 300
    OutlinedTextField(
        value = data.bio,
        onValueChange = { if (it.length <= maxCharacters) updateData(data.copy(bio = it)) },
        modifier = Modifier.fillMaxWidth().testTag(OptionalInfoScreenTestTags.BIO_FIELD),
        placeholder = {
          Text(
              text = stringResource(R.string.bio_placeholder),
              color = colorScheme.onSurface.copy(alpha = 0.5f),
              style = typography.bodyLarge,
          )
        },
        shape = RoundedCornerShape(16.dp),
        supportingText = {
          Text(
              text = stringResource(R.string.bio_support, data.bio.length, maxCharacters),
              style = typography.bodySmall,
              color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
          )
        },
        minLines = 3,
    )

    Spacer(modifier = Modifier.height(32.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      OutlinedButton(
          enabled = !isLoading,
          onClick = onBack,
          modifier =
              Modifier.weight(1f)
                  .padding(end = 8.dp)
                  .testTag(OptionalInfoScreenTestTags.BACK_BUTTON),
      ) {
        Text(stringResource(R.string.back))
      }

      Button(
          enabled = !isLoading,
          onClick = onNext,
          modifier =
              Modifier.weight(1f)
                  .padding(start = 8.dp)
                  .testTag(OptionalInfoScreenTestTags.NEXT_BUTTON),
      ) {
        Text(stringResource(R.string.next))
      }
    }
  }
}
