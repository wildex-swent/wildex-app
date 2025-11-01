package com.android.wildex.ui.camera

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.wildex.model.animaldetector.AnimalDetectResponse

object PostCreationScreenTestTags {
  const val POST_CREATION_SCREEN_HEADER_IMAGE = "post_creation_screen_header_image"
  const val POST_CREATION_SCREEN_FAMILY_BADGE = "post_creation_screen_family_badge"
  const val POST_CREATION_SCREEN_ANIMAL_NAME = "post_creation_screen_animal_name"
  const val POST_CREATION_SCREEN_SPECIES_NAME = "post_creation_screen_species_name"
  const val POST_CREATION_SCREEN_CONFIDENCE = "post_creation_screen_confidence"
  const val POST_CREATION_SCREEN_DESCRIPTION_FIELD = "post_creation_screen_description_field"
  const val POST_CREATION_SCREEN_LOCATION_TOGGLE = "post_creation_screen_location_toggle"
  const val POST_CREATION_SCREEN_CANCEL_BUTTON = "post_creation_screen_cancel_button"
  const val POST_CREATION_SCREEN_CONFIRM_BUTTON = "post_creation_screen_confirm_button"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCreationScreen(
    description: String,
    onDescriptionChange: (String) -> Unit,
    useLocation: Boolean,
    onLocationToggle: (Boolean) -> Unit,
    photoUri: Uri,
    detectionResponse: AnimalDetectResponse,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val maxCharacters = 500
  val animalName = detectionResponse.animalType
  val speciesName = detectionResponse.taxonomy.species
  val family = detectionResponse.taxonomy.family
  val confidence = detectionResponse.confidence

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .background(colorScheme.surface)
              .verticalScroll(rememberScrollState())
  ) {
    // Header with image
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
      // Replace with actual image - using placeholder
      AsyncImage(
          model = photoUri,
          contentDescription = "Post image",
          contentScale = ContentScale.Crop,
          modifier =
              Modifier.fillMaxSize()
                  .testTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_HEADER_IMAGE),
      )

      Box(
          modifier =
              Modifier.fillMaxWidth()
                  .align(Alignment.BottomCenter)
                  .fillMaxHeight(0.3f)
                  .background(
                      Brush.verticalGradient(
                          0f to Color.Transparent,
                          1f to colorScheme.surface,
                      )
                  )
      )

      // Header content
      Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        // Family badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colorScheme.surface.copy(alpha = 0.25f),
            modifier =
                Modifier.align(Alignment.TopStart)
                    .testTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_FAMILY_BADGE),
        ) {
          Text(
              text = family,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
          )
        }

        // Species name and confidence
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
          Column {
            Text(
                text = animalName,
                color = colorScheme.primary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier.testTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_ANIMAL_NAME),
            )
            Text(
                text = speciesName,
                color = colorScheme.primary.copy(alpha = 0.7f),
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic,
                modifier =
                    Modifier.testTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_SPECIES_NAME),
            )
          }

          Column(
              horizontalAlignment = Alignment.End,
              modifier =
                  Modifier.testTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_CONFIDENCE),
          ) {
            Text(
                text = "Confidence",
                color = colorScheme.secondary.copy(alpha = 0.9f),
                fontSize = 12.sp,
            )
            Text(
                text = "${confidence * 100} %",
                color = colorScheme.secondary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    // Form content
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Text(
          text = "Add a description",
          fontSize = 20.sp,
          fontWeight = FontWeight.SemiBold,
          color = colorScheme.onSurface,
      )

      // Description field
      OutlinedTextField(
          value = description,
          onValueChange = { if (it.length <= maxCharacters) onDescriptionChange(it) },
          modifier =
              Modifier.fillMaxWidth()
                  .height(180.dp)
                  .testTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_DESCRIPTION_FIELD),
          placeholder = {
            Text(
                text =
                    "Share what you observed... behavior, habitat, weather conditions, or any interesting details!",
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 15.sp,
            )
          },
          shape = RoundedCornerShape(12.dp),
          supportingText = {
            Text(
                text = "${description.length}/$maxCharacters characters",
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
          },
      )

      Row(
          modifier =
              Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { onLocationToggle(!useLocation) }
                  .testTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_LOCATION_TOGGLE),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              tint = colorScheme.primary,
          )
          Column {
            Text(
                text = "Share Location",
                style = typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Help others discover nearby animals",
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
          }
        }
        Checkbox(checked = useLocation, onCheckedChange = onLocationToggle)
      }

      // Action buttons
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
            onClick = onCancel,
            modifier =
                Modifier.weight(1f)
                    .height(56.dp)
                    .testTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_CANCEL_BUTTON),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.primary),
            border = ButtonDefaults.outlinedButtonBorder().copy(width = 1.dp),
        ) {
          Icon(
              imageVector = Icons.Default.Close,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "Cancel & Retry", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }

        Button(
            onClick = onConfirm,
            modifier =
                Modifier.weight(1f)
                    .height(56.dp)
                    .testTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_CONFIRM_BUTTON),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
        ) {
          Text(text = "✓ Confirm & Post", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
      }

      // Footer text
      Text(
          text = "Your sighting will be shared with the wildlife community",
          fontSize = 13.sp,
          color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.align(Alignment.CenterHorizontally),
      )
    }
  }
}
