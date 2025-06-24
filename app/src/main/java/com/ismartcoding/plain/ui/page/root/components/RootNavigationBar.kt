package com.ismartcoding.plain.ui.page.root.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.theme.navBarBackground
import com.ismartcoding.plain.ui.theme.navBarUnselectedColor

@Composable
fun RootNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navBarColor = MaterialTheme.colorScheme.navBarBackground
    val unselectedColor = MaterialTheme.colorScheme.navBarUnselectedColor
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(navBarColor)
            .padding(top = 8.dp)
            .navigationBarsPadding(), // This will automatically avoid the system navigation gesture area
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onTabSelected(RootTabType.HOME.value) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painterResource(R.drawable.house),
                contentDescription = stringResource(R.string.home),
                modifier = Modifier.size(24.dp),
                tint = if (selectedTab == RootTabType.HOME.value)
                    MaterialTheme.colorScheme.primary 
                else 
                    unselectedColor
            )
        }

        IconButton(
            onClick = { onTabSelected(RootTabType.CHAT.value) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painterResource(R.drawable.message_circle),
                contentDescription = stringResource(R.string.chat),
                modifier = Modifier.size(24.dp),
                tint = if (selectedTab == RootTabType.CHAT.value)
                    MaterialTheme.colorScheme.primary
                else
                    unselectedColor
            )
        }

        IconButton(
            onClick = { onTabSelected(RootTabType.AUDIO.value) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painterResource(R.drawable.music),
                contentDescription = stringResource(R.string.audios),
                modifier = Modifier.size(24.dp),
                tint = if (selectedTab == RootTabType.AUDIO.value)
                    MaterialTheme.colorScheme.primary
                else
                    unselectedColor
            )
        }
        
        IconButton(
            onClick = { onTabSelected(RootTabType.IMAGES.value) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painterResource(R.drawable.image),
                contentDescription = stringResource(R.string.images),
                modifier = Modifier.size(24.dp),
                tint = if (selectedTab == RootTabType.IMAGES.value)
                    MaterialTheme.colorScheme.primary 
                else 
                    unselectedColor
            )
        }
        

        
        IconButton(
            onClick = { onTabSelected(RootTabType.VIDEOS.value) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painterResource(R.drawable.video),
                contentDescription = stringResource(R.string.videos),
                modifier = Modifier.size(24.dp),
                tint = if (selectedTab == RootTabType.VIDEOS.value)
                    MaterialTheme.colorScheme.primary 
                else 
                    unselectedColor
            )
        }

    }
}