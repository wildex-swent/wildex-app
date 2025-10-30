package com.android.wildex.ui.camera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CameraPermissionScreen(
    onRequestPermission: () -> Unit,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Box(
      modifier = modifier.fillMaxSize().background(colorScheme.background),
      contentAlignment = Alignment.Center,
  ) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp),
    ) {
      // Sad camera icon
      Icon(
          imageVector = Icons.Default.CameraAlt,
          contentDescription = null,
          tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.size(120.dp),
      )

      // Funny message
      Text(
          text = "We promise not to take selfies. Just animals.",
          style = typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = colorScheme.onBackground,
          textAlign = TextAlign.Center,
      )

      Text(
          text = "Grant camera access — or we’ll just imagine the animal for you.",
          style = typography.bodyLarge,
          color = colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          lineHeight = 24.sp,
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Request permission button - primary action
      Button(
          onClick = onRequestPermission,
          modifier = Modifier.fillMaxWidth().height(56.dp),
          colors =
              ButtonDefaults.buttonColors(
                  containerColor = colorScheme.primary,
                  contentColor = colorScheme.onPrimary,
              ),
          shape = RoundedCornerShape(16.dp),
      ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Grant Camera Access",
            style = typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
      }

      // Upload button - secondary action
      OutlinedButton(
          onClick = onUploadClick,
          modifier = Modifier.fillMaxWidth().height(56.dp),
          border = BorderStroke(2.dp, colorScheme.outline),
          shape = RoundedCornerShape(16.dp),
      ) {
        Icon(
            imageVector = Icons.Default.Upload,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Upload from Gallery",
            style = typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}
