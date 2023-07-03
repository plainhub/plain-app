package com.ismartcoding.plain.ui.home.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.call.CallsDialog
import com.ismartcoding.plain.ui.contact.ContactsDialog
import com.ismartcoding.plain.ui.sms.SmsDialog

@Composable
fun HomeItemSocial() {
    Column {
        Subtitle(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(R.string.home_item_title_social)
        )
        PListItem(
            title = stringResource(R.string.messages),
            showMore = true,
            onClick = {
                SmsDialog().show()
            },
        )
        PListItem(
            title = stringResource(R.string.contacts),
            showMore = true,
            onClick = {
                ContactsDialog().show()
            },
        )
        PListItem(
            title = stringResource(R.string.calls),
            showMore = true,
            onClick = {
                CallsDialog().show()
            },
        )
    }
}
