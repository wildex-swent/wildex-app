package com.android.wildex.ui.utils.location

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LocationSelector(
    modifier: Modifier = Modifier,
    locationName: String?,
    onClick: () -> Unit,
) {
  Box(
      modifier =
          modifier
              .fillMaxWidth(0.9f)
              .clickable { onClick() }
              .border(
                  width = 1.dp,
                  color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                  shape = RoundedCornerShape(6.dp),
              )
              .padding(vertical = 14.dp, horizontal = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = locationName ?: "Select a location",
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (locationName == null) colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    else colorScheme.onSurface,
            )
          }
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = null,
              tint = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
              modifier = Modifier.size(20.dp),
          )
        }
      }
}
