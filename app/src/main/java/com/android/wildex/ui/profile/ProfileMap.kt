package com.android.wildex.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.wildex.model.utils.Id

@Composable
fun ProfileMap(id: Id = "", onMap: (Id) -> Unit = {}) {
  val cs = colorScheme
  ElevatedCard(
      modifier =
          Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag(ProfileScreenTestTags.MAP),
      shape = RoundedCornerShape(14.dp),
  ) {
    Column(
        modifier =
            Modifier.border(
                    1.dp,
                    cs.onBackground.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(12.dp)) {
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .height(160.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(cs.background)) {
                Text(
                    text = "Map Placeholder",
                    modifier = Modifier.align(Alignment.Center),
                    color = cs.onBackground,
                )
              }

          Button(
              onClick = { onMap(id) },
              modifier = Modifier.padding(top = 10.dp),
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = cs.primary,
                      contentColor = cs.onPrimary,
                  ),
          ) {
            Text(text = "View in full screen →")
          }
        }
  }
}
