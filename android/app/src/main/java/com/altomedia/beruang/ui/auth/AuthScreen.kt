package com.altomedia.beruang.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.altomedia.beruang.ui.theme.*

@Composable
fun AuthScreen(vm: AuthViewModel = hiltViewModel()) {
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val info by vm.info.collectAsState()
    var mode by remember { mutableStateOf("login") }
    var phone by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row {
                Text("BERU", color = Green, fontSize = 44.sp, fontWeight = FontWeight.Black)
                Text("ANG", color = Gold, fontSize = 44.sp, fontWeight = FontWeight.Black)
            }
            Text("A friendly place to roar together.", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(26.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
            ) {
                Column(Modifier.padding(22.dp)) {
                    error?.let {
                        Box(
                            Modifier.fillMaxWidth().background(Danger.copy(alpha = .12f), RoundedCornerShape(12.dp)).padding(12.dp)
                        ) { Text(it, color = Danger, fontSize = 13.sp) }
                        Spacer(Modifier.height(12.dp))
                    }
                    info?.let {
                        Box(
                            Modifier.fillMaxWidth().background(GreenSoft, RoundedCornerShape(12.dp)).padding(12.dp)
                        ) { Text(it, color = Green, fontSize = 13.sp) }
                        Spacer(Modifier.height(12.dp))
                    }

                    if (mode == "signup") {
                        Field("Full name", name) { name = it }
                    }
                    Field("Phone number", phone, KeyboardType.Phone) { phone = it }
                    if (mode == "signup") {
                        Text("Contoh: 0812xxxxxxx — harus diawali 08", color = Muted, fontSize = 11.sp)
                    }
                    Field("Password", pass, KeyboardType.Password) { pass = it }
                    if (mode == "signup") {
                        Text("6+ characters required.", color = Muted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (mode == "login") vm.login(phone.trim(), pass) else vm.signUp(name.trim(), phone.trim(), pass)
                        },
                        enabled = !loading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (mode == "login") "Log in" else "Create account", fontWeight = FontWeight.SemiBold) }

                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (mode == "login") "No account?" else "Already have an account?", color = Muted, fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        TextButton(onClick = { mode = if (mode == "login") "signup" else "login"; vm.clear() }) {
                            Text(if (mode == "login") "Sign up" else "Log in", color = GreenBright, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    type: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = type),
            visualTransformation = if (type == KeyboardType.Password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Bg, unfocusedContainerColor = Bg,
                focusedBorderColor = Green, unfocusedBorderColor = Line,
                focusedTextColor = Text, unfocusedTextColor = Text
            )
        )
    }
}
