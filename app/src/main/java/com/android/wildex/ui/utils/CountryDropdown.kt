package com.android.wildex.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.android.wildex.R
import com.android.wildex.ui.utils.search.SearchEngine
import java.util.Locale

object CountryDropdownTestTags {
  const val COUNTRY_DROPDOWN = "country_dropdown"
  const val COUNTRY_ELEMENT = "country_element_"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryDropdown(
    modifier: Modifier = Modifier,
    selectedCountry: String,
    onCountrySelected: (String) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  val userLocale = LocalContext.current.resources.configuration.locales[0]
  val searchEngine: SearchEngine = remember { SearchEngine() }
  val allCountries =
      remember(userLocale) {
        Locale.getISOCountries()
            .map { code ->
              val locale = Locale("", code)
              val name = locale.getDisplayCountry(userLocale)
              val flag = code.toFlagEmoji()
              Pair(flag, name)
            }
            .sortedBy { it.second }
      }

  val filteredCountries =
      remember(searchQuery, allCountries) {
        if (searchQuery.isBlank()) {
          allCountries
        } else {
          searchEngine.search(searchQuery, allCountries.map { it.second.trim() }, 50).map {
              scoredMatch ->
            allCountries.first { it.second.trim() == scoredMatch.string }
          }
        }
      }

  ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { expanded = !expanded },
      modifier = modifier.testTag(CountryDropdownTestTags.COUNTRY_DROPDOWN),
  ) {
    OutlinedTextField(
        value = if (expanded) searchQuery else selectedCountry,
        onValueChange = {
          searchQuery = it
          if (!expanded) expanded = true
        },
        label = { Text("Country") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        modifier =
            Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth(),
        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
    )

    ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = {
          expanded = false
          searchQuery = ""
        },
        modifier = Modifier.background(colorScheme.surface),
    ) {
      if (filteredCountries.isEmpty()) {
        DropdownMenuItem(text = { Text(stringResource(R.string.no_countries_found), style = typography.bodyMedium) }, onClick = {}, enabled = false)
      } else {
        filteredCountries.forEach { countryPair ->
          val flag = countryPair.first
          val countryName = countryPair.second
          DropdownMenuItem(
              modifier = Modifier.testTag(CountryDropdownTestTags.COUNTRY_ELEMENT + countryName),
              text = { Text("$flag $countryName", style = typography.bodyMedium) },
              onClick = {
                onCountrySelected(countryName)
                expanded = false
                searchQuery = ""
              },
          )
        }
      }
    }
  }
}

fun String.toFlagEmoji(): String {
  val first = Character.codePointAt(this, 0) - 0x41 + 0x1F1E6
  val second = Character.codePointAt(this, 1) - 0x41 + 0x1F1E6
  return String(Character.toChars(first)) + String(Character.toChars(second))
}
