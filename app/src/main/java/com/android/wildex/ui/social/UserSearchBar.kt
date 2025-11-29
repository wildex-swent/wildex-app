package com.android.wildex.ui.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarColors
import com.android.wildex.model.user.User
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.utils.ClickableProfilePicture

/**
 * Test tags to assign to the different components of the search bar for testing purposes
 */
object SearchBarTestTags{
  const val TRAILING_ICON = "trailing_icon"
  const val SEARCH_BAR = "search_bar"
  const val INPUT_FIELD = "input_field"
  const val LEADING_ICON = "leading_icon"
  const val RESULT_LIST = "search_results"
  fun testTagForResult(userId: Id) = "result_$userId"
  fun testTagForResultUsername(userId: Id) = "result_username_$userId"
}

/**
 * Search bar Composable that allows to search for users of the app. When focused, the search bar expands
 * into a search view that takes all available space within the parent and shows the result of the field's
 * input in a list.
 *
 * @param userIndex Algorithm which determines and ranks users matching a typed query in the field
 * @param onResultClick callback function that takes to the current user to a user's profile when they
 *  click on a search result
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchBar(
  userIndex: UserIndex,
  onResultClick: (Id) -> Unit
){
  var expanded by rememberSaveable { mutableStateOf(false) }

  val textFieldState = rememberTextFieldState()

  var searchResults by remember{ mutableStateOf(emptyList<User>()) }

  val screenHeight = LocalWindowInfo.current.containerSize.height.dp

  LaunchedEffect(textFieldState.text) {
    val query = textFieldState.text.toString()

    if (query.isBlank()) {
      searchResults = emptyList()
      return@LaunchedEffect
    }

    searchResults = userIndex.usersMatching(query, 10)
  }

  Box(
    modifier = Modifier.fillMaxWidth()
  ){
    SearchBar(
      colors = SearchBarColors(
        containerColor = colorScheme.background,
        dividerColor = colorScheme.onBackground,
      ),
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .testTag(SearchBarTestTags.SEARCH_BAR)
        .align(Alignment.TopCenter)
        .fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 5.dp),
      inputField = {
        SearchBarDefaults.InputField(
          modifier = Modifier.height(screenHeight / 45)
            .testTag(SearchBarTestTags.INPUT_FIELD),
          colors = SearchBarDefaults.colors().inputFieldColors.copy(
            focusedTextColor = colorScheme.onBackground,
            unfocusedTextColor = colorScheme.onBackground,
            focusedContainerColor = colorScheme.surfaceVariant,
            unfocusedContainerColor = colorScheme.surfaceVariant,
            unfocusedLeadingIconColor = colorScheme.primary,
          ),
          query = textFieldState.text.toString(),
          onQueryChange = {
            textFieldState.edit { replace(0, length, it) }
          },
          onSearch = {
            expanded = false
          },
          expanded = expanded,
          onExpandedChange = { expanded = it },
          placeholder = { Text("Search users", style = typography.bodyMedium) },
          leadingIcon = {
            if (!expanded){
              Icon(Icons.Default.Search, contentDescription = "Search")
            } else {
              IconButton(
                modifier = Modifier.testTag(SearchBarTestTags.LEADING_ICON),
                onClick = { expanded = false }
              ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Go Back")
              }
            }
          },
          trailingIcon = {
            if (expanded){
              IconButton(
                modifier = Modifier.testTag(SearchBarTestTags.TRAILING_ICON),
                onClick = {textFieldState.edit { delete(0, textFieldState.text.length) }}
              ) {
                Icon(Icons.Default.Clear, contentDescription = "Clear text")
              }
            }
          }
        )
      },
      expanded = expanded,
      onExpandedChange = { expanded = it },
    ) {
      LazyColumn (modifier = Modifier.testTag(SearchBarTestTags.RESULT_LIST)){
        items(count = searchResults.size) { index ->
          val user = searchResults[index]
          ListItem(
            headlineContent = { Text(user.name + " " + user.surname, style = typography.bodyLarge) },
            supportingContent = { Text(
              text = user.username,
              modifier = Modifier.testTag(SearchBarTestTags.testTagForResultUsername(user.userId)),
              style = typography.bodyMedium
            )},
            leadingContent = {
              ClickableProfilePicture(
                modifier = Modifier.size(45.dp),
                profilePictureURL = user.profilePictureURL,
                profileUserType = user.userType,
              )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier
              .testTag(SearchBarTestTags.testTagForResult(user.userId))
              .clickable {
                onResultClick(user.userId)
                expanded = false
              }
              .fillMaxWidth()
          )
        }
      }
    }
  }
}