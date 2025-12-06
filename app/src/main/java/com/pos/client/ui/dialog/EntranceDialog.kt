package com.pos.client.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pos.client.data.model.MenuBook

@Composable
fun EntranceDialog(
    tableNumber: String,
    menuBooks: List<MenuBook>,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit // count, bookId
) {
    var count by remember { mutableIntStateOf(2) }
    // 選択されたブックID (初期値はリストの先頭、なければ1)
    var selectedBookId by remember { mutableIntStateOf(menuBooks.firstOrNull()?.bookId ?: 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("入店受付 (Table $tableNumber)") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // --- 人数選択 ---
                Text("ご来店人数", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (count > 1) count-- }, modifier = Modifier.background(Color.LightGray, CircleShape)) {
                        Text("-", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "$count 名", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
                    IconButton(onClick = { count++ }, modifier = Modifier.background(Color.LightGray, CircleShape)) {
                        Icon(Icons.Default.Add, contentDescription = "増やす")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // --- メニューブック選択 ---
                Text("メニュー種別", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                menuBooks.forEach { book ->
                    val isSelected = (book.bookId == selectedBookId)
                    val color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
                    val textColor = if (isSelected) Color.White else Color.Black

                    OutlinedButton(
                        onClick = { selectedBookId = book.bookId },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (isSelected) color else Color.Transparent),
                        border = if(isSelected) null else ButtonDefaults.outlinedButtonBorder
                    ) {
                        Text(book.bookName, color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(count, selectedBookId) }) {
                Text("案内する")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}