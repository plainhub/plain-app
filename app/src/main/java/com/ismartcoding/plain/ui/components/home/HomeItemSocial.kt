package com.ismartcoding.plain.ui.components.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Message
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
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
    itemWidth: Dp,
) {
    Column {
        Subtitle(
            text = stringResource(R.string.home_item_title_social),
        )
        HomeItemFlow {
            GridItem(
                icon = Icons.Outlined.Message,
                stringResource(id = R.string.messages),
                modifier = Modifier.width(itemWidth),
            ) {
                SmsDialog().show()
            }
            GridItem(
                icon = Icons.Outlined.Contacts,
                stringResource(id = R.string.contacts),
                modifier = Modifier.width(itemWidth),
            ) {
                ContactsDialog().show()
            }
            GridItem(
                icon = Icons.Outlined.Call,
                stringResource(id = R.string.calls),
                modifier = Modifier.width(itemWidth),
            ) {
                CallsDialog().show()
            }
        }
    }
}
