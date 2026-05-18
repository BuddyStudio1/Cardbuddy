package com.cardbuddy.app

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.core.content.FileProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import com.cardbuddy.app.data.AppDatabase
import com.cardbuddy.app.data.CardEntity
import com.cardbuddy.app.ui.theme.CardBuddyTheme
import com.cardbuddy.app.util.BarcodeGenerator
import com.cardbuddy.app.util.BarcodeScanner
import com.cardbuddy.app.util.BrandUtils
import com.cardbuddy.app.util.ShortcutManagerHelper
import com.cardbuddy.app.util.WearSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

fun getCategoryName(category: String, context: Context): String {
    return when (category) {
        "All" -> context.getString(R.string.cat_all)
        "Supermarket" -> context.getString(R.string.cat_supermarket)
        "Home & DIY" -> context.getString(R.string.cat_home_diy)
        "Health & Beauty" -> context.getString(R.string.cat_health_beauty)
        "Fashion" -> context.getString(R.string.cat_fashion)
        "Electronics" -> context.getString(R.string.cat_electronics)
        "Fuel" -> context.getString(R.string.cat_fuel)
        "Liquor" -> context.getString(R.string.cat_liquor)
        else -> context.getString(R.string.cat_other)
    }
}

@Serializable
data class CardExport(
    val storeName: String,
    val barcodeNumber: String,
    val usageCount: Int,
    val createdAt: Long,
    val logoUrl: String?,
    val barcodeFormat: Int,
    val notes: String,
    val hexColor: String?,
    val category: String
)

