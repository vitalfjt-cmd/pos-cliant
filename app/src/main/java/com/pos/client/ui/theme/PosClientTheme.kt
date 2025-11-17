// com/pos/client/ui/theme/PosClientTheme.kt (新規作成)

package com.pos.client.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

val Red40 = androidx.compose.ui.graphics.Color(0xFFCF6679) // エラーを避けるためColorを直接定義

@Composable
fun PosClientTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Red40,
            secondary = Red40,
            error = Red40
        ),
        content = content
    )
}