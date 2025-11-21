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
import com.nidoham.charlink.model.Messages
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ---------------------------------------------------------
// THEME COLORS (Your Original Theme)
// ---------------------------------------------------------
@Composable
fun getChatColors(): ChatColorPalette {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        ChatColorPalette(
            background = Color.Black,
            surface = Color.Black,
            textPrimary = Color.White,
            textSecondary = Color(0xFFB0B3B8),
            bubbleMe = Color(0xFF0084FF), // Messenger Blue
            bubbleOther = Color(0xFF303030),
            textOnMe = Color.White,
            textOnOther = Color.White,
            inputBackground = Color(0xFF303030),
            divider = Color(0xFF2A2A2A)
        )
    } else {
        ChatColorPalette(
            background = Color.White,
            surface = Color.White,
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
// MAIN SCREEN (Drawer Removed, Logic Fixed)
// ---------------------------------------------------------
@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val colors = getChatColors()

    val intent = (context as? Activity)?.intent
    val contactId = intent?.getStringExtra("cid") ?: "12345"
    val contactName = intent?.getStringExtra("name") ?: "User Name"
    val contactPhoto = intent?.getStringExtra("photo") ?: ""

    // FIX 1: Message list state
    val messages = remember { mutableStateListOf<Messages>() }
    val listState = rememberLazyListState()

    // FIX 2: Auto-scroll to bottom (Index 0) when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    // NOTE: Drawer wrapper removed here
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.ime, // FIX 3: Handles Keyboard padding
        topBar = {
            ChatHeader(
                contactName = contactName,
                contactPhoto = contactPhoto,
                colors = colors,
                onBackClick = { (context as? Activity)?.finish() },
                onMenuClick = {
                    // Drawer removed, you can put Profile Detail Intent here
                }
            )
        },
        bottomBar = {
            ChatInputArea(
                colors = colors,
                onMessageSent = { text ->
                    val newMessage = Messages.Builder()
                        .id(System.currentTimeMillis().toString())
                        .text(text)
                        .timestamp(System.currentTimeMillis())
                        .sentByUser(true)
                        .senderId("current_user")
                        .receiverId(contactId)
                        .status(Messages.MessageStatus.SENT)
                        .build()

                    // FIX 4: Add to Index 0 because we use Reverse Layout
                    messages.add(0, newMessage)
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
            MessageList(
                messages = messages,
                listState = listState,
                colors = colors
            )
        }
    }
}

// ---------------------------------------------------------
// MESSAGE LIST (Reverse Layout Applied)
// ---------------------------------------------------------
@Composable
fun MessageList(
    messages: List<Messages>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    colors: ChatColorPalette
) {
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
        // FIX 5: reverseLayout = true (Items start from bottom)
        LazyColumn(
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = messages,
                key = { it.id ?: UUID.randomUUID().toString() }
            ) { message ->
                MessageBubble(message, colors)
            }
        }
    }
}

// ---------------------------------------------------------
// MESSAGE BUBBLE (Original Design)
// ---------------------------------------------------------
@Composable
fun MessageBubble(message: Messages, colors: ChatColorPalette) {
    val isMe = message.sentByUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp
            ),
            color = if (isMe) colors.bubbleMe else colors.bubbleOther,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.text ?: "",
                    color = if (isMe) colors.textOnMe else colors.textOnOther,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        color = if (isMe) colors.textOnMe.copy(alpha = 0.7f)
                        else colors.textOnOther.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )

                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIndicator(message.status, colors)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageStatusIndicator(status: Messages.MessageStatus?, colors: ChatColorPalette) {
    val icon = when (status) {
        Messages.MessageStatus.SENT -> Icons.Default.Done
        Messages.MessageStatus.DELIVERED -> Icons.Default.DoneAll
        Messages.MessageStatus.READ -> Icons.Default.DoneAll
        else -> null
    }

    icon?.let {
        Icon(
            imageVector = it,
            contentDescription = status?.name,
            modifier = Modifier.size(14.dp),
            tint = if (status == Messages.MessageStatus.READ)
                Color(0xFF0084FF)
            else colors.textOnMe.copy(alpha = 0.7f)
        )
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// ---------------------------------------------------------
// HEADER (Your Original Header)
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
        color = colors.background,
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
            // Drawer removed, so this just stays as an Info button or does nothing
            IconButton(onClick = onMenuClick) { Icon(Icons.Default.Info, "Info", tint = colors.bubbleMe) }
        }
    }
}

// ---------------------------------------------------------
// INPUT AREA (Your Original Rich Input)
// ---------------------------------------------------------
@Composable
fun ChatInputArea(
    colors: ChatColorPalette,
    onMessageSent: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Surface(
        color = colors.background,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding() // Keyboard Fix
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
                        onClick = {
                            onMessageSent(text.trim())
                            text = ""
                        },
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