class MainActivity : ComponentActivity() {
    private val _shortcutCardId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            CardBuddyTheme {
                CardBuddyApp(_shortcutCardId.value) { _shortcutCardId.value = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val cardId = intent.getLongExtra("card_id", -1L)
            if (cardId != -1L) {
                _shortcutCardId.value = cardId
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardBuddyApp(shortcutCardId: Long? = null, onShortcutHandled: () -> Unit = {}) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = db.cardDao()
    val scope = rememberCoroutineScope()

    val cards by dao.getAllCardsSortedByUsage().collectAsState(initial = emptyList())
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    
    val filteredCards = remember(cards, selectedCategory, searchQuery) {
        cards.filter { card ->
            val matchesCategory = if (selectedCategory == "All") true else card.category == selectedCategory
            val matchesSearch = card.storeName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }
    
    LaunchedEffect(cards) {
        ShortcutManagerHelper.updateShortcuts(context, cards)
        
        // Update missing brand data for existing cards
        val cardsToUpdate = cards.filter { 
            it.logoUrl == null || it.hexColor == null || it.hexColor == "#1A1C1E" || it.category == "Other"
        }
        if (cardsToUpdate.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                cardsToUpdate.forEach { card ->
                    val newLogo = BrandUtils.getLogoUrl(card.storeName)
                    val newColor = BrandUtils.getBrandColor(card.storeName)
                    val newCategory = BrandUtils.getBrandCategory(card.storeName)
                    
                    val updatedCard = card.copy(
                        logoUrl = card.logoUrl ?: newLogo,
                        hexColor = if (card.hexColor == null || card.hexColor == "#1A1C1E") newColor else card.hexColor,
                        category = if (card.category == "Other") newCategory else card.category
                    )
                    
                    if (updatedCard != card) {
                        dao.updateCard(updatedCard)
                    }
                }
            }
        }
    }
    
    val categories = remember(cards) {
        listOf("All") + cards.map { it.category }.distinct().filter { it != "Other" } + listOf("Other")
    }.distinct()

    LaunchedEffect(cards) {
        Log.d("CardBuddy", "Cards updated: ${cards.size} items. Content: $cards")
    }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var selectedCardId by rememberSaveable { mutableStateOf<Long?>(null) }
    
    val selectedCard = remember(selectedCardId, cards) {
        cards.find { it.id == selectedCardId }
    }

    // Handle shortcut intent
    LaunchedEffect(shortcutCardId) {
        if (shortcutCardId != null && shortcutCardId != -1L) {
            withContext(Dispatchers.IO) { dao.incrementUsageCount(shortcutCardId) }
            selectedCardId = shortcutCardId
            onShortcutHandled()
        }
    }

    var storeNameInput by rememberSaveable { mutableStateOf("") }
    var barcodeNumberInput by rememberSaveable { mutableStateOf("") }
    var selectedFormat by rememberSaveable { mutableIntStateOf(-1) }
    var selectedColor by rememberSaveable { mutableStateOf("#1A1C1E") }
    var selectedCategoryInput by rememberSaveable { mutableStateOf("Other") }
    var selectedImagePath by rememberSaveable { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val cardsToExport = dao.getAllCardsDirect().map {
                        CardExport(
                            it.storeName, it.barcodeNumber, it.usageCount,
                            it.createdAt, it.logoUrl, it.barcodeFormat, it.notes, it.hexColor, it.category
                        )
                    }
                    val json = Json.encodeToString(cardsToExport)
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.msg_export_success), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("CardBuddy", "Export failed", e)
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (json != null) {
                        val importedCards = Json.decodeFromString<List<CardExport>>(json)
                        importedCards.forEach {
                            dao.insertCard(CardEntity(
                                storeName = it.storeName,
                                barcodeNumber = it.barcodeNumber,
                                usageCount = it.usageCount,
                                createdAt = it.createdAt,
                                logoUrl = it.logoUrl,
                                barcodeFormat = it.barcodeFormat,
                                notes = it.notes,
                                hexColor = it.hexColor,
                                category = it.category
                            ))
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(R.string.msg_import_success), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CardBuddy", "Import failed", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.error_import_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun resetInputs() {
        storeNameInput = ""
        barcodeNumberInput = ""
        selectedFormat = -1
        selectedColor = "#1A1C1E"
        selectedCategoryInput = "Other"
        selectedImagePath = null
        showAddDialog = false
        Toast.makeText(context, context.getString(R.string.msg_card_added), Toast.LENGTH_SHORT).show()
    }

    fun saveCard(name: String, number: String, format: Int, color: String, category: String, imagePath: String?) {
        scope.launch {
            val logoUrl = BrandUtils.getLogoUrl(name)
            val newCard = CardEntity(
                storeName = name,
                barcodeNumber = number,
                logoUrl = logoUrl,
                hexColor = color,
                barcodeFormat = format,
                imagePath = imagePath,
                category = category
            )
            dao.insertCard(newCard)
            resetInputs()
        }
    }

    var currentPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    
    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoPath?.let { path ->
                val file = File(path)
                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    scope.launch {
                        val (scannedNumber, scannedFormat) = BarcodeScanner.scanBarcode(bitmap)
                        selectedImagePath = path
                        if (scannedNumber != null) {
                            barcodeNumberInput = scannedNumber
                            selectedFormat = scannedFormat
                            Toast.makeText(context, context.getString(R.string.msg_barcode_detected), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.error_no_barcode_found), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (storeNameInput.isNotBlank()) {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val file = File(context.filesDir, "CARD_$timestamp.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                currentPhotoPath = file.absolutePath
                takePhotoLauncher.launch(uri)
            }
        } else {
            Toast.makeText(context, context.getString(R.string.error_camera_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                // Grant persistent permission to this URI
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    Log.e("CardBuddy", "Could not take persistable permission", e)
                }

                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val imagePath = saveBitmapToLocal(bitmap, context)
                    scope.launch {
                        val (scannedNumber, scannedFormat) = BarcodeScanner.scanBarcode(bitmap)
                        selectedImagePath = imagePath
                        if (scannedNumber != null) {
                            barcodeNumberInput = scannedNumber
                            selectedFormat = scannedFormat
                            // If dialog isn't open, open it!
                            if (!showAddDialog) {
                                storeNameInput = ""
                                showAddDialog = true
                            }
                            Toast.makeText(context, context.getString(R.string.msg_barcode_detected), Toast.LENGTH_SHORT).show()
                        } else {
                            if (!showAddDialog) {
                                storeNameInput = ""
                                showAddDialog = true
                            }
                            Toast.makeText(context, context.getString(R.string.error_no_barcode_found), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CardBuddy", "Error picking image", e)
                Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (selectedCard != null) {
        CardDetailScreen(
            card = selectedCard,
            onBack = { selectedCardId = null },
            onDelete = {
                scope.launch {
                    selectedCard.imagePath?.let { path ->
                        try {
                            File(path).delete()
                        } catch (e: Exception) {
                            Log.e("CardBuddy", "Error deleting image file", e)
                        }
                    }
                    dao.deleteCard(selectedCard)
                    selectedCardId = null
                    Toast.makeText(context, context.getString(R.string.msg_card_deleted), Toast.LENGTH_SHORT).show()
                }
            },
            onUpdate = { updatedCard ->
                scope.launch {
                    dao.updateCard(updatedCard)
                }
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isSearching) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(stringResource(R.string.search_hint)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                ),
                                singleLine = true
                            )
                        } else {
                            Text(stringResource(R.string.app_name))
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            isSearching = !isSearching
                            if (!isSearching) searchQuery = ""
                        }) {
                            Icon(
                                imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (isSearching) "Close search" else "Search"
                            )
                        }
                        if (!isSearching && cards.isNotEmpty()) {
                            IconButton(onClick = { 
                                storeNameInput = ""
                                barcodeNumberInput = ""
                                selectedFormat = -1
                                selectedColor = "#1A1C1E"
                                showAddDialog = true 
                            }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_card))
                            }
                        }
                        if (!isSearching) {
                            IconButton(onClick = { createDocumentLauncher.launch("cardbuddy_backup.json") }) {
                                Icon(Icons.Default.Backup, contentDescription = stringResource(R.string.export_btn))
                            }
                            IconButton(onClick = { openDocumentLauncher.launch(arrayOf("application/json", "application/octet-stream")) }) {
                                Icon(Icons.Default.UploadFile, contentDescription = stringResource(R.string.import_btn))
                            }
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, "https://ko-fi.com/buddystudio".toUri())
                            context.startActivity(intent)
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalCafe,
                            contentDescription = null,
                            tint = Color(0xFF29ABE0), // Ko-fi blue
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.like_app_promo),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        ) { padding ->
            val view = LocalView.current
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (cards.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                        edgePadding = 16.dp,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = {}
                    ) {
                        categories.forEach { cat ->
                            Tab(
                                selected = selectedCategory == cat,
                                onClick = { 
                                    selectedCategory = cat
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                },
                                text = {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selectedCategory == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (selectedCategory == cat) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        Text(getCategoryName(cat, context), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                    }
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (filteredCards.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.mipmap.cardbuddy_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.size(180.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            val buddyTips = listOf(
                                stringResource(R.string.buddy_tip_1),
                                stringResource(R.string.buddy_tip_2),
                                stringResource(R.string.buddy_tip_3),
                                stringResource(R.string.buddy_tip_4),
                                stringResource(R.string.buddy_tip_5)
                            )
                            val randomTip = remember { buddyTips.random() }

                            Text(
                                if (cards.isEmpty()) stringResource(R.string.welcome_buddy) else stringResource(R.string.no_results_msg),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (cards.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    randomTip,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            AddCardItem(
                                modifier = Modifier.width(220.dp),
                                onClick = { 
                                    storeNameInput = ""
                                    barcodeNumberInput = ""
                                    selectedFormat = -1
                                    selectedColor = "#1A1C1E"
                                    showAddDialog = true 
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    pickImageLauncher.launch(arrayOf("image/*"))
                                },
                                modifier = Modifier.width(220.dp)
                            ) {
                                Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.import_screenshot_btn))
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            gridItems(filteredCards) { card ->
                                CardItem(
                                    card = card,
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        scope.launch { dao.incrementUsageCount(card.id) }
                                        selectedCardId = card.id
                                    }
                                )
                            }
                            item {
                                AddCardItem(
                                    onClick = {
                                        storeNameInput = ""
                                        barcodeNumberInput = ""
                                        selectedFormat = -1
                                        selectedColor = "#1A1C1E"
                                        selectedImagePath = null
                                        showAddDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.new_card_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = storeNameInput,
                        onValueChange = { 
                            storeNameInput = it
                            val detectedColor = BrandUtils.getBrandColor(it)
                            if (detectedColor != "#1A1C1E") {
                                selectedColor = detectedColor
                            }
                            selectedCategoryInput = BrandUtils.getBrandCategory(it)
                        },
                        label = { Text(stringResource(R.string.store_name_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = barcodeNumberInput,
                        onValueChange = { barcodeNumberInput = it },
                        label = { Text(stringResource(R.string.barcode_number_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = BarcodeGenerator.getFormatName(context, selectedFormat),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.barcode_format_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            BarcodeGenerator.getSupportedFormats(context).forEach { (format, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedFormat = format
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = getCategoryName(selectedCategoryInput, context),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.category_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            BrandUtils.categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(getCategoryName(cat, context)) },
                                    onClick = {
                                        selectedCategoryInput = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.color_label), style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    val colors = listOf(
                        "#00A0E2", "#0050AA", "#0051BA", "#003057", "#000000",
                        "#DF0000", "#2E2E2E", "#87AFC7", "#0000FF", "#1A1C1E",
                        "#4CAF50", "#FF9800", "#9C27B0", "#E91E63", "#795548"
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        modifier = Modifier.height(110.dp)
                    ) {
                        gridItems(colors) { colorHex ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorHex.toColorInt()))
                                    .clickable { selectedColor = colorHex }
                                    .then(
                                        if (selectedColor == colorHex) {
                                            Modifier.background(Color.White.copy(alpha = 0.5f))
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (selectedImagePath != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp))) {
                            Image(
                                bitmap = android.graphics.BitmapFactory.decodeFile(selectedImagePath).asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImagePath = null },
                                modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                if (storeNameInput.isNotBlank()) {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                } else {
                                    Toast.makeText(context, context.getString(R.string.error_store_name_required), Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.take_photo_btn), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                pickImageLauncher.launch(arrayOf("image/*"))
                            },
                            modifier = Modifier.weight(1.1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.screenshot_gallery_btn), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (storeNameInput.isNotBlank()) {
                                saveCard(storeNameInput.trim(), barcodeNumberInput.trim(), selectedFormat, selectedColor, selectedCategoryInput, selectedImagePath)
                            } else {
                                Toast.makeText(context, context.getString(R.string.error_store_name_required), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.save_btn))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.add_card_instructions),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false }
                ) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    card: CardEntity,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (CardEntity) -> Unit
) {
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showNotesDialog by rememberSaveable { mutableStateOf(false) }
    var showPhotoDialog by rememberSaveable { mutableStateOf(false) }
    var editNumber by rememberSaveable(card.id) { mutableStateOf(card.barcodeNumber) }
    var editFormat by rememberSaveable(card.id) { mutableIntStateOf(card.barcodeFormat) }
    var editNotes by rememberSaveable(card.id) { mutableStateOf(card.notes) }
    var editColor by rememberSaveable(card.id) { mutableStateOf(card.hexColor ?: "#1A1C1E") }
    var editCategory by rememberSaveable(card.id) { mutableStateOf(card.category) }
    var expanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    
    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoPath?.let { path ->
                val file = File(path)
                scope.launch {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        val (scannedNumber, scannedFormat) = BarcodeScanner.scanBarcode(bitmap)
                        if (scannedNumber != null) {
                            editNumber = scannedNumber
                            editFormat = scannedFormat
                            onUpdate(card.copy(
                                imagePath = path,
                                barcodeNumber = scannedNumber,
                                barcodeFormat = scannedFormat
                            ))
                            Toast.makeText(context, context.getString(R.string.msg_barcode_detected), Toast.LENGTH_SHORT).show()
                        } else {
                            onUpdate(card.copy(imagePath = path))
                        }
                    }
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(context.filesDir, "CARD_$timestamp.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            currentPhotoPath = file.absolutePath
            takePhotoLauncher.launch(uri)
        } else {
            Toast.makeText(context, context.getString(R.string.error_camera_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {}

                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val imagePath = saveBitmapToLocal(bitmap, context)
                    // Delete old photo if it exists
                    card.imagePath?.let { oldPath ->
                        try { File(oldPath).delete() } catch (e: Exception) {}
                    }
                    scope.launch {
                        val (scannedNumber, scannedFormat) = BarcodeScanner.scanBarcode(bitmap)
                        if (scannedNumber != null) {
                            onUpdate(card.copy(
                                imagePath = imagePath,
                                barcodeNumber = scannedNumber,
                                barcodeFormat = scannedFormat
                            ))
                            Toast.makeText(context, context.getString(R.string.msg_barcode_detected), Toast.LENGTH_SHORT).show()
                        } else {
                            onUpdate(card.copy(imagePath = imagePath))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CardBuddy", "Error picking image", e)
            }
        }
    }

    val headerColor = remember(card.hexColor) {
        if (card.hexColor != null) {
            try {
                Color(card.hexColor.toColorInt())
            } catch (e: Exception) {
                Color(0xFF03A9F4)
            }
        } else {
            Color(0xFF03A9F4)
        }
    }

    val headerContentColor = remember(headerColor) {
        if (headerColor.luminance() > 0.5f) Color.Black else Color.White
    }

    Scaffold(
        containerColor = Color(0xFF0F0E17) // Dark background like screenshot
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Main Card Visualization
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column {
                    // Header part of the card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(headerColor)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StoreLogo(
                                storeName = card.storeName,
                                logoUrl = card.logoUrl,
                                modifier = Modifier.size(44.dp),
                                contentDescription = null // Already described by the Title Text
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val titleFontSize = when {
                                    card.storeName.length > 20 -> 16.sp
                                    card.storeName.length > 12 -> 18.sp
                                    else -> 22.sp
                                }
                                Text(
                                    card.storeName,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = titleFontSize),
                                    color = headerContentColor,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = titleFontSize * 1.1f
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                onClick = { onBack() },
                                color = headerContentColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text(
                                    stringResource(R.string.close_btn),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = headerContentColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Barcode part of the card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (card.barcodeNumber.isNotEmpty()) {
                            val barcodeBitmap = remember(card.barcodeNumber, card.barcodeFormat) {
                                BarcodeGenerator.generateBarcode(card.barcodeNumber, card.barcodeFormat)
                            }

                            if (barcodeBitmap != null) {
                                Image(
                                    bitmap = barcodeBitmap.asImageBitmap(),
                                    contentDescription = stringResource(R.string.barcode_desc, card.storeName),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .background(Color.White),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(
                                    stringResource(R.string.error_barcode_generation),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            Button(onClick = { showEditDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.add_barcode_btn))
                            }
                        }
                    }
                }
            }

            if (card.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    stringResource(R.string.notes_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    card.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                stringResource(R.string.manage_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Management Options
            ManagementOption(
                icon = Icons.Default.Edit,
                title = stringResource(R.string.edit_card),
                onClick = {
                    editNumber = card.barcodeNumber
                    editFormat = card.barcodeFormat
                    editColor = card.hexColor ?: "#1A1C1E"
                    showEditDialog = true
                }
            )
            ManagementOption(
                icon = Icons.AutoMirrored.Filled.Notes,
                title = stringResource(R.string.notes_title),
                onClick = {
                    editNotes = card.notes
                    showNotesDialog = true
                }
            )
            ManagementOption(
                icon = Icons.Default.PhotoCamera,
                title = stringResource(R.string.photos_title),
                onClick = {
                    showPhotoDialog = true
                }
            )
            ManagementOption(
                icon = Icons.Default.PushPin,
                title = stringResource(R.string.pin_shortcut_btn),
                onClick = {
                    ShortcutManagerHelper.pinShortcut(context, card)
                }
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
            ManagementOption(
                icon = Icons.Default.Delete,
                title = stringResource(R.string.delete_btn),
                titleColor = Color(0xFFFF5252),
                onClick = onDelete
            )
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.edit_card_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = editNumber,
                        onValueChange = { editNumber = it },
                        label = { Text(stringResource(R.string.barcode_number_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = BarcodeGenerator.getFormatName(context, editFormat),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.barcode_format_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            BarcodeGenerator.getSupportedFormats(context).forEach { (format, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        editFormat = format
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = getCategoryName(editCategory, context),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.category_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            BrandUtils.categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(getCategoryName(cat, context)) },
                                    onClick = {
                                        editCategory = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.color_label), style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val colors = listOf(
                        "#00A0E2", "#0050AA", "#0051BA", "#003057", "#000000",
                        "#DF0000", "#2E2E2E", "#87AFC7", "#0000FF", "#1A1C1E",
                        "#4CAF50", "#FF9800", "#9C27B0", "#E91E63", "#795548"
                    )
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        modifier = Modifier.height(100.dp)
                    ) {
                        gridItems(colors) { colorHex ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorHex.toColorInt()))
                                    .clickable { editColor = colorHex }
                                    .then(
                                        if (editColor == colorHex) {
                                            Modifier.background(Color.White.copy(alpha = 0.5f))
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdate(card.copy(
                        barcodeNumber = editNumber.trim(),
                        barcodeFormat = if (editFormat == -1) 0 else editFormat, // Default to Code 128 if unknown
                        hexColor = editColor,
                        category = editCategory
                    ))
                    showEditDialog = false
                }) {
                    Text(stringResource(R.string.save_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }
    
    if (showNotesDialog) {
        AlertDialog(
            onDismissRequest = { showNotesDialog = false },
            title = { Text(stringResource(R.string.notes_title)) },
            text = {
                OutlinedTextField(
                    value = editNotes,
                    onValueChange = { editNotes = it },
                    label = { Text(stringResource(R.string.notes_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdate(card.copy(notes = editNotes))
                    showNotesDialog = false
                }) {
                    Text(stringResource(R.string.save_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotesDialog = false }) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }

    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text(stringResource(R.string.photos_title)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (card.imagePath != null) {
                        val file = File(card.imagePath)
                        if (file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = stringResource(R.string.card_photo_desc),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(stringResource(R.string.file_not_found))
                        }
                    } else {
                        Text(stringResource(R.string.no_photo_available))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { pickImageLauncher.launch(arrayOf("image/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.photos_title))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (card.imagePath == null) stringResource(R.string.take_photo_action) else stringResource(R.string.take_new_photo_action))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoDialog = false }) {
                    Text(stringResource(R.string.close_btn))
                }
            }
        )
    }
}

@Composable
fun ManagementOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    titleColor: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = titleColor.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor
            )
        }
    }
}

@Composable
fun StoreLogo(
    storeName: String, 
    logoUrl: String?, 
    modifier: Modifier = Modifier,
    contentDescription: String? = "$storeName Logo"
) {
    var currentLogoUrl by remember(storeName, logoUrl) {
        mutableStateOf(if (!logoUrl.isNullOrBlank()) logoUrl else BrandUtils.getLogoUrl(storeName))
    }
    var hasAttemptedFallback by remember(storeName, logoUrl) { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (currentLogoUrl.isNullOrBlank()) {
            LogoFallback(
                storeName = storeName,
                textColor = Color(0xFF1A1C1E),
                containerColor = Color.Transparent
            )
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currentLogoUrl)
                    .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            ) {
                val state = painter.state
                when (state) {
                    is AsyncImagePainter.State.Loading -> {
                        LogoFallback(
                            storeName = storeName,
                            textColor = Color(0xFF1A1C1E),
                            containerColor = Color.Transparent
                        )
                    }
                    is AsyncImagePainter.State.Error -> {
                        val error = state.result.throwable
                        Log.e("StoreLogo", "Error loading logo for $storeName from $currentLogoUrl: ${error.message}")
                        
                        if (!hasAttemptedFallback) {
                            val fallbackUrl = BrandUtils.getFallbackLogoUrl(storeName)
                            if (fallbackUrl != null && fallbackUrl != currentLogoUrl) {
                                SideEffect {
                                    hasAttemptedFallback = true
                                    currentLogoUrl = fallbackUrl
                                }
                            }
                        }

                        LogoFallback(
                            storeName = storeName,
                            textColor = Color(0xFF1A1C1E),
                            containerColor = Color.Transparent
                        )
                    }
                    is AsyncImagePainter.State.Success -> {
                        SubcomposeAsyncImageContent()
                    }
                    else -> {
                        SubcomposeAsyncImageContent()
                    }
                }
            }
        }
    }
}

@Composable
fun LogoFallback(
    storeName: String,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    val firstLetter = storeName.trim().firstOrNull()?.toString()?.uppercase() ?: "?"

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = containerColor
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = firstLetter,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    color = textColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun AddCardItem(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.add_card),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CardItem(card: CardEntity, onClick: () -> Unit) {
    val backgroundColor = remember(card.hexColor) {
        if (card.hexColor != null) {
            try {
                Color(card.hexColor.toColorInt())
            } catch (e: Exception) {
                Color(0xFF1A1C1E)
            }
        } else {
            Color(0xFF1A1C1E)
        }
    }

    val contentColor = if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.586f) // Standard store card aspect ratio
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StoreLogo(
                storeName = card.storeName,
                logoUrl = card.logoUrl,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentDescription = null // Already described by the store name text below
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = card.storeName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun saveBitmapToLocal(bitmap: Bitmap, context: Context): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "CARD_$timestamp.jpg"
    val file = File(context.filesDir, fileName)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
    }
    return file.absolutePath
}


@Preview(showBackground = true, backgroundColor = 0xFF0F0E17)
@Composable
fun CardItemPreview() {
    CardBuddyTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            CardItem(
                card = CardEntity(
                    storeName = "Albert Heijn",
                    barcodeNumber = "123456789",
                    hexColor = "#00A0E2"
                ),
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E17)
@Composable
fun LongNameCardPreview() {
    CardBuddyTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CardItem(
                card = CardEntity(
                    storeName = "Königliche Porzellan-Manufaktur Berlin",
                    barcodeNumber = "123456789",
                    hexColor = "#2E2E2E"
                ),
                onClick = {}
            )
            CardItem(
                card = CardEntity(
                    storeName = "Association des Centres Distributeurs E.Leclerc",
                    barcodeNumber = "123456789",
                    hexColor = "#0050AA"
                ),
                onClick = {}
            )
        }
    }
}
