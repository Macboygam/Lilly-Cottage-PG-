package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.DormViewModel
import com.example.data.Member
import com.example.data.Payment
import com.example.ui.components.AnimatedBackdrop
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.navigationBars
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        DormitoryApp()
                    }
                }
            }
        }
    }
}

enum class AppTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Dashboard("Dashboard", Icons.Default.Dashboard),
    AddMember("Add Member", Icons.Default.PersonAdd),
    MembersList("Residents", Icons.Default.People),
    Payments("Payments", Icons.Default.AccountBalanceWallet),
    Export("Export CSV", Icons.Default.CloudDownload)
}

@Composable
fun DormitoryApp() {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val viewModel: DormViewModel = viewModel(
        factory = DormViewModel.Factory(app)
    )

    var currentTab by remember { mutableStateOf(AppTab.Dashboard) }
    var editingMember by remember { mutableStateOf<Member?>(null) }

    // Navigation interceptor for edit member screen
    val activeScreen = if (editingMember != null) {
        AppTab.AddMember
    } else {
        currentTab
    }

    val config = LocalConfiguration.current
    val isTablet = config.screenWidthDp >= 600

    AnimatedBackdrop {
        Row(modifier = Modifier.fillMaxSize()) {
            // Navigation Rail for tablets/large displays
            if (isTablet) {
                NavigationRail(
                    containerColor = Color.Transparent,
                    header = {
                        Column(
                            modifier = Modifier.padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Domain,
                                contentDescription = "Dorm icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Lilly Cottage(PG)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    modifier = Modifier.fillMaxHeight()
                ) {
                    AppTab.values().forEach { tab ->
                        val isSelected = activeScreen == tab && editingMember == null
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                editingMember = null
                                currentTab = tab
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                        )
                    }
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Main Screen Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Custom App Head
                    TopAppBarContent(
                        tab = activeScreen,
                        isEditing = editingMember != null,
                        onBack = { editingMember = null }
                    )

                    // Page Renderer with sliding transition effects
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        AnimatedContent(
                            targetState = activeScreen,
                            transitionSpec = {
                                fadeIn(animationSpec = spring()) + slideInHorizontally(
                                    initialOffsetX = { 300 }
                                ) togetherWith fadeOut(animationSpec = spring()) + slideOutHorizontally(
                                    targetOffsetX = { -300 }
                                )
                            },
                            label = "screen_transition"
                        ) { screen ->
                            when (screen) {
                                AppTab.Dashboard -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToAdd = { currentTab = AppTab.AddMember },
                                    onNavigateToList = { currentTab = AppTab.MembersList },
                                    onNavigateToPayments = { currentTab = AppTab.Payments }
                                )
                                AppTab.AddMember -> AddMemberScreen(
                                    viewModel = viewModel,
                                    editingMember = editingMember,
                                    onComplete = {
                                        editingMember = null
                                        currentTab = AppTab.MembersList
                                    }
                                )
                                AppTab.MembersList -> MembersListScreen(
                                    viewModel = viewModel,
                                    onEditMember = { member ->
                                        editingMember = member
                                    }
                                )
                                AppTab.Payments -> PaymentsScreen(
                                    viewModel = viewModel
                                )
                                AppTab.Export -> ExportScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }

                    // Bottom Navigation Bar for smartphones
                    if (!isTablet) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            AppTab.values().forEach { tab ->
                                val isSelected = activeScreen == tab && editingMember == null
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        editingMember = null
                                        currentTab = tab
                                    },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.testTag("bottom_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopAppBarContent(tab: AppTab, isEditing: Boolean, onBack: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditing) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Resident Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Column {
                    Text(
                        text = "LILLY",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 28.sp
                    )
                    Text(
                        text = "COTTAGE",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 28.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                // Subtle label indicating current tab inside display header
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// 1. DASHBOARD SCREEN
@Composable
fun DashboardScreen(
    viewModel: DormViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToPayments: () -> Unit
) {
    val members by viewModel.allMembers.collectAsStateWithLifecycle()
    val pendingMembers by viewModel.pendingMembers.collectAsStateWithLifecycle()
    val incomeMonth by viewModel.currentMonthYear.collectAsStateWithLifecycle()
    val monthlyIncomeSum by viewModel.selectedMonthIncome.collectAsStateWithLifecycle()
    val reminderDaysConfig by viewModel.reminderDaysConfig.collectAsStateWithLifecycle()

    val blockACapacity by viewModel.blockACapacity.collectAsStateWithLifecycle()
    val blockBCapacity by viewModel.blockBCapacity.collectAsStateWithLifecycle()
    val blockCCapacity by viewModel.blockCCapacity.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "System bar notifications enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission denied. You can enable notifications in Settings.", Toast.LENGTH_LONG).show()
        }
    }

    val blockACount = members.count { it.block == "Block A" && it.isActive }
    val blockBCount = members.count { it.block == "Block B" && it.isActive }
    val blockCCount = members.count { it.block == "Block C" && it.isActive }
    val activeCount = members.count { it.isActive }
    val foodCount = members.count { it.foodIncluded && it.isActive }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "LILLY COTTAGE(PG) DESK",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Manage Block A, B & C residents, rent logging, and food services locally with high contrast precision.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Automated Reminders Notification Area
        item {
            val alertMembers = members.filter {
                val diff = getDaysDifference(it.nextDueDate)
                it.isActive && diff >= 0 && diff <= reminderDaysConfig
            }
            val overdueMembersList = members.filter { it.isActive && getDaysDifference(it.nextDueDate) < 0 }

            if (alertMembers.isNotEmpty() || overdueMembersList.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    ),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "Dues alerts", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AUTOMATED ADMINISTRATION ALERTS",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (overdueMembersList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Overdue Payments (${overdueMembersList.size})", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            overdueMembersList.forEach { m ->
                                val elapsed = -getDaysDifference(m.nextDueDate)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• ${m.fullName} (${m.block})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Overdue by $elapsed Days (₹${m.rentAmount})", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        if (alertMembers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Rent Due Soon within next $reminderDaysConfig Days (${alertMembers.size})", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            alertMembers.forEach { m ->
                                val left = getDaysDifference(m.nextDueDate)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• ${m.fullName} (${m.block})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Due in $left Days (₹${m.rentAmount})", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val overdueSize = overdueMembersList.size
                                    val dueSize = alertMembers.size
                                    
                                    if (overdueSize > 0) {
                                        sendSystemNotification(
                                            context = context,
                                            title = "⚠️ Overdue Rent Warning!",
                                            text = "$overdueSize resident(s) have overdue rent! Check desk to collect dues."
                                        )
                                    }
                                    if (dueSize > 0) {
                                        sendSystemNotification(
                                            context = context,
                                            title = "🔔 Rent Due Impending",
                                            text = "$dueSize accounts have rent due within the next $reminderDaysConfig days."
                                        )
                                    }
                                    if (overdueSize == 0 && dueSize == 0) {
                                        sendSystemNotification(
                                            context = context,
                                            title = "✅ System Accounts Clear",
                                            text = "All active dormitory rent accounts are fully paid up."
                                        )
                                    }
                                    Toast.makeText(context, "System notifications pushed successfully!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().testTag("push_notification_alerts_btn")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Alerts to Notification Bar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Metrics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Total Residents",
                        value = "${members.size}",
                        icon = Icons.Default.People,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Overdue Unpaid",
                        value = "${pendingMembers.size}",
                        icon = Icons.Default.PriorityHigh,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Monthly Income ($incomeMonth)",
                        value = "₹${String.format("%,.0f", monthlyIncomeSum)}",
                        icon = Icons.Default.Paid,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Active Residents",
                        value = "$activeCount",
                        icon = Icons.Default.Check,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Total Active Food Plan",
                        value = "$foodCount",
                        icon = Icons.Default.Restaurant,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    val totalCapObj = blockACapacity + blockBCapacity + blockCCapacity
                    val totalOccupied = blockACount + blockBCount + blockCCount
                    val totalAvailable = (totalCapObj - totalOccupied).coerceAtLeast(0)
                    MetricCard(
                        title = "Available Slots / Beds",
                        value = "$totalAvailable / $totalCapObj",
                        icon = Icons.Default.Domain,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Action Keys (Quick Management Panel)
        item {
            Text(
                "Quick Management Panel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToAdd,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("quick_add_member"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Member", fontSize = 12.sp, maxLines = 1)
                }

                Button(
                    onClick = onNavigateToPayments,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("quick_record_payment"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Record")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Collect Rent", fontSize = 12.sp, maxLines = 1)
                }

                Button(
                    onClick = onNavigateToList,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(50.dp)
                        .testTag("quick_members_list"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Search Residents", fontSize = 12.sp, maxLines = 1)
                }
            }
        }

        // Reminders Alert Settings Timings Customization
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Configure Upcoming Payment Alerts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Customize warning days in advance for active residents",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1, 3, 5, 7, 10).forEach { days ->
                            val isSelected = reminderDaysConfig == days
                            val containerCol = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            val contentCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(containerCol)
                                    .clickable { viewModel.reminderDaysConfig.value = days }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$days Days",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = contentCol
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pending Payment Alerts Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PriorityHigh,
                            contentDescription = "Alert",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Pending Collection Dues",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    if (pendingMembers.isEmpty()) {
                        Text(
                            "Safe! No overdue rent payments recorded for active residents.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pendingMembers.take(3).forEach { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(p.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                        Text("${p.block} • Overdue since: ${p.nextDueDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                                    }
                                    Text("₹${p.rentAmount}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                            if (pendingMembers.size > 3) {
                                Text(
                                    "And ${pendingMembers.size - 3} other overdue members",
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Block Distribution visualizers
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Block Occupancy Breakdown",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Configured capacity & current real-time room availability",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        var showCapacityDialog by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showCapacityDialog = true },
                            modifier = Modifier.testTag("manage_capacities_btn")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Manage Capacities", tint = MaterialTheme.colorScheme.primary)
                        }

                        if (showCapacityDialog) {
                            var tempA by remember { mutableStateOf(blockACapacity.toString()) }
                            var tempB by remember { mutableStateOf(blockBCapacity.toString()) }
                            var tempC by remember { mutableStateOf(blockCCapacity.toString()) }
                            
                            AlertDialog(
                                onDismissRequest = { showCapacityDialog = false },
                                title = { Text("Configure Room Capacities", fontWeight = FontWeight.Black) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("Set the maximum number of beds/rooms for each block. These limit properties persist locally across restarts.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        
                                        OutlinedTextField(
                                            value = tempA,
                                            onValueChange = { tempA = it.filter { char -> char.isDigit() } },
                                            label = { Text("Block A Capacity") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("cap_input_a")
                                        )
                                        OutlinedTextField(
                                            value = tempB,
                                            onValueChange = { tempB = it.filter { char -> char.isDigit() } },
                                            label = { Text("Block B Capacity") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("cap_input_b")
                                        )
                                        OutlinedTextField(
                                            value = tempC,
                                            onValueChange = { tempC = it.filter { char -> char.isDigit() } },
                                            label = { Text("Block C Capacity") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("cap_input_c")
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val valA = tempA.toIntOrNull() ?: 30
                                            val valB = tempB.toIntOrNull() ?: 20
                                            val valC = tempC.toIntOrNull() ?: 15
                                            viewModel.blockACapacity.value = valA
                                            viewModel.blockBCapacity.value = valB
                                            viewModel.blockCCapacity.value = valC
                                            showCapacityDialog = false
                                        },
                                        modifier = Modifier.testTag("save_capacities_btn")
                                    ) {
                                        Text("Save Settings")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCapacityDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    BlockProgress(label = "Block A", count = blockACount, total = blockACapacity.coerceAtLeast(1), color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    BlockProgress(label = "Block B", count = blockBCount, total = blockBCapacity.coerceAtLeast(1), color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    BlockProgress(label = "Block C", count = blockCCount, total = blockCCapacity.coerceAtLeast(1), color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

@Composable
fun BoldFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.25.sp
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    title.uppercase(),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BlockProgress(label: String, count: Int, total: Int, color: Color) {
    val progress = count.toFloat() / total.toFloat()
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text("$count Occupants", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}


fun calculateAge(dobString: String): Int {
    return try {
        val parts = dobString.split("-")
        if (parts.size == 3) {
            val year = parts[0].toIntOrNull() ?: 2000
            val month = parts[1].toIntOrNull() ?: 1
            val day = parts[2].toIntOrNull() ?: 1
            val birth = java.util.Calendar.getInstance().apply {
                set(year, month - 1, day)
            }
            val today = java.util.Calendar.getInstance()
            var ageVal = today.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR)
            if (today.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) {
                ageVal--
            }
            ageVal.coerceAtLeast(0)
        } else {
            20
        }
    } catch (e: Exception) {
        20
    }
}


// 2. ADD & EDIT MEMBER SCREEN
@Composable
fun AddMemberScreen(
    viewModel: DormViewModel,
    editingMember: Member?,
    onComplete: () -> Unit
) {
    val context = LocalContext.current

    // Fields States
    var fullName by remember { mutableStateOf(editingMember?.fullName ?: "") }
    var parentName by remember { mutableStateOf(editingMember?.parentName ?: "") }
    var parentPhone by remember { mutableStateOf(editingMember?.parentPhone ?: "") }
    var gender by remember { mutableStateOf(editingMember?.gender ?: "Male") }
    var dateOfBirth by remember { mutableStateOf(editingMember?.dateOfBirth ?: "2000-01-01") }
    var mobileNumber by remember { mutableStateOf(editingMember?.mobileNumber ?: "") }
    var emailId by remember { mutableStateOf(editingMember?.emailId ?: "") }
    var aadhaarPhotoUri by remember { mutableStateOf<String?>(editingMember?.aadhaarPhotoUri) }
    var drivingLicenceNumber by remember { mutableStateOf(editingMember?.drivingLicenceNumber ?: "") }
    var address by remember { mutableStateOf(editingMember?.address ?: "") }
    var city by remember { mutableStateOf(editingMember?.city ?: "") }
    var state by remember { mutableStateOf(editingMember?.state ?: "") }
    var block by remember { mutableStateOf(editingMember?.block ?: "Block A") }
    
    // Rent
    var rentAmount by remember { mutableStateOf(editingMember?.rentAmount?.toString() ?: "5000") }
    var paymentDate by remember { mutableStateOf(editingMember?.paymentDate ?: "2026-05-26") }
    var nextDueDate by remember { mutableStateOf(editingMember?.nextDueDate ?: "2026-06-26") }
    var paymentMethod by remember { mutableStateOf(editingMember?.paymentMethod ?: "UPI") }
    var foodIncluded by remember { mutableStateOf(editingMember?.foodIncluded ?: true) }
    var isActive by remember { mutableStateOf(editingMember?.isActive ?: true) }
    var joinDate by remember { mutableStateOf(editingMember?.joinDate ?: "2026-05-26") }

    // Picker
    val pickAadhaarMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // Safely copy picked image to local app storage for permanent access
                val localUri = saveUriToInternalStorage(context, uri)
                if (localUri != null) {
                    aadhaarPhotoUri = localUri.toString()
                } else {
                    aadhaarPhotoUri = uri.toString()
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Section: Personal Details
        InputFieldCard(title = "Resident Profile") {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("full_name_field"),
                leadingIcon = { Icon(Icons.Default.Person, null) }
            )

            // Date Picker DOB
            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = { },
                readOnly = true,
                label = { Text("Date of Birth *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showDatePicker(context, dateOfBirth) { dateOfBirth = it }
                    },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker(context, dateOfBirth) { dateOfBirth = it } }) {
                        Icon(Icons.Default.CalendarToday, null)
                    }
                }
            )

            // Gender Segmented Picker
            Text("Gender Selector", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Male", "Female").forEach { g ->
                    val isSelected = gender == g
                    FilterChip(
                        selected = isSelected,
                        onClick = { gender = g },
                        label = { Text(g) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }

            OutlinedTextField(
                value = mobileNumber,
                onValueChange = { mobileNumber = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                label = { Text("Personal Mobile Number *") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Phone, null) }
            )

        }

        // Section: Parent details
        InputFieldCard(title = "Parent Relations") {
            OutlinedTextField(
                value = parentName,
                onValueChange = { parentName = it },
                label = { Text("Father / Mother Name") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.SupervisorAccount, null) }
            )
            OutlinedTextField(
                value = parentPhone,
                onValueChange = { parentPhone = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                label = { Text("Parent Contact Number") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.SupportAgent, null) }
            )
        }

        // Section: Identification Documents
        InputFieldCard(title = "KYC Verification") {
            Text(
                text = "Aadhaar Card Photo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (aadhaarPhotoUri == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (aadhaarPhotoUri != null) {
                        AsyncImage(
                            model = aadhaarPhotoUri,
                            contentDescription = "Aadhaar Card Photo Viewer",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Uploaded",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Aadhaar Photo Uploaded",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AssignmentInd,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "No Aadhaar Document Uploaded",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            try {
                                pickAadhaarMedia.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            } catch (e: Exception) {
                                Toast.makeText(context, "No gallery or photo picker application available!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("upload_aadhaar_photo_button")
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Camera")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (aadhaarPhotoUri == null) "Choose Aadhaar Photo" else "Change Aadhaar Photo")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = drivingLicenceNumber,
                onValueChange = { drivingLicenceNumber = it },
                label = { Text("Driving Licence (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Badge, null) }
            )
        }

        // Section: Local Address
        InputFieldCard(title = "Permanent Address") {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Street Address") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                leadingIcon = { Icon(Icons.Default.LocationOn, null) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("State") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section: Placement Allocation & Rent
        InputFieldCard(title = "Dorm Allocation & Fees") {
            // Block Radio Picker
            Text("Block Assignment *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Block A", "Block B", "Block C").forEach { b ->
                    val isSelected = block == b
                    BoldFilterChip(
                        selected = isSelected,
                        onClick = { block = b },
                        label = b,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = rentAmount,
                onValueChange = { rentAmount = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("Rent Price Per Month (₹) *") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.CurrencyRupee, null) }
            )

            // Service Food included
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Restaurant, contentDescription = "Food icon", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Food Services Included", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Include standard 3 meals daily", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = foodIncluded,
                    onCheckedChange = { foodIncluded = it }
                )
            }
        }

        // Section: Resident Status & Registration Date
        InputFieldCard(title = "Registration & Active Status") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Join Date
                OutlinedTextField(
                    value = joinDate,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Join Date") },
                    modifier = Modifier
                        .weight(1.2f)
                        .clickable { showDatePicker(context, joinDate) { joinDate = it } },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker(context, joinDate) { joinDate = it } }) {
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                        }
                    }
                )

                // Active / Left Member Switch
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Status", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(if (isActive) "Active" else "Left", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }
            }
        }

        // Action Buttons save/submit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onComplete,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        Toast.makeText(context, "Resident Name is required!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (mobileNumber.isBlank()) {
                        Toast.makeText(context, "Personal Mobile Number is required!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val rentDouble = rentAmount.toDoubleOrNull() ?: 0.0
                    val computedAge = calculateAge(dateOfBirth)

                    if (editingMember != null) {
                        viewModel.updateMember(
                            editingMember.copy(
                                fullName = fullName,
                                parentName = parentName,
                                parentPhone = parentPhone,
                                gender = gender,
                                age = computedAge,
                                dateOfBirth = dateOfBirth,
                                mobileNumber = mobileNumber,
                                emailId = emailId,
                                aadhaarPhotoUri = aadhaarPhotoUri,
                                drivingLicenceNumber = drivingLicenceNumber.takeIf { it.isNotBlank() },
                                address = address,
                                city = city,
                                state = state,
                                block = block,
                                rentAmount = rentDouble,
                                paymentDate = paymentDate,
                                nextDueDate = nextDueDate,
                                paymentMethod = paymentMethod,
                                foodIncluded = foodIncluded,
                                isActive = isActive,
                                joinDate = joinDate
                            )
                        )
                        Toast.makeText(context, "Resident details updated successfully!", Toast.LENGTH_SHORT).show()
                        sendSystemNotification(context, "Resident Details Updated", "$fullName's profile details have been successfully modified.")
                    } else {
                        viewModel.addMember(
                            Member(
                                fullName = fullName,
                                parentName = parentName,
                                parentPhone = parentPhone,
                                gender = gender,
                                age = computedAge,
                                dateOfBirth = dateOfBirth,
                                mobileNumber = mobileNumber,
                                emailId = emailId,
                                aadhaarPhotoUri = aadhaarPhotoUri,
                                drivingLicenceNumber = drivingLicenceNumber.takeIf { it.isNotBlank() },
                                address = address,
                                city = city,
                                state = state,
                                block = block,
                                rentAmount = rentDouble,
                                paymentDate = paymentDate,
                                nextDueDate = nextDueDate,
                                paymentMethod = paymentMethod,
                                foodIncluded = foodIncluded,
                                isActive = isActive,
                                joinDate = joinDate
                            )
                        )
                        Toast.makeText(context, "Resident registered successfully!", Toast.LENGTH_SHORT).show()
                        sendSystemNotification(context, "New Resident Registered", "$fullName has been successfully added to $block.")
                    }
                    onComplete()
                },
                modifier = Modifier
                    .weight(1.5f)
                    .testTag("save_member_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save")
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (editingMember != null) "Update Resident" else "Register Resident")
            }
        }
    }
}

@Composable
fun InputFieldCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title.uppercase(),
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}


// 3. MEMBERS / RESIDENTS LIST SCREEN
@Composable
fun MembersListScreen(
    viewModel: DormViewModel,
    onEditMember: (Member) -> Unit
) {
    val members by viewModel.filteredMembers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedBlock by viewModel.selectedBlock.collectAsStateWithLifecycle()
    val memberStatusFilter by viewModel.memberStatusFilter.collectAsStateWithLifecycle()

    var selectedProfileForPopup by remember { mutableStateOf<Member?>(null) }
    var memberToDelete by remember { mutableStateOf<Member?>(null) }

    selectedProfileForPopup?.let { activeMember ->
        MemberProfileDialog(member = activeMember, onDismiss = { selectedProfileForPopup = null })
    }

    val context = LocalContext.current

    memberToDelete?.let { mToDelete ->
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("Remove Resident", fontWeight = FontWeight.Bold) },
            text = { Text("Are you absolutely sure you want to permanently delete resident ${mToDelete.fullName} from the manager? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = mToDelete.fullName
                        viewModel.deleteMember(mToDelete)
                        Toast.makeText(context, "Resident record removed!", Toast.LENGTH_SHORT).show()
                        sendSystemNotification(context, "Resident Record Deleted", "Resident $name has been removed from the directory.")
                        memberToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search & Filters Box
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search by name or phone...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("members_search_bar"),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    } else null,
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Block Horizontal Filter Chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All Slots", "Block A", "Block B", "Block C").forEach { block ->
                        val displayBlock = if (block == "All Slots") "All" else block
                        val isSelected = selectedBlock == displayBlock
                        BoldFilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectedBlock.value = displayBlock },
                            label = block,
                            modifier = Modifier.testTag("filter_chip_${displayBlock.replace(" ", "_").lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Status Horizontal Filter Chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL residents", "ACTIVE members", "LEFT residents").forEach { label ->
                        val filterValue = when (label) {
                            "ACTIVE members" -> "Active"
                            "LEFT residents" -> "Left"
                            else -> "All"
                        }
                        val isSelected = memberStatusFilter == filterValue
                        BoldFilterChip(
                            selected = isSelected,
                            onClick = { viewModel.memberStatusFilter.value = filterValue },
                            label = label.uppercase(),
                            modifier = Modifier.testTag("filter_status_${filterValue.lowercase()}")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content list
        if (members.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Group, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No matching residents found.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(members, key = { it.id }) { member ->
                    var isExpanded by remember { mutableStateOf(false) }
                    val daysUntilDue = getDaysDifference(member.nextDueDate)
                    val isOverdue = daysUntilDue < 0 && member.isActive
                    val borderStroke = if (isOverdue) {
                        BorderStroke(2.5.dp, MaterialTheme.colorScheme.error)
                    } else {
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = borderStroke,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .testTag("member_card_${member.fullName.replace(" ", "_").lowercase()}"),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Primary Summary Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Colored initials avatar fallback (Clickable to launch Profile Popup)
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (member.block) {
                                                "Block A" -> MaterialTheme.colorScheme.primaryContainer
                                                "Block B" -> MaterialTheme.colorScheme.secondaryContainer
                                                else -> MaterialTheme.colorScheme.tertiaryContainer
                                            }
                                        )
                                        .clickable { selectedProfileForPopup = member },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        member.fullName.take(2).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Informative Name Columns
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            member.fullName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Badge(containerColor = if (member.isActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error) {
                                            Text(
                                                if (member.isActive) "ACTIVE" else "LEFT",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        if (isOverdue) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                                Text(
                                                    "OVERDUE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text(member.block, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Rent: ₹${member.rentAmount}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Quick Dialer launcher
                                IconButton(onClick = {
                                    try {
                                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${member.mobileNumber}")
                                        }
                                        context.startActivity(dialIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No dialer application found!", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Default.Call, "Call Resident", tint = Color(0xFF2E7D32))
                                }
                            }

                            // Dynamic Collapsible Extended View Details
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    
                                    // Detailed layout columns
                                    Text(
                                        "Detailed Administration Info",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    DetailContentRow("Gender / Age / DOB", "${member.gender} • ${member.age} yrs • DOB: ${member.dateOfBirth}")
                                    DetailContentRow("Aadhaar Card Uploaded", if (member.aadhaarPhotoUri != null) "Yes (Photo File Uploaded)" else "No")
                                    DetailContentRow("Driving Licence", member.drivingLicenceNumber ?: "Not Provided")
                                    DetailContentRow("Email Address", member.emailId.ifEmpty { "None" })
                                    DetailContentRow("Food Services", if (member.foodIncluded) "Yes (Fully Included)" else "No (Not Included)")
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Parent / Guardian details",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    DetailContentRow("Parent Contact Name", member.parentName)
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Contact Phone: ${member.parentPhone}", fontSize = 12.sp)
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                                        data = Uri.parse("tel:${member.parentPhone}")
                                                    }
                                                    context.startActivity(dialIntent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "No dialer application found!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.PhoneCallback, "Call Parent", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    DetailContentRow("Address Location", "${member.address}, ${member.city}, ${member.state}")
                                    DetailContentRow("Last Paid Rent Recipient", "${member.paymentDate} via ${member.paymentMethod}")
                                    DetailContentRow("Next Payment Due Date", member.nextDueDate)

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Interactive administrative actions keys
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { onEditMember(member) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("edit_member_btn_${member.id}"),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Edit Bio", fontSize = 12.sp)
                                        }

                                        IconButton(
                                            onClick = { selectedProfileForPopup = member },
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                                .size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OpenInNew,
                                                contentDescription = "Quick Profile Popup Preview",
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                memberToDelete = member

                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("delete_member_btn_${member.id}"),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Remove", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailContentRow(label: String, valContent: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valContent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// Legacy AlertDialogDelete helper removed; replaced by native, safe Material 3 Compose AlertDialog


// 4. PAYMENTS & CASH INFLOW JOURNAL
@Composable
fun PaymentsScreen(viewModel: DormViewModel) {
    val members by viewModel.allMembers.collectAsStateWithLifecycle()
    val payments by viewModel.allPayments.collectAsStateWithLifecycle()
    val currentMonthYear by viewModel.currentMonthYear.collectAsStateWithLifecycle()
    val monthOptions by viewModel.monthYearOptions.collectAsStateWithLifecycle()
    val totalMonthIncome by viewModel.selectedMonthIncome.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Record Dues State controls
    var selectedMemberId by remember { mutableStateOf<Int?>(null) }
    var inputAmount by remember { mutableStateOf("") }
    var inputDatePaid by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var inputDateNextDue by remember { mutableStateOf(viewModel.getDefaultNextDueDate()) }
    var inputMethod by remember { mutableStateOf("UPI") }
    var inputNotes by remember { mutableStateOf("") }

    var expandedMonthDropdown by remember { mutableStateOf(false) }
    var expandedMemberDropdown by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<Payment?>(null) }

    paymentToDelete?.let { pay ->
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = { Text("Delete Payment Record", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this payment record of ₹${String.format("%,.0f", pay.amount)} for ${pay.memberName}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val payName = pay.memberName
                        val payAmt = pay.amount
                        viewModel.deletePayment(pay)
                        Toast.makeText(context, "Payment record removed!", Toast.LENGTH_SHORT).show()
                        sendSystemNotification(context, "Payment Deleted", "Rent receipt of ₹${String.format("%,.2f", payAmt)} for $payName has been deleted.")
                        paymentToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Rent Collection Ledger input
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Rent Receipt Recorder",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Select Resident Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedMemberDropdown = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_resident_deposit"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        val activeMember = members.find { it.id == selectedMemberId }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeMember?.let { "${it.fullName} (${it.block})" } ?: "Select Resident *",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }

                    DropdownMenu(
                        expanded = expandedMemberDropdown,
                        onDismissRequest = { expandedMemberDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.fullName} (${m.block}) - Due: ${m.nextDueDate}") },
                                onClick = {
                                    selectedMemberId = m.id
                                    inputAmount = m.rentAmount.toString()
                                    expandedMemberDropdown = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Amount field
                    OutlinedTextField(
                        value = inputAmount,
                        onValueChange = { inputAmount = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Amount Received (₹) *") },
                        modifier = Modifier.weight(1.2f),
                        leadingIcon = { Icon(Icons.Default.CurrencyRupee, null) }
                    )

                    // Method Selector
                    Box(modifier = Modifier.weight(1f)) {
                        var expandedMethod by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { expandedMethod = true },
                            modifier = Modifier.padding(top = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(inputMethod, maxLines = 1)
                                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = expandedMethod,
                            onDismissRequest = { expandedMethod = false }
                        ) {
                            listOf("UPI", "Cash", "Bank").forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode) },
                                    onClick = {
                                        inputMethod = mode
                                        expandedMethod = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Payment date
                    OutlinedTextField(
                        value = inputDatePaid,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Payment Date") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker(context, inputDatePaid) { inputDatePaid = it } },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker(context, inputDatePaid) { inputDatePaid = it } }) {
                                Icon(Icons.Default.CalendarMonth, null)
                            }
                        }
                    )

                    // Next Dues Date
                    OutlinedTextField(
                        value = inputDateNextDue,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Next Due Date") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker(context, inputDateNextDue) { inputDateNextDue = it } },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker(context, inputDateNextDue) { inputDateNextDue = it } }) {
                                Icon(Icons.Default.CalendarToday, null)
                            }
                        }
                    )
                }

                OutlinedTextField(
                    value = inputNotes,
                    onValueChange = { inputNotes = it },
                    placeholder = { Text("Optional Notes (e.g., Rent for June)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val mId = selectedMemberId
                        if (mId == null) {
                            Toast.makeText(context, "Please select an active resident!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val amt = inputAmount.toDoubleOrNull()
                        if (amt == null || amt <= 0) {
                            Toast.makeText(context, "Please input a valid paid amount!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val member = members.find { it.id == mId }
                        val residentName = member?.fullName ?: "Resident"

                        viewModel.recordPayment(
                            memberId = mId,
                            amount = amt,
                            datePaid = inputDatePaid,
                            nextDueDate = inputDateNextDue,
                            method = inputMethod,
                            notes = inputNotes
                        )

                        Toast.makeText(context, "Payment Logged & Resident status updated!", Toast.LENGTH_SHORT).show()
                        sendSystemNotification(
                            context = context,
                            title = "💰 Payment Registered",
                            text = "Received ₹${String.format("%,.2f", amt)} from $residentName via $inputMethod."
                        )
                        
                        // Clear states
                        selectedMemberId = null
                        inputAmount = ""
                        inputNotes = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_deposit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Record")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record Rent Payment")
                }
            }
        }

        // Summary month income progress ledger section
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Select Month Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Revenue Inflows Log", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Total: ₹${String.format("%,.0f", totalMonthIncome)}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF2E7D32))
                    }

                    // Month Picker Selector dropdown
                    Box {
                        OutlinedButton(onClick = { expandedMonthDropdown = true }) {
                            Text(currentMonthYear)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, null)
                        }

                        DropdownMenu(
                            expanded = expandedMonthDropdown,
                            onDismissRequest = { expandedMonthDropdown = false }
                        ) {
                            monthOptions.forEach { mOpt ->
                                DropdownMenuItem(
                                    text = { Text(mOpt) },
                                    onClick = {
                                        viewModel.currentMonthYear.value = mOpt
                                        expandedMonthDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Payments Record Log
                val logs = payments.filter { it.paymentDate.startsWith(currentMonthYear) }
                if (logs.isEmpty()) {
                    Text(
                        "No deposits logged in $currentMonthYear.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                } else {
                    Box(modifier = Modifier.heightIn(max = 200.dp)) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(logs) { payLog ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(payLog.memberName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${payLog.memberBlock} • ${payLog.paymentDate} • via ${payLog.paymentMethod}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (payLog.notes.isNotEmpty()) {
                                            Text(payLog.notes, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "₹${String.format("%,.0f", payLog.amount)}",
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF2E7D32),
                                            fontSize = 14.sp
                                        )
                                        IconButton(
                                            onClick = { paymentToDelete = payLog },
                                            modifier = Modifier.size(36.dp).testTag("delete_payment_btn_${payLog.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Payment",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// 5. EXPORT FOR ADVANCED SPREADSHEETS (EXCEL EXPORT)
@Composable
fun ExportScreen(viewModel: DormViewModel) {
    val context = LocalContext.current
    val totalCount by viewModel.allMembers.collectAsStateWithLifecycle()
    val totalPayments by viewModel.allPayments.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large styled illustrative icon
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E7D32).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Excel Sheet icon",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(45.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "Excel-Compatible Spreadsheet Export",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Compile and share offline CSV records. These files instantly open in Microsoft Excel, Google Sheets, or LibreOffice.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Active Residents", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${totalCount.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Financial Receipts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${totalPayments.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF2E7D32))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val fileUri = viewModel.exportToExcel(context)
                        if (fileUri != null) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "Lilly Cottage(PG) Administration Report")
                                putExtra(Intent.EXTRA_STREAM, fileUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(Intent.createChooser(shareIntent, "Share Lilly Cottage(PG) Report via:"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "No sharing application found!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Spreadsheet generation failed!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("export_csv_action_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Spreadsheet (CSV)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Context activity finder helper to prevent dialog BadTokenExceptions
fun Context.findActivity(): android.app.Activity? {
    var currentContext = this
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is android.app.Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

private var isDatePickerActive = false

// Utility system DatePickerDialog launcher
fun showDatePicker(context: Context, initialDateStr: String, onDateSelected: (String) -> Unit) {
    if (isDatePickerActive) return
    isDatePickerActive = true
    val activityContext = context.findActivity()
    if (activityContext == null || activityContext.isFinishing) {
        isDatePickerActive = false
        onDateSelected(initialDateStr.ifEmpty { "2026-05-26" })
        return
    }
    val cal = Calendar.getInstance()
    try {
        val parts = initialDateStr.split("-")
        if (parts.size == 3) {
            cal.set(Calendar.YEAR, parts[0].toInt())
            cal.set(Calendar.MONTH, parts[1].toInt() - 1)
            cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
        }
    } catch (e: Exception) {}

    try {
        val picker = android.app.DatePickerDialog(
            activityContext,
            { _, year, month, dayOfMonth ->
                isDatePickerActive = false
                val monthStr = String.format("%02d", month + 1)
                val dayStr = String.format("%02d", dayOfMonth)
                onDateSelected("$year-$monthStr-$dayStr")
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        picker.setOnDismissListener {
            isDatePickerActive = false
        }
        picker.show()
    } catch (e: Exception) {
        isDatePickerActive = false
        e.printStackTrace()
    }
}

// Days tracker difference helper
fun getDaysDifference(dateStr: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val targetDate = sdf.parse(dateStr)
        val today = sdf.parse(sdf.format(Date()))
        if (targetDate != null && today != null) {
            val diff = targetDate.time - today.time
            diff / (1000 * 60 * 60 * 24)
        } else {
            999L
        }
    } catch (e: Exception) {
        999L
    }
}

// Safely copy URI content to local files internal storage to guarantee permanent access & avoid SecurityExceptions
fun saveUriToInternalStorage(context: Context, uri: Uri): Uri? {
    return try {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val fileName = "aadhaar_${System.currentTimeMillis()}.png"
        val file = java.io.File(context.filesDir, fileName)
        val outputStream = java.io.FileOutputStream(file)
        
        val buffer = ByteArray(4 * 1024)
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            outputStream.write(buffer, 0, read)
        }
        
        outputStream.flush()
        outputStream.close()
        inputStream.close()
        
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// 6. DETAILED PROFILE POPUP DIALOG
@Composable
fun MemberProfileDialog(member: Member, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", fontWeight = FontWeight.Black)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("RESIDENT BIO DATA", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 0.5.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile Initials Indicator instead of profile photoUri
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            member.fullName.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    member.fullName.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Badge(containerColor = if (member.isActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error) {
                        Text(
                            text = if (member.isActive) "ACTIVE RESIDENT" else "LEFT COUNTERPART",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                        Text(
                            text = member.block.uppercase(),
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Profile Fields Grid
                Text(
                    text = "Aadhaar Card Photo",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (member.aadhaarPhotoUri != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AsyncImage(
                            model = member.aadhaarPhotoUri,
                            contentDescription = "Aadhaar Card Document Photo View",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Text(
                        text = "No Aadhaar image found",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                ProfileFieldRow(label = "Driving Licence (Optional)", value = member.drivingLicenceNumber ?: "Not Provided", icon = Icons.Default.Badge)
                ProfileFieldRow(label = "Contact Mobile", value = member.mobileNumber, icon = Icons.Default.Phone)
                ProfileFieldRow(label = "Emergency Contact Guardian", value = "${member.parentName} (${member.parentPhone})", icon = Icons.Default.SupervisorAccount)
                ProfileFieldRow(label = "Age & Sex", value = "${member.age} Yrs old • ${member.gender}", icon = Icons.Default.Face)
                ProfileFieldRow(label = "DOB", value = member.dateOfBirth, icon = Icons.Default.CalendarToday)
                ProfileFieldRow(label = "Permanent Address Address", value = "${member.address}, ${member.city}, ${member.state}", icon = Icons.Default.Home)
                ProfileFieldRow(label = "Monthly Rent Price Amount", value = "₹${String.format("%,.2f", member.rentAmount)}", icon = Icons.Default.CurrencyRupee)
                ProfileFieldRow(label = "Last Paid Rent Info", value = "${member.paymentDate} via ${member.paymentMethod}", icon = Icons.Default.Payment)
                ProfileFieldRow(label = "Next Payment Due Date", value = member.nextDueDate, icon = Icons.Default.CalendarToday)
                ProfileFieldRow(label = "Join registration Date", value = member.joinDate.ifEmpty { member.paymentDate }, icon = Icons.Default.Today)
                ProfileFieldRow(label = "Meals Food Counterpart Service?", value = if (member.foodIncluded) "Yes (3 Daily Meals Provided)" else "No", icon = Icons.Default.Restaurant)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ProfileFieldRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

fun sendSystemNotification(context: Context, title: String, text: String) {
    if (!hasNotificationPermission(context)) {
        return
    }

    val channelId = "dorm_admin_notifications"
    val channelName = "Dormitory Alerts"
    val notificationId = System.currentTimeMillis().toInt()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = "Channel for dormitory alerts, updates, and payments"
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(text)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    try {
        val manager = NotificationManagerCompat.from(context)
        manager.notify(notificationId, builder.build())
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}

