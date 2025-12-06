package com.pos.client.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pos.client.data.model.MenuItem
import com.pos.client.data.model.OptionItem

@Composable
fun ItemDetailDialog(
    menuItem: MenuItem,
    allOptions: List<OptionItem>,
    onDismiss: () -> Unit,
    onAddToCart: (Int, List<OptionItem>) -> Unit
) {
    var quantity by remember { mutableIntStateOf(1) }
    val selectedOptions = remember { mutableStateListOf<OptionItem>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(menuItem.menuName) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("価格: ¥${menuItem.price}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (quantity > 1) quantity-- }, modifier = Modifier.background(Color.LightGray, CircleShape)) {
                        Text("-", style = MaterialTheme.typography.headlineSmall)
                    }
                    Text(quantity.toString(), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 24.dp))
                    IconButton(onClick = { quantity++ }, modifier = Modifier.background(Color.LightGray, CircleShape)) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Text("オプション選択", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp))

                allOptions.forEach { option ->
                    val isSelected = selectedOptions.contains(option)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) selectedOptions.remove(option)
                                else selectedOptions.add(option)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null // Rowのclickableで制御するためnull
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option.optionName)
                        Spacer(modifier = Modifier.weight(1f))
                        if (option.price > 0) {
                            Text("+¥${option.price}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onAddToCart(quantity, selectedOptions.toList())
            }) {
                Text("カートに入れる")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
}