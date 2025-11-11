package com.android.wildex.ui.report

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

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
    val isSubmitting: Boolean = false,
    val errorMsg: String? = null
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
    private val currentUserId: Id = Firebase.auth.uid ?: defaultUser.userId
) : ViewModel() {}
