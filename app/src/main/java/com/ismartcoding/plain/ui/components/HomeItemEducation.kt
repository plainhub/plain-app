package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.GridItem
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.endict.VocabulariesDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeItemEducation(
    navController: NavHostController,
) {
    Column {
        Subtitle(
            text = stringResource(R.string.home_item_title_education)
        )
        HomeItemFlow {
            GridItem(
                icon = Icons.Outlined.Language,
                stringResource(id = R.string.memorize_words),
                modifier = Modifier
                    .weight(1f)
            ) {
                VocabulariesDialog().show()
            }
            Spacer(modifier = Modifier.weight(3f))
        }
    }
}
