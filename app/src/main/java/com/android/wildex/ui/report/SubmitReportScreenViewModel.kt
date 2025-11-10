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

data class SubmitReportUiState(
    val imageUri: Uri? = null,
    val description: String = "",
    val location: Location? = null,
    val isSubmitting: Boolean = false,
    val errorMsg: String? = null,
    val success: Boolean = false
)

class SubmitReportScreenViewModel(
    private val reportRepository: ReportRepository = RepositoryProvider.reportRepository,
    private val storageRepository: StorageRepository = RepositoryProvider.storageRepository,
    private val currentId: Id? = Firebase.auth.uid
) : ViewModel() {}
