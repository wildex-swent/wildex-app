package com.android.wildex.ui.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.wildex.R

@Composable
fun NamingScreen(
    data: OnBoardingData,
    updateData: (OnBoardingData) -> Unit,
    onNext: () -> Unit,
    canProceed: Boolean,
    isLoading: Boolean,
) {

  Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(R.string.welcome),
            style = typography.displayMedium,
            color = colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 70.dp))

        OutlinedTextField(
            value = data.name,
            onValueChange = { updateData(data.copy(name = it)) },
            label = {
              Text(
                  stringResource(R.string.first_name),
                  color = colorScheme.onBackground.copy(.6f),
                  style = typography.bodyMedium,
              )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = data.surname,
            onValueChange = { updateData(data.copy(surname = it)) },
            label = {
              Text(
                  stringResource(R.string.last_name),
                  color = colorScheme.onBackground.copy(.6f),
                  style = typography.bodyMedium,
              )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = data.username,
            onValueChange = { updateData(data.copy(username = it)) },
            label = {
              Text(
                  stringResource(R.string.username),
                  color = colorScheme.onBackground.copy(.6f),
                  style = typography.bodyMedium,
              )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(enabled = canProceed && !isLoading, onClick = onNext) {
          Text(stringResource(R.string.next))
        }
      }
}
