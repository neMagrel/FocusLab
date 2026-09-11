package com.example.focuslab.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FocusPrimaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("focus_primary_button")
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CancelFocusButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val errorColor = MaterialTheme.colorScheme.error
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, errorColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = errorColor),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("cancel_focus_button")
    ) {
        Text(text = "Отменить фокус", fontWeight = FontWeight.SemiBold)
    }
}
