package com.nidoham.charlink.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

// ---------------------------------------------------------
// THEME COLORS
// ---------------------------------------------------------
@Composable
fun getChatColors(): ChatColorPalette {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        ChatColorPalette(
            background = Color.Black,
            surface = Color.Black, // Same as background for seamless look
            textPrimary = Color.White,
            textSecondary = Color(0xFFB0B3B8),
            bubbleMe = Color(0xFF0084FF),
            bubbleOther = Color(0xFF303030),
            textOnMe = Color.White,
            textOnOther = Color.White,
            inputBackground = Color(0xFF303030),
            divider = Color(0xFF2A2A2A)
        )
    } else {
        ChatColorPalette(
            background = Color.White,
            surface = Color.White, // Same as background for seamless look
            textPrimary = Color(0xFF050505),
            textSecondary = Color(0xFF65676B),
            bubbleMe = Color(0xFF0084FF),
            bubbleOther = Color(0xFFF0F2F5),
            textOnMe = Color.White,
            textOnOther = Color(0xFF050505),
            inputBackground = Color(0xFFF0F2F5),
            divider = Color(0xFFE4E6EB)
        )
    }
}

data class ChatColorPalette(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val bubbleMe: Color,
    val bubbleOther: Color,
    val textOnMe: Color,
    val textOnOther: Color,
    val inputBackground: Color,
    val divider: Color
)

// ---------------------------------------------------------
// DATA CLASS
// ---------------------------------------------------------
data class ChatMessage(
    val id: String,
    val text: String,
    val isFromMe: Boolean,
    val timestamp: Long
)

// ---------------------------------------------------------
// MAIN SCREEN
// ---------------------------------------------------------
@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = getChatColors()

    val intent = (context as? Activity)?.intent
    val contactId = intent?.getStringExtra("cid") ?: "ID: 12345"
    val contactName = intent?.getStringExtra("name") ?: "User Name"
    val contactPhoto = intent?.getStringExtra("photo") ?: ""

    // Message List
    val messages = remember { mutableStateListOf<ChatMessage>() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawerContent(
                contactId = contactId,
                contactName = contactName,
                contactPhoto = contactPhoto,
                colors = colors
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = colors.background,
            // Handle insets manually for seamless background
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                ChatHeader(
                    contactName = contactName,
                    contactPhoto = contactPhoto,
                    colors = colors,
                    onBackClick = { (context as? Activity)?.finish() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            },
            bottomBar = {
                ChatInputArea(
                    colors = colors,
                    onMessageSent = { text ->
                        // Add new messages to index 0 (Bottom of the screen due to reverseLayout)
                        messages.add(0, ChatMessage(
                            id = System.currentTimeMillis().toString(),
                            text = text,
                            isFromMe = true,
                            timestamp = System.currentTimeMillis()
                        ))
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(colors.background)
            ) {
                MessageList(messages = messages, colors = colors)
            }
        }
    }
}

// ---------------------------------------------------------
// HEADER (Restored Back Icon)
// ---------------------------------------------------------
@Composable
fun ChatHeader(
    contactName: String,
    contactPhoto: String,
    colors: ChatColorPalette,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Surface(
        color = colors.background, // Matches system background
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, bottom = 12.dp, start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Restored Back Button (Main UI)
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, "Back", tint = colors.bubbleMe)
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onMenuClick() }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(40.dp)) {
                    if (contactPhoto.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(contactPhoto),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = colors.inputBackground
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.padding(8.dp),
                                tint = colors.textSecondary
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd)
                            .background(colors.background, CircleShape)
                            .padding(2.dp)
                            .background(Color(0xFF31A24C), CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = contactName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 17.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Active now",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(onClick = {}) { Icon(Icons.Default.Call, "Call", tint = colors.bubbleMe) }
            IconButton(onClick = {}) { Icon(Icons.Default.Videocam, "Video", tint = colors.bubbleMe) }
            IconButton(onClick = onMenuClick) { Icon(Icons.Default.Info, "Info", tint = colors.bubbleMe) }
        }
    }
}

// ---------------------------------------------------------
// MESSAGE LIST (Messenger Style: Bottom to Top)
// ---------------------------------------------------------
@Composable
fun MessageList(messages: List<ChatMessage>, colors: ChatColorPalette) {
    val listState = rememberLazyListState()

    if (messages.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = colors.inputBackground
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp),
                        tint = colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Say hi to your new friend!",
                    color = colors.textSecondary,
                    fontSize = 16.sp
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            // This makes the list fill from the bottom upwards (Messenger style)
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items = messages, key = { it.id }) { message ->
                MessageBubble(message, colors)
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, colors: ChatColorPalette) {
    val isMe = message.isFromMe

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp
            ),
            color = if (isMe) colors.bubbleMe else colors.bubbleOther,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(vertical = 1.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (isMe) colors.textOnMe else colors.textOnOther,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 15.sp
            )
        }
    }
}

// ---------------------------------------------------------
// INPUT AREA
// ---------------------------------------------------------
@Composable
fun ChatInputArea(
    colors: ChatColorPalette,
    onMessageSent: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Surface(
        color = colors.background, // Unified background
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(modifier = Modifier.padding(bottom = 6.dp)) {
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.AddCircle, null, tint = colors.bubbleMe)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.CameraAlt, null, tint = colors.bubbleMe)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Image, null, tint = colors.bubbleMe)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.inputBackground,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (text.isEmpty()) {
                        Text("Aa", color = colors.textSecondary, fontSize = 15.sp)
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 15.sp
                        ),
                        cursorBrush = SolidColor(colors.bubbleMe),
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(modifier = Modifier.padding(bottom = 6.dp)) {
                if (text.isNotBlank()) {
                    IconButton(
                        onClick = { onMessageSent(text.trim()); text = "" },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Send, null, tint = colors.bubbleMe)
                    }
                } else {
                    IconButton(
                        onClick = { onMessageSent("👍") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ThumbUp, null, tint = colors.bubbleMe)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// DRAWER CONTENT (Removed Back Icon)
// ---------------------------------------------------------
@Composable
fun ChatDrawerContent(
    contactId: String,
    contactName: String,
    contactPhoto: String,
    colors: ChatColorPalette
) {
    ModalDrawerSheet(
        drawerContainerColor = colors.surface,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp) // Increased top padding since icon is gone
        ) {
            // REMOVED THE BACK ARROW HERE

            // Profile Image
            if (contactPhoto.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(contactPhoto),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = colors.inputBackground
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp),
                        tint = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = contactName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Text(
                text = "ID: $contactId",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                fontWeight = FontWeight.Medium
            )
        }

        Divider(color = colors.divider)

        DrawerMenuItem(Icons.Outlined.Person, "Profile", colors)
        DrawerMenuItem(Icons.Outlined.Notifications, "Mute notifications", colors)
        DrawerMenuItem(Icons.Outlined.Search, "Search in conversation", colors)
        DrawerMenuItem(Icons.Outlined.Palette, "Theme", colors)

        Divider(color = colors.divider, modifier = Modifier.padding(vertical = 8.dp))

        DrawerMenuItem(Icons.Outlined.Block, "Block", colors, Color.Red)
        DrawerMenuItem(Icons.Outlined.Report, "Report", colors, Color.Red)
    }
}

@Composable
fun DrawerMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    colors: ChatColorPalette,
    overrideTint: Color? = null
) {
    val finalTint = overrideTint ?: colors.textPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = finalTint,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = finalTint
        )
    }
}