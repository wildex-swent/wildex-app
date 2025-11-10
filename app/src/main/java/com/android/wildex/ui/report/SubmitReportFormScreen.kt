package com.android.wildex.ui.report

import android.content.Context
import androidx.compose.runtime.Composable

@Composable
fun SubmitReportFormScreen(
    uiState: SubmitReportUiState,
    onCameraClick: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
    context: Context,
    onGoBack: () -> Unit,
) {}
