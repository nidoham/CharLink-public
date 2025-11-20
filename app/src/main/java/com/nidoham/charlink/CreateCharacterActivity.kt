package com.nidoham.charlink

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.nidoham.charlink.ui.theme.CharLinkTheme
import com.nidoham.charlink.viewmodel.CreateCharState
import com.nidoham.charlink.viewmodel.CreateCharacterViewModel

class CreateCharacterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CharLinkTheme {
                CreateCharacterScreen(
                    onBackClick = { finish() },
                    onSuccess = { finish() } // Close activity on success
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateCharacterScreen(
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: CreateCharacterViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Form States
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var backgroundStory by remember { mutableStateOf("") }
    var addressAs by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Traits
    val traitsList = listOf("Cute", "Loving", "Bossy", "Shy", "Sweet", "Witty", "Bold", "Outgoing")
    val selectedTraits = remember { mutableStateListOf<String>() }

    // Image Picker
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    // Handle Events (Success/Error)
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is CreateCharState.Success -> {
                Toast.makeText(context, "Character Created!", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            is CreateCharState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Create a character", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Image Picker ---
            Card(
                modifier = Modifier
                    .size(120.dp)
                    .clickable { launcher.launch("image/*") },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF9C7FE8))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Selected Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Person, "Add Photo", tint = Color.White, modifier = Modifier.size(60.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Inputs ---
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Age") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // --- Traits ---
            Text("Personality Traits", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                traitsList.forEach { trait ->
                    val selected = selectedTraits.contains(trait)
                    FilterChip(
                        selected = selected,
                        onClick = { if (selected) selectedTraits.remove(trait) else selectedTraits.add(trait) },
                        label = { Text(trait) },
                        leadingIcon = if (selected) { { Icon(Icons.Filled.Check, null) } } else null
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = backgroundStory,
                onValueChange = { backgroundStory = it },
                label = { Text("Background Story") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = addressAs,
                onValueChange = { addressAs = it },
                label = { Text("How should they address you?") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))

            // --- Button ---
            Button(
                onClick = {
                    viewModel.createCharacter(
                        context = context,
                        imageUri = imageUri,
                        name = name,
                        age = age,
                        traits = selectedTraits,
                        backgroundStory = backgroundStory,
                        addressAs = addressAs
                    )
                },
                enabled = uiState !is CreateCharState.Loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C7FE8))
            ) {
                if (uiState is CreateCharState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Continue", fontSize = 18.sp)
                }
            }
        }
    }
}