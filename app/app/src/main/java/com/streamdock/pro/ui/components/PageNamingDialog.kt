package com.streamdock.pro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.streamdock.pro.data.repository.StreamDockRepository
import com.streamdock.pro.ui.viewmodel.StreamDockViewModel

@Composable
fun PageNamingDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf(StreamDockRepository.DEFAULT_ROWS.toString()) }
    var cols by remember { mutableStateOf(StreamDockRepository.DEFAULT_COLS.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1B26),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = "NUEVA PÁGINA",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(16.dp))

                StyledInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre de la página",
                    placeholder = "Ej: Juegos, OBS, etc.",
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    StyledInput(
                        value = rows,
                        onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) rows = it },
                        label = "Filas (1-${StreamDockViewModel.MAX_GRID_SIZE})",
                        placeholder = StreamDockRepository.DEFAULT_ROWS.toString(),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    StyledInput(
                        value = cols,
                        onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) cols = it },
                        label = "Columnas (1-${StreamDockViewModel.MAX_GRID_SIZE})",
                        placeholder = StreamDockRepository.DEFAULT_COLS.toString(),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCELAR", color = Color(0xFF9499B0))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val r = (rows.toIntOrNull() ?: StreamDockRepository.DEFAULT_ROWS)
                                .coerceIn(1, StreamDockViewModel.MAX_GRID_SIZE)
                            val c = (cols.toIntOrNull() ?: StreamDockRepository.DEFAULT_COLS)
                                .coerceIn(1, StreamDockViewModel.MAX_GRID_SIZE)
                            val trimmed = name.trim()
                            if (trimmed.isNotEmpty()) onConfirm(trimmed, r, c)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = name.isNotBlank(),
                    ) {
                        Text("CREAR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
