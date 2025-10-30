package com.android.wildex.ui.camera

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.wildex.model.animaldetector.BoundingBox

@Composable
fun PostCreationScreen(
    description: String,
    onDescriptionChange: (String) -> Unit,
    useLocation: Boolean,
    onLocationToggle: (Boolean) -> Unit,
    photoUri: Uri,
    boundingBox: BoundingBox,
    animalName: String,
    speciesName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
  var isExpanded by remember { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top section - Image with bounding box overlay
      Box(
          modifier = Modifier.fillMaxWidth().weight(1f),
          contentAlignment = Alignment.Center,
      ) {
        // Photo
        AsyncImage(
            model = photoUri,
            contentDescription = "Detected animal",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )

        // Bounding box overlay with detection info card
        BoundingBoxOverlay(
            boundingBox = boundingBox,
            animalName = animalName,
            speciesName = speciesName,
        )
      }

      // Bottom section - Expandable input area
      ExpandableInputSection(
          description = description,
          onDescriptionChange = { onDescriptionChange(it) },
          useLocation = useLocation,
          onLocationToggle = { onLocationToggle(it) },
          isExpanded = isExpanded,
          onExpandToggle = { isExpanded = !isExpanded },
          onConfirm = onConfirm,
          onCancel = onCancel,
      )
    }
  }
}

@Composable
private fun BoxScope.BoundingBoxOverlay(
    boundingBox: BoundingBox,
    animalName: String,
    speciesName: String,
) {
  val colorScheme = colorScheme
  Canvas(modifier = Modifier.fillMaxSize()) {
    val strokeWidth = 4.dp.toPx()

    // Convert normalized coordinates to screen pixels
    val left = boundingBox.x * size.width
    val top = boundingBox.y * size.height
    val width = boundingBox.width * size.width
    val height = boundingBox.height * size.height

    // Draw bounding box with rounded corners

    drawRoundRect(
        color = colorScheme.primary,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(12.dp.toPx()),
        style = Stroke(width = strokeWidth),
    )

    // Corner accents (top-left and bottom-right)
    val cornerLength = 30.dp.toPx()

    // Top-left corner
    drawLine(
        color = colorScheme.primary,
        start = Offset(left, top),
        end = Offset(left + cornerLength, top),
        strokeWidth = strokeWidth * 2,
    )
    drawLine(
        color = colorScheme.primary,
        start = Offset(left, top),
        end = Offset(left, top + cornerLength),
        strokeWidth = strokeWidth * 2,
    )

    // Bottom-right corner
    drawLine(
        color = colorScheme.primary,
        start = Offset(left + width, top + height),
        end = Offset(left + width - cornerLength, top + height),
        strokeWidth = strokeWidth * 2,
    )
    drawLine(
        color = colorScheme.primary,
        start = Offset(left + width, top + height),
        end = Offset(left + width, top + height - cornerLength),
        strokeWidth = strokeWidth * 2,
    )
  }

  // Info card above bounding box
  DetectionInfoCard(
      animalName = animalName,
      speciesName = speciesName,
      modifier =
          Modifier.align(Alignment.TopStart)
              .offset(
                  x = (boundingBox.x * LocalConfiguration.current.screenWidthDp).dp,
                  y =
                      (boundingBox.y * LocalConfiguration.current.screenHeightDp)
                          .dp
                          .coerceAtLeast(16.dp),
              ),
  )
}

@Composable
private fun DetectionInfoCard(
    animalName: String,
    speciesName: String,
    modifier: Modifier = Modifier,
) {
  Card(
      modifier = modifier.padding(8.dp),
      elevation = CardDefaults.cardElevation(8.dp),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
            imageVector = Icons.Default.Pets,
            contentDescription = null,
            tint = colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = animalName,
            style = typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onPrimaryContainer,
        )
      }
      Text(
          text = speciesName,
          style = typography.bodySmall,
          color = colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
          fontStyle = FontStyle.Italic,
      )
    }
  }
}

@Composable
private fun ExpandableInputSection(
    description: String,
    onDescriptionChange: (String) -> Unit,
    useLocation: Boolean,
    onLocationToggle: (Boolean) -> Unit,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
  Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
      elevation = CardDefaults.cardElevation(16.dp),
  ) {
    Column {
      // Drag handle and expand button
      IconButton(
          onClick = onExpandToggle,
          modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
      ) {
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = colorScheme.onSurfaceVariant,
        )
      }

      AnimatedVisibility(visible = isExpanded) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // Description field
          OutlinedTextField(
              value = description,
              onValueChange = onDescriptionChange,
              modifier = Modifier.fillMaxWidth(),
              label = { Text("Add a description (optional)") },
              placeholder = { Text("Tell us more about this sighting...") },
              minLines = 3,
              maxLines = 5,
              shape = RoundedCornerShape(12.dp),
          )

          // Location checkbox
          Row(
              modifier =
                  Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .clickable { onLocationToggle(!useLocation) }
                      .padding(12.dp),
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
        }
      }

      // Action buttons (always visible)
      Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Cancel button
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
        ) {
          Text("Cancel")
        }

        // Confirm button
        Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
        ) {
          Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Post")
        }
      }
    }
  }
}
