package com.android.wildex.ui.report

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.report.Report
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Data class representing the UI state of the Submit Report Screen.
 *
 * @property imageUri The URI of the image to be submitted with the report.
 * @property description The description text of the report.
 * @property location The location associated with the report.
 * @property isSubmitting Flag indicating whether the report is currently being submitted.
 * @property errorMsg An optional error message if submission fails.
 */
data class SubmitReportUiState(
    val imageUri: Uri? = null,
    val description: String = "",
    val location: Location? = null,
    val hasPickedLocation: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMsg: String? = null,
)

/**
 * ViewModel for managing the state and logic of the Submit Report Screen.
 *
 * @property reportRepository The repository for handling report data operations.
 * @property storageRepository The repository for handling storage operations.
 * @property currentUserId The unique identifier of the current user.
 */
class SubmitReportScreenViewModel(
    private val reportRepository: ReportRepository = RepositoryProvider.reportRepository,
    private val storageRepository: StorageRepository = RepositoryProvider.storageRepository,
    private val currentUserId: Id = Firebase.auth.uid ?: "",
) : ViewModel() {
  private val _uiState = MutableStateFlow(SubmitReportUiState())
  val uiState: StateFlow<SubmitReportUiState> = _uiState.asStateFlow()

  /**
   * Called when the user updates the description text.
   *
   * @param description The new description text.
   */
  fun updateDescription(description: String) {
    _uiState.value = _uiState.value.copy(description = description)
  }

  /**
   * Called when the user selects or changes the image for the report.
   *
   * @param imageUri The URI of the selected image.
   */
  fun updateImage(imageUri: Uri?) {
    _uiState.value = _uiState.value.copy(imageUri = imageUri)
  }

  /**
   * Called when the user picks a location from the LocationPickerScreen.
   *
   * @param picked The location selected by the user.
   */
  fun updateLocation(picked: Location) {
    _uiState.value = _uiState.value.copy(location = picked, hasPickedLocation = true)
  }

  /** Called when the user picks a location from the LocationPickerScreen. */
  fun clearLocation() {
    _uiState.value = _uiState.value.copy(location = null, hasPickedLocation = false)
  }

  /** Clears any existing error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Submits the report with the current UI state data.
   *
   * @param onSuccess A callback function to be invoked upon successful submission.
   */
  fun submitReport(onSuccess: () -> Unit) {
    val currentState = _uiState.value

    when {
      currentState.imageUri == null -> {
        setError("Please provide an image for the report.")
        return
      }
      currentState.description.isBlank() -> {
        setError("Please provide a description for the report.")
        return
      }
      currentState.location == null -> {
        setError("Location is required to submit a report.")
        return
      }
    }

    viewModelScope.launch {
      try {
        _uiState.value = currentState.copy(isSubmitting = true)
        val authorId = currentUserId
        val reportId = reportRepository.getNewReportId()

        val imageUri = currentState.imageUri ?: throw IllegalStateException("No image provided")
        val imageUrl =
            storageRepository.uploadReportImage(reportId, imageUri)
                ?: throw IllegalStateException("Image upload failed: URL is null")

        val location = currentState.location ?: throw IllegalStateException("No location provided")

        val report =
            Report(
                reportId = reportId,
                imageURL = imageUrl,
                location = location,
                date = Timestamp(Date()),
                description = currentState.description,
                authorId = authorId,
                assigneeId = null,
            )

        reportRepository.addReport(report)

        resetUiStateOnSuccess()
        onSuccess()
      } catch (e: Exception) {
        setError(e.message ?: "Something went wrong while submitting the report.")
        _uiState.value = _uiState.value.copy(isSubmitting = false)
      }
    }
  }

  /**
   * Sets an error message in the UI state.
   *
   * @param message The error message to be set.
   */
  private fun setError(message: String) {
    _uiState.value = _uiState.value.copy(errorMsg = message)
  }

  /** Resets the UI state to its initial values upon successful report submission. */
  private fun resetUiStateOnSuccess() {
    _uiState.value = SubmitReportUiState()
  }
}
