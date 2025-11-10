package com.android.wildex.ui.report

import android.content.Context
import androidx.compose.runtime.Composable

/**
 * Screen displaying the Submit Report Form Screen.
 *
 * @param uiState The current UI state of the submit report form.
 * @param onCameraClick Callback invoked when the camera button is clicked.
 * @param onDescriptionChange Callback invoked when the description text changes.
 * @param onSubmitClick Callback invoked when the submit button is clicked.
 * @param context The context of the current state of the application.
 * @param onGoBack Callback invoked when the user wants to go back to the previous screen.
 */
@Composable
fun SubmitReportFormScreen(
    uiState: SubmitReportUiState,
    onCameraClick: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
    context: Context,
    onGoBack: () -> Unit,
) {}
