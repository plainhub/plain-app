package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Message
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.GridItem
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.call.CallsDialog
import com.ismartcoding.plain.ui.contact.ContactsDialog
import com.ismartcoding.plain.ui.sms.SmsDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeItemSocial(
    navController: NavHostController,
) {
    Column {
        Subtitle(
            text = stringResource(R.string.home_item_title_social)
        )
        HomeItemFlow {
            GridItem(icon = Icons.Outlined.Message, stringResource(id = R.string.messages), modifier = Modifier.weight(1f)) {
                SmsDialog().show()
            }
            GridItem(icon = Icons.Outlined.Contacts, stringResource(id = R.string.contacts), modifier = Modifier.weight(1f)) {
                ContactsDialog().show()
            }
            GridItem(icon = Icons.Outlined.Call, stringResource(id = R.string.calls), modifier = Modifier.weight(1f)) {
                CallsDialog().show()
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
