package com.ismartcoding.plain.ui.page.chat

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatEditTextPage(
    navController: NavHostController,
    content: String,
    onSave: (String) -> Unit,
) {
    var inputValue by remember { mutableStateOf(content) }
    val focusManager = LocalFocusManager.current

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(Res.string.edit_text),
                actions = {
                    PIconButton(
                        icon = Res.drawable.save,
                        contentDescription = stringResource(Res.string.save),
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        if (inputValue.isNotEmpty()) {
                            onSave(inputValue)
                            focusManager.clearFocus()
                            navController.navigateUp()
                        }
                    }
                })
        },
        content = { paddingValues ->
            OutlinedTextField(
                value = inputValue,
                onValueChange = { inputValue = it },
                modifier =
                Modifier
                    .padding(start = 16.dp, end = 16.dp, top = paddingValues.calculateTopPadding())
                    .imePadding()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default,
                shape = RoundedCornerShape(8.dp),
            )
        },
    )
}
