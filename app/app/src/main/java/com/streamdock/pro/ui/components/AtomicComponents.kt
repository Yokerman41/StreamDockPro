package com.streamdock.pro.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        color = Color(0xFF6366F1), // Indigo/Blueish
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

@Composable
fun StyledInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        Surface(
            color = Color(0xFF161921),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF2D313E))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(
                        text = label,
                        color = Color(0xFF9499B0),
                        fontSize = 10.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp
                            ),
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = keyboardOptions,
                            decorationBox = { innerTextField ->
                                if (value.isEmpty()) {
                                    Text(
                                        text = placeholder,
                                        color = Color(0xFF4B4F5E),
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        trailingIcon?.invoke()
                    }
                }
            }
        }
    }
}

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF2196F3),
    contentColor: Color = Color.White,
    icon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon?.invoke()
            if (icon != null) Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text.uppercase(),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
