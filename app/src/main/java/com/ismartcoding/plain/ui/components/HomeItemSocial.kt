package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

@Composable
fun HomeItemSocial(
    navController: NavHostController,
) {
    Column {
        Subtitle(
            text = stringResource(R.string.home_item_title_social)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GridItem(icon = Icons.Outlined.Message, stringResource(id = R.string.messages), modifier = Modifier.weight(1f)) {
                SmsDialog().show()
            }
            Spacer(modifier = Modifier.width(8.dp))
            GridItem(icon = Icons.Outlined.Contacts, stringResource(id = R.string.contacts), modifier = Modifier.weight(1f)) {
                ContactsDialog().show()
            }
            Spacer(modifier = Modifier.width(8.dp))
            GridItem(icon = Icons.Outlined.Call, stringResource(id = R.string.calls), modifier = Modifier.weight(1f)) {
                CallsDialog().show()
            }
            Spacer(modifier = Modifier.width(8.dp))
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
