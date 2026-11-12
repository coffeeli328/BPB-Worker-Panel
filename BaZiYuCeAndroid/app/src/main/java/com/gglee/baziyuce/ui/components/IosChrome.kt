package com.gglee.baziyuce.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gglee.baziyuce.ui.theme.Ink

/** Compact inline nav title like iOS `.navigationBarTitleDisplayMode(.inline)`. */
@Composable
fun InlineNavTitle(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            color = Ink,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
