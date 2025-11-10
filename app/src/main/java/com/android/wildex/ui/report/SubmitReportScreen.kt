package com.android.wildex.ui.report

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SubmitReportScreen(
    viewModel: SubmitReportScreenViewModel = viewModel(),
    onSubmitted: () -> Unit = {},
    onGoBack: () -> Unit = {}
) {}
