package com.nidoham.charlink.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.nidoham.charlink.model.Message
import com.nidoham.charlink.viewmodel.ChatViewModel

// ---------------------------------------------------------
// THEME COLORS
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
// MAIN SCREEN
// ---------------------------------------------------------
@Composable
@Preview(showBackground = true)
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val colors = getChatColors()

    // Get Intent Data
    val intent = (context as? Activity)?.intent
    val characterId = intent?.getStringExtra("cid") ?: "test_character_id"
    val name = intent?.getStringExtra("name") ?: "Unknown"
    val photo = intent?.getStringExtra("photo") ?: ""

    // 1. Start listening to Firebase when characterId changes
    // FIX: Changed startListeningToMessages -> startChatWithCharacter
    LaunchedEffect(characterId) {
        viewModel.startChatWithCharacter(characterId)
    }

    // 2. Observe the StateFlow from ViewModel
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.ime,
        topBar = {
            ChatHeader(
                name = name,
                photo = photo,
                colors = colors,
                onBackClick = { (context as? Activity)?.finish() },
                onMenuClick = { /* Menu action */ }
            )
        },
        bottomBar = {
            ChatInputArea(
                colors = colors,
                onMessageSent = { text ->
                    viewModel.sendMessage(text, characterId)
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
                colors = colors,
                onLongPress = { msg ->
                    viewModel.deleteMessage(characterId, msg.id)
                }
            )
        }
    }
}

// ---------------------------------------------------------
// MESSAGE LIST
// ---------------------------------------------------------
@Composable
fun MessageList(
    messages: List<Message>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    colors: ChatColorPalette,
    onLongPress: (Message) -> Unit
) {
    // Auto-scroll logic
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val latest = messages.firstOrNull()
            // Scroll to bottom if latest message is from me, or if we are already near bottom
            if (latest?.sentByUser == true || listState.firstVisibleItemIndex < 2) {
                listState.animateScrollToItem(0)
            }
        }
    }

    if (messages.isEmpty()) {
        EmptyChatState(colors)
    } else {
        SelectionContainer {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(top = 16.dp, bottom = 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    count = messages.size,
                    key = { index -> messages[index].id }
                ) { index ->
                    val message = messages[index]
                    val olderMsg = messages.getOrNull(index + 1)
                    val newerMsg = messages.getOrNull(index - 1)

                    val isFirstInGroup = olderMsg?.senderId != message.senderId
                    val isLastInGroup = newerMsg?.senderId != message.senderId

                    val showDateHeader = olderMsg == null ||
                            message.smartDateDisplay != olderMsg.smartDateDisplay

                    Column(modifier = Modifier.wrapContentWidth()) {
                        if (showDateHeader) {
                            DateSeparator(message.smartDateDisplay, colors)
                        }

                        // ROW: Message Bubble
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = if (message.sentByUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(modifier = Modifier.widthIn(max = 300.dp)) {
                                MessageBubbleOptimized(
                                    message = message,
                                    colors = colors,
                                    isFirstInGroup = isFirstInGroup,
                                    isLastInGroup = isLastInGroup
                                )
                            }
                        }

                        // Spacing between bubbles
                        if (isLastInGroup) {
                            Spacer(modifier = Modifier.height(8.dp))
                        } else {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// MESSAGE BUBBLE
// ---------------------------------------------------------
@Composable
fun MessageBubbleOptimized(
    message: Message,
    colors: ChatColorPalette,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean
) {
    val isMe = message.sentByUser

    // Bubble Corner Logic
    val topStart = if (isMe) 18.dp else if (isFirstInGroup) 18.dp else 4.dp
    val topEnd = if (isMe) if (isFirstInGroup) 18.dp else 4.dp else 18.dp
    val bottomStart = if (isMe) 18.dp else if (isLastInGroup) 18.dp else 4.dp
    val bottomEnd = if (isMe) if (isLastInGroup) 18.dp else 4.dp else 18.dp

    Surface(
        shape = RoundedCornerShape(
            topStart = topStart,
            topEnd = topEnd,
            bottomStart = bottomStart,
            bottomEnd = bottomEnd
        ),
        color = if (isMe) colors.bubbleMe else colors.bubbleOther,
        modifier = Modifier.wrapContentWidth(),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.text ?: "",
                color = if (isMe) colors.textOnMe else colors.textOnOther,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
        }
    }
}

// ---------------------------------------------------------
// EMPTY STATE & HELPERS
// ---------------------------------------------------------
@Composable
fun EmptyChatState(colors: ChatColorPalette) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = colors.inputBackground,
                modifier = Modifier.size(100.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    modifier = Modifier.padding(24.dp),
                    tint = colors.textSecondary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No messages yet",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "Say hello!",
                color = colors.textSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun DateSeparator(dateString: String, colors: ChatColorPalette) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Surface(
            color = colors.inputBackground.copy(alpha = 0.8f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = dateString,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        }
    }
}

// ---------------------------------------------------------
// HEADER
// ---------------------------------------------------------
@Composable
fun ChatHeader(
    name: String,
    photo: String,
    colors: ChatColorPalette,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Surface(
        color = colors.background,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, bottom = 8.dp, start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.bubbleMe)
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
                    if (photo.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(photo),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
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
                    // Online dot
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
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 16.sp,
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
// INPUT AREA
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
            .imePadding()
            .navigationBarsPadding()
    ) {
        Column {
            HorizontalDivider(color = colors.divider, thickness = 0.5.dp)

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(modifier = Modifier.padding(bottom = 8.dp)) {
                    Icon(
                        Icons.Default.AddCircle, null,
                        tint = colors.bubbleMe,
                        modifier = Modifier.size(28.dp).clickable { }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Image, null,
                        tint = colors.bubbleMe,
                        modifier = Modifier.size(28.dp).clickable { }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
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
                            Text("Type a message...", color = colors.textSecondary, fontSize = 15.sp)
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

                Spacer(modifier = Modifier.width(8.dp))

                Box(modifier = Modifier.padding(bottom = 8.dp)) {
                    if (text.isNotBlank()) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send, null,
                            tint = colors.bubbleMe,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable {
                                    onMessageSent(text.trim())
                                    text = ""
                                }
                        )
                    } else {
                        Icon(
                            Icons.Default.ThumbUp, null,
                            tint = colors.bubbleMe,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onMessageSent("👍") }
                        )
                    }
                }
            }
        }
    }
}