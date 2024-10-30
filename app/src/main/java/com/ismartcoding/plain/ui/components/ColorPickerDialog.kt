package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.ClipboardTextField
import com.ismartcoding.plain.ui.base.colorpicker.ColorEnvelope
import com.ismartcoding.plain.ui.base.colorpicker.HsvColorPicker
import com.ismartcoding.plain.ui.base.colorpicker.rememberColorPickerController
import com.ismartcoding.plain.ui.theme.palette.safeHexToColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    title: String,
    initValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val colorPickerController = rememberColorPickerController()
    var customColorValue by remember { mutableStateOf(initValue) }
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(customColorValue)
                }
            ) {
                Text(stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(id = R.string.cancel))
            }
        },
        title = {
            Text(text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(300.dp),
                    controller = colorPickerController,
                    initialColor = customColorValue.safeHexToColor(),
                    onColorChanged = { colorEnvelope: ColorEnvelope ->
                        customColorValue = colorEnvelope.hexCode
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    ClipboardTextField(
                        value = customColorValue,
                        modifier = Modifier.weight(0.6f),
                        placeholder = stringResource(R.string.primary_color_hint),
                        onValueChange = {
                            customColorValue = it
                        },
                        onConfirm = {
                            onConfirm(it)
                        }
                    )
                    Card(
                        modifier = Modifier
                            .height(50.dp)
                            .width(50.dp),
                        shape = RoundedCornerShape(CornerSize(5.dp)),
                        colors = CardColors(
                            containerColor = customColorValue.safeHexToColor(),
                            contentColor = contentColorFor(backgroundColor = customColorValue.safeHexToColor()),
                            disabledContainerColor = Color.LightGray,
                            disabledContentColor = Color.LightGray
                        )
                    ) {}
                }
            }
        })
}