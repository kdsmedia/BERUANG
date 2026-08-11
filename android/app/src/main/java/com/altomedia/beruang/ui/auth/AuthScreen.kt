package com.altomedia.beruang.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.altomedia.beruang.ui.theme.*

@Composable
fun AuthScreen(vm: AuthViewModel = hiltViewModel()) {
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val info by vm.info.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf("login") }
    var phone by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo wordmark with brand gradient
            Box(
                Modifier.padding(bottom = 8.dp).clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Green, Gold))).padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Text("🐻", fontSize = 28.sp)
            }
            Row {
                Text("BERU", color = Green, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Text("ANG", color = Gold, fontSize = 34.sp, fontWeight = FontWeight.Black)
            }
            Text("A friendly place to roar together", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 28.dp))

            // Error banner
            error?.let {
                Box(
                    Modifier.fillMaxWidth().padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Danger.copy(alpha = .1f))
                        .border(1.dp, Danger.copy(alpha = .4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) { Text(it, color = Danger, fontSize = 13.sp) }
            }
            info?.let {
                Box(
                    Modifier.fillMaxWidth().padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GreenSoft)
                        .padding(12.dp)
                ) { Text(it, color = Green, fontSize = 13.sp) }
            }

            if (mode == "signup") {
                IgField("Full name", name, Icons.Filled.Person) { name = it }
            }
            IgField("Phone number", phone, Icons.Filled.Phone, KeyboardType.Phone) { phone = it }
            if (mode == "signup") {
                Text("Format: 0812xxxxxxx (must start with 08)", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 6.dp))
            }
            IgField("Password", pass, Icons.Filled.Lock, KeyboardType.Password) { pass = it }
            if (mode == "signup") {
                Text("6+ characters required", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 12.dp))
            } else {
                Spacer(Modifier.height(14.dp))
            }

            Button(
                onClick = {
                    if (mode == "login") vm.login(phone.trim(), pass) else vm.signUp(name.trim(), phone.trim(), pass)
                },
                enabled = !loading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White, disabledContainerColor = Green.copy(alpha = .5f)),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (loading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else Text(if (mode == "login") "Log in" else "Create account", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(color = Line, modifier = Modifier.weight(1f))
                Text(if (mode == "login") "OR" else "ALREADY HAVE AN ACCOUNT?", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp))
                HorizontalDivider(color = Line, modifier = Modifier.weight(1f))
            }

            TextButton(onClick = { mode = if (mode == "login") "signup" else "login"; vm.clear() }) {
                Text(if (mode == "login") "Create new account" else "Log in to existing account", color = GreenBright, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun IgField(
    label: String,
    value: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    type: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        OutlinedTextField(
            value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp)) },
            keyboardOptions = KeyboardOptions(keyboardType = type),
            visualTransformation = if (type == KeyboardType.Password) PasswordVisualTransformation() else VisualTransformation.None,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Surface2, unfocusedContainerColor = Surface2,
                focusedBorderColor = Green, unfocusedBorderColor = Line,
                focusedTextColor = Text, unfocusedTextColor = Text,
                cursorColor = Green
            )
        )
    }
}
