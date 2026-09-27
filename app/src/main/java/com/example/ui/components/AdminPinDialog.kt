package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TealPrimary

@Composable
fun AdminPinDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    verifyPin: (String) -> Boolean
) {
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = TealPrimary
            )
        },
        title = {
            Text(
                text = "بوابة الإدارة السرية",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "يرجى إدخال رمز المرور (PIN) للوصول إلى لوحة التحكم وإدارة بيانات الأطباء:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 32) {
                            pinText = it
                            errorMessage = null
                        }
                    },
                    label = { Text("رمز المرور (PIN)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (verifyPin(pinText.trim())) {
                        onSuccess()
                    } else {
                        errorMessage = "رمز المرور غير صحيح! حاول مرة أخرى"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("admin_pin_confirm")
            ) {
                Text("دخول")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("admin_pin_cancel")
            ) {
                Text("إلغاء")
            }
        }
    )
}
