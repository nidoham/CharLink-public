package com.nidoham.charlink

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.nidoham.charlink.screens.ChatScreen
import com.nidoham.charlink.screens.ExploreScreen
import com.nidoham.charlink.screens.HomeScreen
import com.nidoham.charlink.screens.ProfileScreen
import com.nidoham.charlink.ui.theme.CharLinkTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CharLinkTheme {
                MainAppStructure()
            }
        }
    }
}

// ================== Navigation Routes ==================
sealed class Screen(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Chat : Screen("chat", "Chat", Icons.Filled.Email, Icons.Outlined.Email)
    data object Explore : Screen("explore", "Explore", Icons.Filled.Explore, Icons.Outlined.Explore)
    data object Profile : Screen("profile", "Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
}

data class DrawerMenuItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

// ================== Main Structure ==================
@OptIn(ExperimentalMaterial3Api::class)
@Preview()
@Composable
fun MainAppStructure() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navItems = listOf(Screen.Home, Screen.Chat, Screen.Explore, Screen.Profile)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                DrawerHeader()
                HorizontalDivider()
                DrawerBody(
                    items = listOf(
                        DrawerMenuItem("Creator Studio", Icons.Default.Edit) {},
                        DrawerMenuItem("Leaderboard", Icons.Default.Star) {},
                        DrawerMenuItem("Settings", Icons.Default.Settings) {},
                        DrawerMenuItem("Help", Icons.Default.Info) {},
                        DrawerMenuItem("About", Icons.Default.Person) {}
                    )
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "CharLink", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { }) { Icon(Icons.Default.Search, contentDescription = "Search") }
                        IconButton(onClick = { }) { Icon(Icons.Default.Notifications, contentDescription = "Notifications") }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    navItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            label = { Text(screen.title) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == screen.route) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHostContainer(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

// ================== Updated Drawer Header ==================
@Composable
fun DrawerHeader() {
    // Firebase User Data Fetching
    // Note: This will crash in @Preview unless you add a check or mock data,
    // but works on a real device if Firebase is initialized.
    val user = FirebaseAuth.getInstance().currentUser
    val userName = user?.displayName ?: "Guest User"
    val userUid = user?.uid ?: "UID: N/A"
    val photoUrl = user?.photoUrl

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE94560), Color(0xFF16213E))
                )
            )
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        Column {
            // Profile Image with Coil
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.ic_launcher_foreground) // Ensure this drawable exists
                    .error(R.drawable.ic_launcher_foreground)
                    .build(),
                contentDescription = "Profile Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color.White, CircleShape)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Username
            Text(
                text = userName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            // UID
            Text(
                text = "UID: $userUid",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun DrawerBody(items: List<DrawerMenuItem>) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        items.forEach { item ->
            NavigationDrawerItem(
                label = { Text(text = item.title) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                selected = false,
                onClick = item.onClick,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}

@Composable
fun NavHostContainer(navController: NavHostController, modifier: Modifier = Modifier) {
    // FIXED: Capture context here to use inside the composable
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = Screen.Home.route, modifier = modifier) {
        composable(Screen.Home.route) {
            HomeScreen(
                onCreateClick = {
                    // FIXED: Use 'context' instead of 'this'
                    // Make sure 'CreateCharacterActivity' exists in your project
                    val intent = Intent(context, CreateCharacterActivity::class.java)
                    context.startActivity(intent)

                    // FIXED: Cast context to Activity to access overridePendingTransition
                    if (context is Activity) {
                        context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                }
            )
        }
        composable(Screen.Chat.route) { ChatScreen() }
        composable(Screen.Explore.route) { ExploreScreen() }
        composable(Screen.Profile.route) { ProfileScreen() }
    }
}