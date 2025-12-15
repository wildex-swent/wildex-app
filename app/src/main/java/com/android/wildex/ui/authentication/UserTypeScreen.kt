package com.android.wildex.ui.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.wildex.R
import com.android.wildex.model.user.UserType

@Composable
fun UserTypeScreen(
    data: OnBoardingData,
    updateData: (OnBoardingData) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean,
) {

  Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(R.string.account_type),
            style = typography.displayMedium,
            color = colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 70.dp),
        )

        SingleChoiceSegmentedButtonRow {
          UserType.entries.forEachIndexed { index, option ->
            SegmentedButton(
                shape =
                    SegmentedButtonDefaults.itemShape(index = index, count = UserType.entries.size),
                onClick = { updateData(data.copy(userType = option)) },
                selected = data.userType.ordinal == index,
                colors =
                    SegmentedButtonColors(
                        activeContainerColor = colorScheme.primary,
                        activeContentColor = colorScheme.onPrimary,
                        activeBorderColor = colorScheme.primary,
                        inactiveContainerColor = colorScheme.background,
                        inactiveContentColor = colorScheme.onBackground,
                        inactiveBorderColor = colorScheme.primary,
                        disabledActiveContainerColor = Color(1),
                        disabledActiveContentColor = Color(1),
                        disabledActiveBorderColor = Color(1),
                        disabledInactiveContainerColor = Color(1),
                        disabledInactiveContentColor = Color(1),
                        disabledInactiveBorderColor = Color(1),
                    ),
            ) {
              Text(
                  text = option.name.replaceFirstChar { it.uppercaseChar() },
                  color =
                      if (index == data.userType.ordinal) colorScheme.onPrimary
                      else colorScheme.onBackground,
                  style = typography.bodyMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(
                  text = stringResource(R.string.user_type),
                  style = typography.bodyMedium,
                  color = colorScheme.onBackground,
                  textAlign = TextAlign.Center,
              )
              Text(
                  text = stringResource(R.string.user_type_setting),
                  style = typography.bodySmall,
                  color = colorScheme.onBackground.copy(.7f),
                  textAlign = TextAlign.Center,
              )
            }
        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          OutlinedButton(
              enabled = !isLoading,
              onClick = onBack,
              modifier = Modifier.weight(1f).padding(end = 8.dp),
          ) {
            Text(stringResource(R.string.back))
          }

          Button(
              enabled = !isLoading,
              onClick = onNext,
              modifier = Modifier.weight(1f).padding(start = 8.dp),
          ) {
            Text(stringResource(R.string.complete))
          }
        }
      }
}
