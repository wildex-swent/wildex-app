package com.android.wildex.ui.report

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Screen displaying the Submit Report Screen.
 *
 * @param viewModel The ViewModel managing the state of the Submit Report Screen.
 * @param onSubmitted Callback invoked when the report has been successfully submitted.
 * @param onGoBack Callback invoked when the user wants to go back to the previous screen.
 */
@Composable
fun SubmitReportScreen(
    viewModel: SubmitReportScreenViewModel = viewModel(),
    onSubmitted: () -> Unit = {},
    onGoBack: () -> Unit = {}
) {}
