package com.travelnotes.offline

import android.content.ContentValues
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.travelnotes.offline.data.ImageLayoutMode
import com.travelnotes.offline.data.Note
import com.travelnotes.offline.ui.DraftImage
import com.travelnotes.offline.ui.MainViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TravelNotesApp()
        }
    }
}

private enum class PdfPaperMode {
    AUTO,
    SMALL,
    MEDIUM,
    LARGE
}

private enum class PdfCardSizeMode {
    SMALL,
    MEDIUM,
    LARGE
}

private enum class PdfExportQualityMode {
    STANDARD,
    ULTRA
}

private enum class PdfStyleMode {
    FOLLOW_APP,
    BLACK_THEME,
    WHITE_THEME
}

private enum class AppThemeMode(val storageValue: String) {
    FOLLOW_SYSTEM("follow_system"),
    BLACK("black"),
    WHITE("white");

    companion object {
        fun fromStorage(value: String?): AppThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: FOLLOW_SYSTEM
        }
    }
}

private data class PdfCardMetrics(
    val pageWidth: Int,
    val pageHeight: Int,
    val cardInsetX: Float,
    val cardInsetTop: Float,
    val cardInsetBottom: Float,
    val cardPaddingX: Float,
    val cardPaddingTop: Float,
    val footerAreaHeight: Float,
    val scale: Float,
    val imageGap: Float,
    val imageCornerRadius: Float
)

private data class PdfImageItem(
    val path: String,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

private data class PdfImageRow(
    val height: Float,
    val items: List<PdfImageItem>
)

private data class MosaicImageEntry(
    val absoluteIndex: Int,
    val path: String
)

private const val PREFS_APP_SETTINGS = "app_settings"
private const val KEY_THEME_MODE = "theme_mode"

@Composable
private fun TravelNotesApp() {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_APP_SETTINGS, Context.MODE_PRIVATE)
    }
    var themeMode by remember {
        mutableStateOf(
            AppThemeMode.fromStorage(
                prefs.getString(KEY_THEME_MODE, AppThemeMode.FOLLOW_SYSTEM.storageValue)
            )
        )
    }

    XStyleTheme(themeMode = themeMode) {
        TravelNotesScreen(
            themeMode = themeMode,
            onThemeModeChange = { mode ->
                themeMode = mode
                saveThemeMode(prefs, mode)
            }
        )
    }
}

@Composable
private fun TravelNotesScreen(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val isDarkMode = resolveIsDarkTheme(themeMode)
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var capturedPreviewFile by remember { mutableStateOf<File?>(null) }
    var capturedPreviewUri by remember { mutableStateOf<Uri?>(null) }
    var isConfirmingCapturedPhoto by remember { mutableStateOf(false) }
    var noteForPdfShare by remember { mutableStateOf<Note?>(null) }
    var isSharingPdf by remember { mutableStateOf(false) }
    var imageViewerPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var imageViewerInitialIndex by remember { mutableStateOf(0) }
    var isSettingsVisible by remember { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MainViewModel.MAX_IMAGE_PICKER_SELECTION)
    ) { uris: List<Uri> ->
        viewModel.addPickedImages(uris)
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        val file = pendingCameraFile
        pendingCameraUri = null
        pendingCameraFile = null
        if (uri == null || file == null) return@rememberLauncherForActivityResult

        if (success && file.exists() && file.length() > 0L) {
            capturedPreviewFile = file
            capturedPreviewUri = uri
        } else {
            deleteLocalFile(file)
        }
    }

    var openedNoteId by remember { mutableStateOf<Long?>(null) }
    val openedNote = viewModel.notes.firstOrNull { it.id == openedNoteId }

    val hasOverlayToClose = imageViewerPaths.isNotEmpty() ||
        isSettingsVisible ||
        capturedPreviewUri != null ||
        noteForPdfShare != null ||
        viewModel.isComposerVisible ||
        openedNoteId != null
    BackHandler(enabled = hasOverlayToClose) {
        when {
            imageViewerPaths.isNotEmpty() -> {
                imageViewerPaths = emptyList()
                imageViewerInitialIndex = 0
            }
            isSettingsVisible -> {
                isSettingsVisible = false
            }
            capturedPreviewUri != null -> {
                capturedPreviewFile?.let(::deleteLocalFile)
                capturedPreviewFile = null
                capturedPreviewUri = null
            }
            noteForPdfShare != null -> {
                if (!isSharingPdf) {
                    noteForPdfShare = null
                }
            }
            viewModel.isComposerVisible -> viewModel.cancelEdit()
            openedNoteId != null -> openedNoteId = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDarkMode) {
                        listOf(Color(0xFF020304), Color(0xFF070A0E), Color(0xFF0D1219))
                    } else {
                        listOf(Color(0xFFE6ECF5), Color(0xFFDEE6F1), Color(0xFFD4DEEC))
                    }
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButtonPosition = FabPosition.End,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        openedNoteId = null
                        viewModel.startCreate()
                    },
                    containerColor = if (isDarkMode) Color(0xFFEDF2F8) else Color(0xFF121316),
                    contentColor = if (isDarkMode) Color.Black else Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_add_note)
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    HeaderSection(
                        isDarkMode = isDarkMode,
                        onOpenSettings = { isSettingsVisible = true }
                    )
                }

                item {
                    OutlinedTextField(
                        value = viewModel.searchInput,
                        onValueChange = viewModel::onSearchChange,
                        label = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.onSearchChange(viewModel.searchInput)
                                focusManager.clearFocus()
                            }
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.cd_search)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                item {
                    HorizontalDivider(color = Color(0xFF2B2C30))
                }

                if (viewModel.notes.isEmpty()) {
                    item {
                        EmptyStateCard(isDarkMode = isDarkMode)
                    }
                } else {
                    itemsIndexed(viewModel.notes, key = { _, note -> note.id }) { index, note ->
                        AnimatedNoteItem(
                            index = index,
                            note = note,
                            onOpen = { openedNoteId = note.id },
                            isDarkMode = isDarkMode
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }

        NoteDetailOverlay(
            note = openedNote,
            onDismiss = { openedNoteId = null },
            onEdit = {
                openedNoteId = null
                viewModel.startEdit(it)
            },
            onDelete = {
                openedNoteId = null
                viewModel.deleteNote(it)
            },
            onShare = { noteForPdfShare = it },
            onImageClick = { imagePaths, index ->
                imageViewerPaths = imagePaths
                imageViewerInitialIndex = index
            }
        )

        SharePdfOptionsOverlay(
            note = noteForPdfShare,
            isSharing = isSharingPdf,
            onDismiss = {
                if (!isSharingPdf) {
                    noteForPdfShare = null
                }
            },
            onShare = { note, withWatermark, paperMode, qualityMode, styleMode ->
                noteForPdfShare = null
                coroutineScope.launch {
                    isSharingPdf = true
                    val pdfUri = withContext(Dispatchers.IO) {
                        buildNotePdfUri(
                            context = context,
                            note = note,
                            includeWatermark = withWatermark,
                            paperMode = paperMode,
                            qualityMode = qualityMode,
                            styleMode = styleMode,
                            appIsDarkTheme = isDarkMode
                        )
                    }
                    isSharingPdf = false

                    if (pdfUri == null) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.msg_share_pdf_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    sharePdf(context, pdfUri, note)
                }
            }
        )

        EditorOverlay(
            visible = viewModel.isComposerVisible,
            isEditing = viewModel.isEditing,
            title = viewModel.titleInput,
            content = viewModel.contentInput,
            images = viewModel.draftImages,
            imageLayoutMode = viewModel.selectedImageLayoutMode,
            onTitleChange = viewModel::onTitleChange,
            onContentChange = viewModel::onContentChange,
            onPickImages = {
                pickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onImageLayoutModeChange = viewModel::onImageLayoutModeChange,
            onTakePhoto = {
                val captureTarget = createTempCameraCaptureTarget(context)
                if (captureTarget == null) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.msg_camera_unavailable),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    pendingCameraFile = captureTarget.file
                    pendingCameraUri = captureTarget.uri
                    cameraLauncher.launch(captureTarget.uri)
                }
            },
            onRemoveImage = viewModel::removeDraftImage,
            onMoveImage = viewModel::moveDraftImage,
            onSave = viewModel::saveNote,
            onDismiss = viewModel::cancelEdit
        )

        CapturedPhotoConfirmOverlay(
            photoUri = capturedPreviewUri,
            isProcessing = isConfirmingCapturedPhoto,
            onConfirm = {
                val sourceFile = capturedPreviewFile ?: return@CapturedPhotoConfirmOverlay
                coroutineScope.launch {
                    isConfirmingCapturedPhoto = true
                    val galleryUri = withContext(Dispatchers.IO) {
                        saveCapturedPhotoToGallery(context, sourceFile)
                    }
                    isConfirmingCapturedPhoto = false

                    if (galleryUri == null) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.msg_camera_save_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    viewModel.addPickedImages(listOf(galleryUri))
                    deleteLocalFile(sourceFile)
                    capturedPreviewFile = null
                    capturedPreviewUri = null
                }
            },
            onDiscard = {
                capturedPreviewFile?.let(::deleteLocalFile)
                capturedPreviewFile = null
                capturedPreviewUri = null
            }
        )

        NoteImageViewerOverlay(
            imagePaths = imageViewerPaths,
            initialIndex = imageViewerInitialIndex,
            onDismiss = {
                imageViewerPaths = emptyList()
                imageViewerInitialIndex = 0
            }
        )

        SettingsOverlay(
            visible = isSettingsVisible,
            isDarkMode = isDarkMode,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            onDismiss = { isSettingsVisible = false }
        )
    }
}

@Composable
private fun HeaderSection(
    isDarkMode: Boolean,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (isDarkMode) {
                        listOf(Color(0xFF1A2433), Color(0xFF1E2A3B), Color(0xFF172130))
                    } else {
                        listOf(Color(0xFFF4F8FF), Color(0xFFECF2FC), Color(0xFFE5ECF8))
                    }
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .border(
                width = 1.dp,
                color = if (isDarkMode) Color(0xFF2C3A4E) else Color(0xFFD3DDEA),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.header_title),
                style = MaterialTheme.typography.headlineMedium,
                color = if (isDarkMode) Color.White else Color(0xFF111317),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp
            )
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = if (isDarkMode) Color(0xFFD9E2EE) else Color(0xFF2D3A4B)
                )
            }
        }
        Text(
            text = stringResource(R.string.header_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDarkMode) Color(0xFF9DA4AE) else Color(0xFF495060),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun EmptyStateCard(isDarkMode: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = if (isDarkMode) Color(0xFF2F3E53) else Color(0xFFD3DCE9),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1A2330) else Color(0xFFFFFFFF)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            Text(
                text = stringResource(R.string.empty_notes_title),
                color = if (isDarkMode) Color.White else Color(0xFF111317),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.empty_notes_hint),
                color = if (isDarkMode) Color(0xFFA6ADBA) else Color(0xFF5C6575),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SettingsOverlay(
    visible: Boolean,
    isDarkMode: Boolean,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val interactionSource = remember { MutableInteractionSource() }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.96f),
        exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.96f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1A2330) else Color.White
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        color = if (isDarkMode) Color.White else Color(0xFF12161E),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.label_app_theme),
                        color = if (isDarkMode) Color(0xFFD8DEE8) else Color(0xFF394457),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(AppThemeMode.entries, key = { _, mode -> mode.storageValue }) { _, mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                label = { Text(appThemeModeLabel(mode)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isDarkMode) Color(0xFFDAEDFF) else Color(0xFF1D2735),
                                    selectedLabelColor = if (isDarkMode) Color.Black else Color.White,
                                    containerColor = if (isDarkMode) Color(0xFF2B3038) else Color(0xFFE0E6F0),
                                    labelColor = if (isDarkMode) Color(0xFFD5DDE9) else Color(0xFF2A3442)
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = stringResource(R.string.btn_done),
                                color = if (isDarkMode) Color(0xFFC3CBD6) else Color(0xFF2C394D)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CapturedPhotoConfirmOverlay(
    photoUri: Uri?,
    isProcessing: Boolean,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit
) {
    val uri = photoUri ?: return
    val interactionSource = remember { MutableInteractionSource() }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.96f),
        exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.96f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.68f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {}
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111317))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.camera_capture_preview_title),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    AsyncImage(
                        model = uri,
                        contentDescription = stringResource(R.string.cd_draft_image),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1B1E22)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDiscard,
                            enabled = !isProcessing
                        ) {
                            Text(stringResource(R.string.btn_discard_photo), color = Color(0xFFBFC7D3))
                        }
                        Button(
                            onClick = onConfirm,
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEDF2F8),
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = if (isProcessing) {
                                    stringResource(R.string.btn_processing)
                                } else {
                                    stringResource(R.string.btn_use_photo)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharePdfOptionsOverlay(
    note: Note?,
    isSharing: Boolean,
    onDismiss: () -> Unit,
    onShare: (Note, Boolean, PdfPaperMode, PdfExportQualityMode, PdfStyleMode) -> Unit
) {
    val currentNote = note ?: return
    val interactionSource = remember { MutableInteractionSource() }
    var selectedPaperMode by remember(currentNote.id) { mutableStateOf(PdfPaperMode.AUTO) }
    var selectedQualityMode by remember(currentNote.id) { mutableStateOf(PdfExportQualityMode.STANDARD) }
    var selectedStyleMode by remember(currentNote.id) { mutableStateOf(PdfStyleMode.FOLLOW_APP) }
    var includeWatermark by remember(currentNote.id) { mutableStateOf(true) }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.96f),
        exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.96f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111317))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.share_pdf_title),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (currentNote.title.isBlank()) {
                            stringResource(R.string.untitled_note)
                        } else {
                            currentNote.title
                        },
                        color = Color(0xFF9CA5B3),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.label_pdf_paper_size),
                        color = Color(0xFFD8DEE8),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(PdfPaperMode.entries, key = { _, mode -> mode.name }) { _, mode ->
                            FilterChip(
                                selected = mode == selectedPaperMode,
                                onClick = { selectedPaperMode = mode },
                                label = {
                                    Text(
                                        text = pdfPaperModeLabel(mode),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                enabled = !isSharing,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFDAEDFF),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF2B3038),
                                    labelColor = Color(0xFFD5DDE9)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.label_pdf_quality),
                        color = Color(0xFFD8DEE8),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PdfExportQualityMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedQualityMode == mode,
                                onClick = { selectedQualityMode = mode },
                                label = { Text(pdfQualityModeLabel(mode)) },
                                enabled = !isSharing,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFDAEDFF),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF2B3038),
                                    labelColor = Color(0xFFD5DDE9)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.label_pdf_style),
                        color = Color(0xFFD8DEE8),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(PdfStyleMode.entries, key = { _, mode -> mode.name }) { _, mode ->
                            FilterChip(
                                selected = selectedStyleMode == mode,
                                onClick = { selectedStyleMode = mode },
                                label = {
                                    Text(
                                        text = pdfStyleModeLabel(mode),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                enabled = !isSharing,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFDAEDFF),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF2B3038),
                                    labelColor = Color(0xFFD5DDE9)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.label_pdf_watermark),
                        color = Color(0xFFD8DEE8),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = includeWatermark,
                            onClick = { includeWatermark = true },
                            label = { Text(stringResource(R.string.pdf_watermark_on)) },
                            enabled = !isSharing,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFDAEDFF),
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF2B3038),
                                labelColor = Color(0xFFD5DDE9)
                            )
                        )
                        FilterChip(
                            selected = !includeWatermark,
                            onClick = { includeWatermark = false },
                            label = { Text(stringResource(R.string.pdf_watermark_off)) },
                            enabled = !isSharing,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFDAEDFF),
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF2B3038),
                                labelColor = Color(0xFFD5DDE9)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            onShare(
                                currentNote,
                                includeWatermark,
                                selectedPaperMode,
                                selectedQualityMode,
                                selectedStyleMode
                            )
                        },
                        enabled = !isSharing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEDF2F8),
                            contentColor = Color.Black
                        )
                    ) {
                        Text(stringResource(R.string.btn_share_pdf_generate))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss, enabled = !isSharing) {
                            Text(stringResource(R.string.btn_cancel), color = Color(0xFFC3CBD6))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorOverlay(
    visible: Boolean,
    isEditing: Boolean,
    title: String,
    content: String,
    images: List<DraftImage>,
    imageLayoutMode: ImageLayoutMode,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onPickImages: () -> Unit,
    onImageLayoutModeChange: (ImageLayoutMode) -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveImage: (DraftImage) -> Unit,
    onMoveImage: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.96f),
        exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.96f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.64f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.96f)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) {
                            stringResource(R.string.overlay_edit_title)
                        } else {
                            stringResource(R.string.overlay_create_title)
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = Color.White
                        )
                    }
                }

                EditorCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    isEditing = isEditing,
                    title = title,
                    content = content,
                    images = images,
                    imageLayoutMode = imageLayoutMode,
                    onTitleChange = onTitleChange,
                    onContentChange = onContentChange,
                    onPickImages = onPickImages,
                    onImageLayoutModeChange = onImageLayoutModeChange,
                    onTakePhoto = onTakePhoto,
                    onRemoveImage = onRemoveImage,
                    onMoveImage = onMoveImage,
                    onSave = onSave,
                    onCancel = onDismiss
                )
            }
        }
    }
}

@Composable
private fun NoteDetailOverlay(
    note: Note?,
    onDismiss: () -> Unit,
    onEdit: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onShare: (Note) -> Unit,
    onImageClick: (List<String>, Int) -> Unit
) {
    val currentNote = note ?: return
    val interactionSource = remember { MutableInteractionSource() }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.95f),
        exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.95f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111317))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = if (currentNote.title.isBlank()) {
                                stringResource(R.string.untitled_note)
                            } else {
                                currentNote.title
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_close),
                                tint = Color.White
                            )
                        }
                    }

                    Text(
                        text = formatTime(currentNote.updatedAt),
                        color = Color(0xFF98A0AE),
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (currentNote.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = currentNote.content,
                            color = Color(0xFFE5E8EE),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (currentNote.imagePaths.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        NoteImagesMosaic(
                            imagePaths = currentNote.imagePaths,
                            maxPreview = Int.MAX_VALUE,
                            layoutMode = currentNote.imageLayoutMode,
                            onImageClick = { index ->
                                onImageClick(currentNote.imagePaths, index)
                            }
                        )
                    }

                    if (currentNote.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = currentNote.tags.joinToString(separator = "  ") { "#$it" },
                            color = Color(0xFF5CB9FF),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF2B2E34))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { onShare(currentNote) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.cd_share_pdf),
                                tint = Color(0xFF98D3FF)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.btn_share_pdf),
                                color = Color(0xFF98D3FF)
                            )
                        }
                        TextButton(onClick = { onEdit(currentNote) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.cd_edit),
                                tint = Color(0xFFCFD5DF)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_edit), color = Color(0xFFCFD5DF))
                        }
                        TextButton(onClick = { onDelete(currentNote) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = stringResource(R.string.cd_delete),
                                tint = Color(0xFFFF8A8A)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_delete), color = Color(0xFFFF8A8A))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteImageViewerOverlay(
    imagePaths: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    if (imagePaths.isEmpty()) return
    val safeInitialPage = initialIndex.coerceIn(0, imagePaths.lastIndex)
    val pagerState = rememberPagerState(
        initialPage = safeInitialPage,
        pageCount = { imagePaths.size }
    )

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(180))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.97f))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ZoomableViewerImage(
                    path = imagePaths[page],
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${imagePaths.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableViewerImage(path: String, modifier: Modifier = Modifier) {
    val file = remember(path) { File(path) }
    var scale by remember(path) { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(path) {
                while (true) {
                    awaitPointerEventScope {
                        var handledMultiTouch = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressedCount = event.changes.count { it.pressed }
                            if (pressedCount >= 2) {
                                handledMultiTouch = true
                                val zoomChange = event.calculateZoom()
                                if (zoomChange.isFinite() && zoomChange > 0f) {
                                    scale = (scale * zoomChange).coerceIn(0.05f, 8f)
                                }
                                event.changes.forEach { change ->
                                    change.consume()
                                }
                            }
                            if (event.changes.none { it.pressed }) break
                        }
                        if (handledMultiTouch && scale < 1f) {
                            scale = 1f
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (file.exists()) {
            AsyncImage(
                model = file,
                contentDescription = stringResource(R.string.cd_note_photo),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun EditorCard(
    modifier: Modifier = Modifier,
    isEditing: Boolean,
    title: String,
    content: String,
    images: List<DraftImage>,
    imageLayoutMode: ImageLayoutMode,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onPickImages: () -> Unit,
    onImageLayoutModeChange: (ImageLayoutMode) -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveImage: (DraftImage) -> Unit,
    onMoveImage: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var isFocusMode by rememberSaveable { mutableStateOf(false) }
    var isContentFocused by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141518))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) {
                        stringResource(R.string.editor_editing_title)
                    } else {
                        stringResource(R.string.editor_new_title)
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { isFocusMode = !isFocusMode }) {
                    Text(
                        text = if (isFocusMode) {
                            stringResource(R.string.btn_focus_mode_off)
                        } else {
                            stringResource(R.string.btn_focus_mode_on)
                        },
                        color = Color(0xFFBFE4FF)
                    )
                }
            }

            AnimatedVisibility(
                visible = isEditing && !isFocusMode,
                enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 4 },
                exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 4 }
            ) {
                Text(
                    text = stringResource(R.string.editor_editing_notice),
                    color = Color(0xFF4DB6FF),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (!isFocusMode) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(stringResource(R.string.label_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.hint_focus_mode),
                    color = Color(0xFF89B8DD),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                label = { Text(stringResource(R.string.label_content_with_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onFocusChanged { state ->
                        isContentFocused = state.isFocused
                        if (state.isFocused) {
                            coroutineScope.launch {
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                minLines = if (isFocusMode) 8 else 4,
                maxLines = Int.MAX_VALUE,
                shape = RoundedCornerShape(14.dp)
            )

            if (!isFocusMode && !isContentFocused && images.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                DraftImagesRow(
                    images = images,
                    onRemoveImage = onRemoveImage,
                    onMoveImage = onMoveImage
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.hint_drag_to_sort),
                    color = Color(0xFF8D96A3),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))
                ImageLayoutModeSelector(
                    imageCount = images.size,
                    selectedMode = imageLayoutMode,
                    onModeSelected = onImageLayoutModeChange
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!isFocusMode && !isContentFocused) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onPickImages) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = stringResource(R.string.cd_pick_photos),
                            tint = Color(0xFFCCE7FF)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_add_photos), color = Color(0xFFCCE7FF))
                    }

                    TextButton(onClick = onTakePhoto) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = stringResource(R.string.cd_take_photo),
                            tint = Color(0xFFDFF3FF)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_take_photo), color = Color(0xFFDFF3FF))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.btn_cancel), color = Color(0xFFB8C0CC))
                }

                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEDF2F8),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = stringResource(R.string.cd_save))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isEditing) {
                            stringResource(R.string.btn_update)
                        } else {
                            stringResource(R.string.btn_save)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageLayoutModeSelector(
    imageCount: Int,
    selectedMode: ImageLayoutMode,
    onModeSelected: (ImageLayoutMode) -> Unit
) {
    val options = remember(imageCount) {
        ImageLayoutMode.availableForImageCount(imageCount)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.label_image_layout_mode),
            color = Color(0xFFDDE3ED),
            style = MaterialTheme.typography.bodySmall
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(options, key = { _, mode -> mode.storageValue }) { _, mode ->
                FilterChip(
                    selected = mode == selectedMode,
                    onClick = { onModeSelected(mode) },
                    label = { Text(text = layoutModeLabel(mode, imageCount)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFDAEDFF),
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF232830),
                        labelColor = Color(0xFFD2D9E5)
                    )
                )
            }
        }
        Text(
            text = stringResource(R.string.hint_image_layout_mode),
            color = Color(0xFF8D96A3),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun layoutModeLabel(mode: ImageLayoutMode, imageCount: Int): String {
    return when (mode) {
        ImageLayoutMode.AUTO -> stringResource(R.string.layout_mode_auto)
        ImageLayoutMode.SMALL -> stringResource(R.string.layout_mode_small)
        ImageLayoutMode.TWO_COLUMNS -> stringResource(R.string.layout_mode_two_columns)
        ImageLayoutMode.LARGE_FIRST -> {
            if (imageCount == 1) {
                stringResource(R.string.layout_mode_large_single)
            } else {
                stringResource(R.string.layout_mode_large_first)
            }
        }
        ImageLayoutMode.LARGE_LAST -> stringResource(R.string.layout_mode_large_last)
    }
}

@Composable
private fun pdfPaperModeLabel(mode: PdfPaperMode): String {
    return when (mode) {
        PdfPaperMode.AUTO -> stringResource(R.string.pdf_paper_auto)
        PdfPaperMode.SMALL -> stringResource(R.string.pdf_paper_small)
        PdfPaperMode.MEDIUM -> stringResource(R.string.pdf_paper_medium)
        PdfPaperMode.LARGE -> stringResource(R.string.pdf_paper_large)
    }
}

@Composable
private fun appThemeModeLabel(mode: AppThemeMode): String {
    return when (mode) {
        AppThemeMode.FOLLOW_SYSTEM -> stringResource(R.string.app_theme_follow_system)
        AppThemeMode.BLACK -> stringResource(R.string.app_theme_black)
        AppThemeMode.WHITE -> stringResource(R.string.app_theme_white)
    }
}

@Composable
private fun pdfQualityModeLabel(mode: PdfExportQualityMode): String {
    return when (mode) {
        PdfExportQualityMode.STANDARD -> stringResource(R.string.pdf_quality_standard)
        PdfExportQualityMode.ULTRA -> stringResource(R.string.pdf_quality_ultra)
    }
}

@Composable
private fun pdfStyleModeLabel(mode: PdfStyleMode): String {
    return when (mode) {
        PdfStyleMode.FOLLOW_APP -> stringResource(R.string.pdf_style_follow_app)
        PdfStyleMode.BLACK_THEME -> stringResource(R.string.pdf_style_black_theme)
        PdfStyleMode.WHITE_THEME -> stringResource(R.string.pdf_style_white_theme)
    }
}

@Composable
private fun DraftImagesRow(
    images: List<DraftImage>,
    onRemoveImage: (DraftImage) -> Unit,
    onMoveImage: (Int, Int) -> Unit
) {
    val density = LocalDensity.current
    val reorderStepPx = with(density) { 88.dp.toPx() }

    var draggedImageId by remember { mutableStateOf<String?>(null) }
    var dragDistanceX by remember { mutableFloatStateOf(0f) }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(items = images, key = { _, image -> image.id }) { _, image ->
            val isDragging = draggedImageId == image.id
            val animatedScale by animateFloatAsState(
                targetValue = if (isDragging) 1.08f else 1f,
                animationSpec = tween(180),
                label = "drag-scale"
            )
            val animatedElevation by animateDpAsState(
                targetValue = if (isDragging) 12.dp else 0.dp,
                animationSpec = tween(180),
                label = "drag-elevation"
            )
            val borderAlpha by animateFloatAsState(
                targetValue = if (isDragging) 1f else 0f,
                animationSpec = tween(180),
                label = "drag-border"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = if (isDragging) dragDistanceX else 0f
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
                    .shadow(animatedElevation, RoundedCornerShape(14.dp))
                    .size(98.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1A1D20))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF5CB9FF).copy(alpha = borderAlpha),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .pointerInput(image.id, images) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedImageId = image.id
                                dragDistanceX = 0f
                            },
                            onDragEnd = {
                                draggedImageId = null
                                dragDistanceX = 0f
                            },
                            onDragCancel = {
                                draggedImageId = null
                                dragDistanceX = 0f
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            if (draggedImageId != image.id) return@detectDragGesturesAfterLongPress

                            dragDistanceX += dragAmount.x
                            val currentIndex = images.indexOfFirst { it.id == image.id }
                            if (currentIndex == -1) return@detectDragGesturesAfterLongPress

                            if (dragDistanceX >= reorderStepPx && currentIndex < images.lastIndex) {
                                onMoveImage(currentIndex, currentIndex + 1)
                                dragDistanceX -= reorderStepPx
                            } else if (dragDistanceX <= -reorderStepPx && currentIndex > 0) {
                                onMoveImage(currentIndex, currentIndex - 1)
                                dragDistanceX += reorderStepPx
                            }
                        }
                    }
            ) {
                AsyncImage(
                    model = imageModel(image),
                    contentDescription = stringResource(R.string.cd_draft_image),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { onRemoveImage(image) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .background(Color(0x88000000), RoundedCornerShape(999.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_remove_image),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedNoteItem(
    index: Int,
    note: Note,
    onOpen: () -> Unit,
    isDarkMode: Boolean
) {
    var visible by remember(note.id) { mutableStateOf(false) }

    LaunchedEffect(note.id) {
        delay((index * 45L).coerceAtMost(260L))
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 7 },
        exit = fadeOut(tween(180))
    ) {
        NoteListCard(note = note, onOpen = onOpen, isDarkMode = isDarkMode)
    }
}

@Composable
private fun NoteListCard(note: Note, onOpen: () -> Unit, isDarkMode: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = if (isDarkMode) Color(0xFF2F3E53) else Color(0xFFD3DCE9),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1A2330) else Color(0xFFFFFFFF)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = if (note.title.isBlank()) stringResource(R.string.untitled_note) else note.title,
                color = if (isDarkMode) Color.White else Color(0xFF151922),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = formatTime(note.updatedAt),
                    color = if (isDarkMode) Color(0xFF939BA7) else Color(0xFF667082),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = note.content,
                    color = if (isDarkMode) Color(0xFFC9CFDA) else Color(0xFF3F4756),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (note.imagePaths.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                NoteImagesMosaic(
                    imagePaths = note.imagePaths,
                    maxPreview = 9,
                    layoutMode = note.imageLayoutMode
                )
            }

            if (note.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note.tags.joinToString(separator = "  ") { "#$it" },
                    color = Color(0xFF5CB9FF),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

        }
    }
}

@Composable
private fun NoteImagesMosaic(
    imagePaths: List<String>,
    maxPreview: Int,
    layoutMode: ImageLayoutMode,
    onImageClick: ((Int) -> Unit)? = null
) {
    val visibleEntries = if (maxPreview == Int.MAX_VALUE) {
        imagePaths.mapIndexed { index, path -> MosaicImageEntry(absoluteIndex = index, path = path) }
    } else {
        imagePaths.take(maxPreview).mapIndexed { index, path ->
            MosaicImageEntry(absoluteIndex = index, path = path)
        }
    }
    if (visibleEntries.isEmpty()) return

    when (layoutMode) {
        ImageLayoutMode.AUTO -> NoteImagesMosaicAuto(visibleEntries, onImageClick)
        ImageLayoutMode.SMALL -> NoteImagesMosaicSmall(visibleEntries, onImageClick)
        ImageLayoutMode.TWO_COLUMNS -> NoteImagesMosaicTwoColumns(visibleEntries, onImageClick)
        ImageLayoutMode.LARGE_FIRST -> NoteImagesMosaicFocus(visibleEntries, onImageClick, focusLast = false)
        ImageLayoutMode.LARGE_LAST -> NoteImagesMosaicFocus(visibleEntries, onImageClick, focusLast = true)
    }
}

@Composable
private fun NoteImagesMosaicAuto(
    visibleEntries: List<MosaicImageEntry>,
    onImageClick: ((Int) -> Unit)?
) {
    when (visibleEntries.size) {
        1 -> {
            MosaicImageCell(
                entry = visibleEntries[0],
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp),
                onImageClick = onImageClick
            )
        }
        2 -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MosaicImageCell(
                    entry = visibleEntries[0],
                    modifier = Modifier
                        .weight(1f)
                        .height(128.dp),
                    onImageClick = onImageClick
                )
                MosaicImageCell(
                    entry = visibleEntries[1],
                    modifier = Modifier
                        .weight(1f)
                        .height(128.dp),
                    onImageClick = onImageClick
                )
            }
        }
        3 -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MosaicImageCell(
                        entry = visibleEntries[0],
                        modifier = Modifier
                            .weight(1f)
                            .height(94.dp),
                        onImageClick = onImageClick
                    )
                    MosaicImageCell(
                        entry = visibleEntries[1],
                        modifier = Modifier
                            .weight(1f)
                            .height(94.dp),
                        onImageClick = onImageClick
                    )
                }
                MosaicImageCell(
                    entry = visibleEntries[2],
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp),
                    onImageClick = onImageClick
                )
            }
        }
        4 -> NoteImagesMosaicGrid(
            entries = visibleEntries,
            columns = 2,
            fixedHeight = 98.dp,
            onImageClick = onImageClick
        )
        else -> NoteImagesMosaicGrid(
            entries = visibleEntries,
            columns = 3,
            fixedHeight = null,
            onImageClick = onImageClick
        )
    }
}

@Composable
private fun NoteImagesMosaicSmall(
    visibleEntries: List<MosaicImageEntry>,
    onImageClick: ((Int) -> Unit)?
) {
    if (visibleEntries.size == 1) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            MosaicImageCell(
                entry = visibleEntries[0],
                modifier = Modifier
                    .size(112.dp),
                onImageClick = onImageClick
            )
        }
        return
    }
    NoteImagesMosaicGrid(
        entries = visibleEntries,
        columns = 3,
        fixedHeight = null,
        onImageClick = onImageClick
    )
}

@Composable
private fun NoteImagesMosaicTwoColumns(
    visibleEntries: List<MosaicImageEntry>,
    onImageClick: ((Int) -> Unit)?
) {
    val columns = if (visibleEntries.size == 1) 1 else 2
    NoteImagesMosaicGrid(
        entries = visibleEntries,
        columns = columns,
        fixedHeight = 118.dp,
        onImageClick = onImageClick
    )
}

@Composable
private fun NoteImagesMosaicFocus(
    visibleEntries: List<MosaicImageEntry>,
    onImageClick: ((Int) -> Unit)?,
    focusLast: Boolean
) {
    if (visibleEntries.size == 1) {
        MosaicImageCell(
            entry = visibleEntries[0],
            modifier = Modifier
                .fillMaxWidth()
                .height(188.dp),
            onImageClick = onImageClick
        )
        return
    }

    val focusIndex = if (focusLast) visibleEntries.lastIndex else 0
    val focusEntry = visibleEntries[focusIndex]
    val sideEntries = visibleEntries.filterIndexed { index, _ -> index != focusIndex }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!focusLast) {
                MosaicImageCell(
                    entry = focusEntry,
                    modifier = Modifier
                        .weight(1.85f)
                        .height(176.dp),
                    onImageClick = onImageClick
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(176.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val topEntry = sideEntries.getOrNull(0)
                val secondEntry = sideEntries.getOrNull(1)
                if (topEntry != null) {
                    MosaicImageCell(
                        entry = topEntry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(if (secondEntry == null) 1f else 0.5f),
                        onImageClick = onImageClick
                    )
                }
                if (secondEntry != null) {
                    MosaicImageCell(
                        entry = secondEntry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.5f),
                        onImageClick = onImageClick
                    )
                }
                if (topEntry == null) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            if (focusLast) {
                MosaicImageCell(
                    entry = focusEntry,
                    modifier = Modifier
                        .weight(1.85f)
                        .height(176.dp),
                    onImageClick = onImageClick
                )
            }
        }

        val remaining = sideEntries.drop(2)
        if (remaining.isNotEmpty()) {
            NoteImagesMosaicGrid(
                entries = remaining,
                columns = 3,
                fixedHeight = null,
                onImageClick = onImageClick
            )
        }
    }
}

@Composable
private fun NoteImagesMosaicGrid(
    entries: List<MosaicImageEntry>,
    columns: Int,
    fixedHeight: androidx.compose.ui.unit.Dp?,
    onImageClick: ((Int) -> Unit)?
) {
    if (entries.isEmpty()) return

    val rows = entries.chunked(columns)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { rowEntries ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowEntries.forEach { entry ->
                    val cellModifier = if (fixedHeight != null) {
                        Modifier
                            .weight(1f)
                            .height(fixedHeight)
                    } else {
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    }
                    MosaicImageCell(
                        entry = entry,
                        modifier = cellModifier,
                        onImageClick = onImageClick
                    )
                }
                repeat(columns - rowEntries.size) {
                    val spacerModifier = if (fixedHeight != null) {
                        Modifier
                            .weight(1f)
                            .height(fixedHeight)
                    } else {
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    }
                    Spacer(modifier = spacerModifier)
                }
            }
        }
    }
}

@Composable
private fun MosaicImageCell(
    entry: MosaicImageEntry,
    modifier: Modifier,
    onImageClick: ((Int) -> Unit)?
) {
    val file = File(entry.path)
    val clickableModifier = if (onImageClick != null) {
        modifier.clickable { onImageClick(entry.absoluteIndex) }
    } else {
        modifier
    }
    Box(
        modifier = clickableModifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1C1F24))
    ) {
        if (file.exists()) {
            AsyncImage(
                model = file,
                contentDescription = stringResource(R.string.cd_note_photo),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun imageModel(image: DraftImage): Any {
    return when (image) {
        is DraftImage.Existing -> File(image.path)
        is DraftImage.New -> image.uri
    }
}

private data class CameraCaptureTarget(
    val file: File,
    val uri: Uri
)

private fun createTempCameraCaptureTarget(context: Context): CameraCaptureTarget? {
    return runCatching {
        val captureDir = File(context.cacheDir, "camera_captures")
        if (!captureDir.exists()) {
            captureDir.mkdirs()
        }
        val file = File(captureDir, "capture-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        CameraCaptureTarget(file = file, uri = uri)
    }.getOrNull()
}

private fun saveCapturedPhotoToGallery(context: Context, sourceFile: File): Uri? {
    if (!sourceFile.exists() || sourceFile.length() <= 0L) return null

    val imageName = "NoteAnytime_${System.currentTimeMillis()}.jpg"
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/Note Anytime"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val destinationUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return null

    return try {
        val output = resolver.openOutputStream(destinationUri)
            ?: throw IllegalStateException("Cannot open destination output stream")
        sourceFile.inputStream().use { input ->
            output.use { stream ->
                input.copyTo(stream)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val publishValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(destinationUri, publishValues, null, null)
        }
        destinationUri
    } catch (_: Exception) {
        runCatching { resolver.delete(destinationUri, null, null) }
        null
    }
}

private fun deleteLocalFile(file: File) {
    runCatching {
        if (file.exists()) {
            file.delete()
        }
    }
}

private fun sharePdf(context: Context, pdfUri: Uri, note: Note) {
    val title = if (note.title.isBlank()) {
        context.getString(R.string.untitled_note)
    } else {
        note.title
    }

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, pdfUri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TITLE, title)
        clipData = ClipData.newUri(context.contentResolver, "note_pdf", pdfUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(
        sendIntent,
        context.getString(R.string.share_pdf_chooser_title)
    )
    context.startActivity(chooser)
}

private fun buildNotePdfUri(
    context: Context,
    note: Note,
    includeWatermark: Boolean,
    paperMode: PdfPaperMode,
    qualityMode: PdfExportQualityMode,
    styleMode: PdfStyleMode,
    appIsDarkTheme: Boolean
): Uri? {
    val outputDir = File(context.cacheDir, "shared_pdfs")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }

    val noteTitle = if (note.title.isBlank()) {
        context.getString(R.string.untitled_note)
    } else {
        note.title
    }
    val fileDate = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date(note.updatedAt))
    val outputFile = File(outputDir, "${sanitizeFileName(noteTitle)}_$fileDate.pdf")
    val document = PdfDocument()

    val resolvedCardSize = resolvePdfCardSizeMode(note, paperMode)
    val metrics = resolvePdfCardMetrics(resolvedCardSize)
    val renderScale = resolvePdfRenderScale(qualityMode)
    val logicalPageWidth = metrics.pageWidth
    val logicalPageHeight = metrics.pageHeight
    val pageWidth = (logicalPageWidth * renderScale).toInt().coerceAtLeast(1)
    val pageHeight = (logicalPageHeight * renderScale).toInt().coerceAtLeast(1)
    val cardPaddingX = metrics.cardPaddingX
    val cardPaddingTop = metrics.cardPaddingTop
    val footerAreaHeight = metrics.footerAreaHeight
    val scale = metrics.scale

    val cardRect = RectF(
        metrics.cardInsetX,
        metrics.cardInsetTop,
        logicalPageWidth - metrics.cardInsetX,
        logicalPageHeight - metrics.cardInsetBottom
    )
    val contentLeft = cardRect.left + cardPaddingX
    val contentRight = cardRect.right - cardPaddingX
    val contentWidth = contentRight - contentLeft

    val isDarkPdfTheme = when (styleMode) {
        PdfStyleMode.FOLLOW_APP -> appIsDarkTheme
        PdfStyleMode.BLACK_THEME -> true
        PdfStyleMode.WHITE_THEME -> false
    }
    val pageBackgroundColor = if (isDarkPdfTheme) {
        android.graphics.Color.parseColor("#070B12")
    } else {
        android.graphics.Color.parseColor("#F3F5F8")
    }
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkPdfTheme) {
            android.graphics.Color.parseColor("#172131")
        } else {
            android.graphics.Color.WHITE
        }
    }
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkPdfTheme) {
            android.graphics.Color.parseColor("#2A3B55")
        } else {
            android.graphics.Color.parseColor("#E6E9EE")
        }
        strokeWidth = 2f
    }
    val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkPdfTheme) {
            android.graphics.Color.parseColor("#F4F8FF")
        } else {
            android.graphics.Color.parseColor("#111318")
        }
        textSize = 44f * scale
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val headerMetaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkPdfTheme) {
            android.graphics.Color.parseColor("#A8B8CE")
        } else {
            android.graphics.Color.parseColor("#8F97A5")
        }
        textSize = 24f * scale
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkPdfTheme) {
            android.graphics.Color.parseColor("#E8EEF8")
        } else {
            android.graphics.Color.parseColor("#171A1F")
        }
        textSize = 30f * scale
    }
    val chipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkPdfTheme) {
            android.graphics.Color.parseColor("#CCE3FF")
        } else {
            android.graphics.Color.parseColor("#135CAB")
        }
        textSize = 24f * scale
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val chipBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkPdfTheme) {
            android.graphics.Color.parseColor("#23344D")
        } else {
            android.graphics.Color.parseColor("#EAF3FF")
        }
    }
    val imagePlaceholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkPdfTheme) {
            android.graphics.Color.parseColor("#243346")
        } else {
            android.graphics.Color.parseColor("#E8EBF0")
        }
    }
    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkPdfTheme) {
            android.graphics.Color.parseColor("#9EB0C8")
        } else {
            android.graphics.Color.parseColor("#8A92A0")
        }
        textSize = 22f * scale
    }
    val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkPdfTheme) {
            android.graphics.Color.parseColor("#92A4BA")
        } else {
            android.graphics.Color.parseColor("#97A0AF")
        }
        textSize = 20f * scale
    }

    var pageNumber = 0
    lateinit var page: PdfDocument.Page
    lateinit var canvas: Canvas
    var y = 0f
    var contentBottomLimit = 0f

    val exportedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    val previewBodyLineLimit = when (resolvedCardSize) {
        PdfCardSizeMode.SMALL -> 2
        PdfCardSizeMode.MEDIUM -> 8
        PdfCardSizeMode.LARGE -> Int.MAX_VALUE
    }
    val contentStartSpacing = when (resolvedCardSize) {
        PdfCardSizeMode.SMALL -> 34f * scale
        PdfCardSizeMode.MEDIUM -> 30f * scale
        PdfCardSizeMode.LARGE -> 26f * scale
    }
    val pdfImagePaths = when (resolvedCardSize) {
        PdfCardSizeMode.LARGE -> note.imagePaths
        PdfCardSizeMode.SMALL, PdfCardSizeMode.MEDIUM -> note.imagePaths.take(9)
    }

    fun startPage() {
        pageNumber += 1
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        page = document.startPage(pageInfo)
        canvas = page.canvas
        canvas.scale(renderScale, renderScale)
        canvas.drawColor(pageBackgroundColor)
        val cardCornerRadius = 30f * scale
        canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, cardPaint)

        val headerBottom = when (resolvedCardSize) {
            PdfCardSizeMode.LARGE -> {
                val titleLines = splitTextToLines(
                    text = noteTitle,
                    paint = headerTitlePaint,
                    maxWidth = contentWidth
                ).take(2)
                var titleY = cardRect.top + cardPaddingTop + headerTitlePaint.textSize
                titleLines.forEach { line ->
                    canvas.drawText(line, contentLeft, titleY, headerTitlePaint)
                    titleY += headerTitlePaint.fontSpacing
                }
                canvas.drawText(
                    exportedAt,
                    contentLeft,
                    titleY - (headerTitlePaint.fontSpacing - headerMetaPaint.fontSpacing),
                    headerMetaPaint
                )
                titleY + (12f * scale)
            }
            PdfCardSizeMode.SMALL, PdfCardSizeMode.MEDIUM -> {
                val titleLines = splitTextToLines(
                    text = noteTitle,
                    paint = headerTitlePaint,
                    maxWidth = contentWidth
                ).take(1)
                val titleY = cardRect.top + cardPaddingTop + headerTitlePaint.textSize
                titleLines.forEach { line ->
                    canvas.drawText(line, contentLeft, titleY, headerTitlePaint)
                }
                val exportDateWidth = headerMetaPaint.measureText(exportedAt)
                val titleDateGap = if (resolvedCardSize == PdfCardSizeMode.SMALL) {
                    headerMetaPaint.fontSpacing * 1.18f
                } else {
                    headerMetaPaint.fontSpacing * 0.95f
                }
                val dateY = titleY + titleDateGap
                canvas.drawText(
                    exportedAt,
                    contentRight - exportDateWidth,
                    dateY,
                    headerMetaPaint
                )
                dateY + (12f * scale)
            }
        }
        canvas.drawLine(contentLeft, headerBottom, contentRight, headerBottom, dividerPaint)

        val footerTop = cardRect.bottom - footerAreaHeight
        canvas.drawLine(contentLeft, footerTop, contentRight, footerTop, dividerPaint)
        val pageText = "${pageNumber}"
        val pageTextWidth = footerPaint.measureText(pageText)
        canvas.drawText(
            pageText,
            contentRight - pageTextWidth,
            cardRect.bottom - (30f * scale),
            footerPaint
        )

        val appName = context.getString(R.string.app_name)
        val appAndWatermarkText = if (includeWatermark) {
            "$appName · ${context.getString(R.string.pdf_watermark_text)}"
        } else {
            ""
        }
        if (appAndWatermarkText.isNotBlank()) {
            val footerMaxWidth = contentWidth - pageTextWidth - (24f * scale)
            val footerDisplayText = ellipsizeText(appAndWatermarkText, watermarkPaint, footerMaxWidth)
            canvas.drawText(
                footerDisplayText,
                contentLeft,
                cardRect.bottom - (30f * scale),
                watermarkPaint
            )
        }

        y = headerBottom + contentStartSpacing
        contentBottomLimit = footerTop
    }

    fun finishPage() {
        document.finishPage(page)
    }

    fun ensureSpace(requiredHeight: Float) {
        if (y + requiredHeight <= contentBottomLimit) return
        finishPage()
        startPage()
    }

    return try {
        startPage()

        if (note.content.isNotBlank()) {
            val allBodyLines = splitTextToLines(note.content, bodyPaint, contentWidth)
            val bodyLines = allBodyLines.take(previewBodyLineLimit)
            val bodyLineHeight = bodyPaint.fontSpacing * 1.2f
            bodyLines.forEachIndexed { index, line ->
                ensureSpace(bodyLineHeight)
                val displayText = if (
                    previewBodyLineLimit != Int.MAX_VALUE &&
                    index == bodyLines.lastIndex &&
                    allBodyLines.size > bodyLines.size
                ) {
                    ellipsizeText("$line...", bodyPaint, contentWidth)
                } else {
                    line
                }
                canvas.drawText(displayText, contentLeft, y, bodyPaint)
                y += bodyLineHeight
            }
        }

        if (note.tags.isNotEmpty()) {
            y += 18f * scale
            val chipHeight = 42f * scale
            val chipRadius = 21f * scale
            var chipX = contentLeft
            var chipY = y

            note.tags.forEach { tag ->
                val label = "#$tag"
                val chipWidth = chipTextPaint.measureText(label) + (32f * scale)
                if (chipX + chipWidth > contentRight) {
                    chipX = contentLeft
                    chipY += chipHeight + (10f * scale)
                }

                if (chipY + chipHeight > contentBottomLimit) {
                    finishPage()
                    startPage()
                    chipX = contentLeft
                    chipY = y
                }

                val chipRect = RectF(chipX, chipY, chipX + chipWidth, chipY + chipHeight)
                canvas.drawRoundRect(chipRect, chipRadius, chipRadius, chipBackgroundPaint)
                canvas.drawText(
                    label,
                    chipX + (16f * scale),
                    chipY + chipHeight - (13f * scale),
                    chipTextPaint
                )
                chipX += chipWidth + (10f * scale)
            }
            y = chipY + chipHeight + (18f * scale)
        }

        if (pdfImagePaths.isNotEmpty()) {
            val imageRows = buildPdfImageRows(
                imagePaths = pdfImagePaths,
                layoutMode = resolvePdfImageLayoutMode(
                    requestedMode = note.imageLayoutMode,
                    imageCount = pdfImagePaths.size
                ),
                contentWidth = contentWidth,
                gap = metrics.imageGap
            )

            imageRows.forEachIndexed { rowIndex, row ->
                val hasNextRow = rowIndex < imageRows.lastIndex
                val rowTrailingGap = if (hasNextRow) metrics.imageGap else 0f
                ensureSpace(row.height + rowTrailingGap)
                row.items.forEach { item ->
                    val rect = RectF(
                        contentLeft + item.left,
                        y + item.top,
                        contentLeft + item.left + item.width,
                        y + item.top + item.height
                    )
                    canvas.drawRoundRect(
                        rect,
                        metrics.imageCornerRadius,
                        metrics.imageCornerRadius,
                        imagePlaceholderPaint
                    )

                    val bitmap = decodeBitmapForPdf(
                        path = item.path,
                        targetWidth = (item.width * renderScale).toInt().coerceAtLeast(1),
                        targetHeight = (item.height * renderScale).toInt().coerceAtLeast(1),
                        qualityMode = qualityMode
                    )
                    if (bitmap != null) {
                        drawRoundedBitmap(
                            canvas = canvas,
                            bitmap = bitmap,
                            rect = rect,
                            radius = metrics.imageCornerRadius
                        )
                        bitmap.recycle()
                    }
                }
                y += row.height + rowTrailingGap
            }
        }

        finishPage()
        FileOutputStream(outputFile).use { output ->
            document.writeTo(output)
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outputFile
        )
    } catch (_: Exception) {
        null
    } finally {
        document.close()
    }
}

private fun decodeBitmapForPdf(
    path: String,
    targetWidth: Int,
    targetHeight: Int,
    qualityMode: PdfExportQualityMode
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val safeTargetWidth = targetWidth.coerceAtLeast(1)
    val safeTargetHeight = targetHeight.coerceAtLeast(1)
    val multiplier = when (qualityMode) {
        PdfExportQualityMode.STANDARD -> 4
        PdfExportQualityMode.ULTRA -> 8
    }
    val maxDimension = when (qualityMode) {
        PdfExportQualityMode.STANDARD -> 8192
        PdfExportQualityMode.ULTRA -> 16384
    }
    val minimumScaleFactor = when (qualityMode) {
        PdfExportQualityMode.STANDARD -> 2
        PdfExportQualityMode.ULTRA -> 3
    }
    val requestedWidth = (safeTargetWidth * multiplier).coerceAtMost(maxDimension)
    val requestedHeight = (safeTargetHeight * multiplier).coerceAtMost(maxDimension)

    val widthRatio = bounds.outWidth.toFloat() / requestedWidth.toFloat()
    val heightRatio = bounds.outHeight.toFloat() / requestedHeight.toFloat()
    var sampleSize = maxOf(widthRatio, heightRatio).toInt().coerceAtLeast(1)

    fun decodeWithSample(size: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = size.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(path, options)
    }

    var bitmap = decodeWithSample(sampleSize)
    while (
        bitmap != null &&
        sampleSize > 1 &&
        (
            bitmap.width < safeTargetWidth * minimumScaleFactor ||
            bitmap.height < safeTargetHeight * minimumScaleFactor
        )
    ) {
        bitmap.recycle()
        sampleSize = (sampleSize / 2).coerceAtLeast(1)
        bitmap = decodeWithSample(sampleSize)
    }
    return bitmap
}

private fun drawRoundedBitmap(
    canvas: Canvas,
    bitmap: Bitmap,
    rect: RectF,
    radius: Float
) {
    val clipPath = Path().apply {
        addRoundRect(rect, radius, radius, Path.Direction.CW)
    }
    val destinationRatio = rect.width() / rect.height()
    val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val srcRect = if (bitmapRatio > destinationRatio) {
        // Bitmap is wider: crop left/right and keep center area.
        val srcWidth = (bitmap.height * destinationRatio).toInt().coerceAtMost(bitmap.width)
        val left = ((bitmap.width - srcWidth) / 2).coerceAtLeast(0)
        Rect(left, 0, left + srcWidth, bitmap.height)
    } else {
        // Bitmap is taller: crop top/bottom and keep center area.
        val srcHeight = (bitmap.width / destinationRatio).toInt().coerceAtMost(bitmap.height)
        val top = ((bitmap.height - srcHeight) / 2).coerceAtLeast(0)
        Rect(0, top, bitmap.width, top + srcHeight)
    }
    val checkpoint = canvas.save()
    canvas.clipPath(clipPath)
    val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    canvas.drawBitmap(bitmap, srcRect, rect, bitmapPaint)
    canvas.restoreToCount(checkpoint)
}

private fun resolvePdfCardMetrics(sizeMode: PdfCardSizeMode): PdfCardMetrics {
    return when (sizeMode) {
        PdfCardSizeMode.SMALL -> PdfCardMetrics(
            pageWidth = 1240,
            pageHeight = 1754,
            cardInsetX = 220f,
            cardInsetTop = 310f,
            cardInsetBottom = 430f,
            cardPaddingX = 34f,
            cardPaddingTop = 30f,
            footerAreaHeight = 64f,
            scale = 0.8f,
            imageGap = 8f,
            imageCornerRadius = 12f
        )
        PdfCardSizeMode.MEDIUM -> PdfCardMetrics(
            pageWidth = 1240,
            pageHeight = 1754,
            cardInsetX = 112f,
            cardInsetTop = 168f,
            cardInsetBottom = 148f,
            cardPaddingX = 44f,
            cardPaddingTop = 38f,
            footerAreaHeight = 78f,
            scale = 0.92f,
            imageGap = 9f,
            imageCornerRadius = 14f
        )
        PdfCardSizeMode.LARGE -> PdfCardMetrics(
            pageWidth = 1240,
            pageHeight = 1754,
            cardInsetX = 42f,
            cardInsetTop = 74f,
            cardInsetBottom = 62f,
            cardPaddingX = 56f,
            cardPaddingTop = 48f,
            footerAreaHeight = 88f,
            scale = 1f,
            imageGap = 10f,
            imageCornerRadius = 16f
        )
    }
}

private fun resolvePdfRenderScale(qualityMode: PdfExportQualityMode): Float {
    return when (qualityMode) {
        PdfExportQualityMode.STANDARD -> 1f
        PdfExportQualityMode.ULTRA -> 2f
    }
}

private fun resolvePdfImageLayoutMode(requestedMode: ImageLayoutMode, imageCount: Int): ImageLayoutMode {
    val availableModes = ImageLayoutMode.availableForImageCount(imageCount)
    return if (requestedMode in availableModes) requestedMode else ImageLayoutMode.AUTO
}

private fun buildPdfImageRows(
    imagePaths: List<String>,
    layoutMode: ImageLayoutMode,
    contentWidth: Float,
    gap: Float
): List<PdfImageRow> {
    if (imagePaths.isEmpty()) return emptyList()

    return when (layoutMode) {
        ImageLayoutMode.AUTO -> buildPdfAutoRows(imagePaths, contentWidth, gap)
        ImageLayoutMode.SMALL -> buildPdfSmallRows(imagePaths, contentWidth, gap)
        ImageLayoutMode.TWO_COLUMNS -> buildPdfTwoColumnRows(imagePaths, contentWidth, gap)
        ImageLayoutMode.LARGE_FIRST -> buildPdfFocusRows(imagePaths, contentWidth, gap, focusLast = false)
        ImageLayoutMode.LARGE_LAST -> buildPdfFocusRows(imagePaths, contentWidth, gap, focusLast = true)
    }
}

private fun buildPdfAutoRows(
    imagePaths: List<String>,
    contentWidth: Float,
    gap: Float
): List<PdfImageRow> {
    return when (imagePaths.size) {
        1 -> listOf(
            PdfImageRow(
                height = contentWidth * 0.56f,
                items = listOf(
                    PdfImageItem(
                        path = imagePaths[0],
                        left = 0f,
                        top = 0f,
                        width = contentWidth,
                        height = contentWidth * 0.56f
                    )
                )
            )
        )
        2 -> {
            val cellWidth = (contentWidth - gap) / 2f
            val cellHeight = cellWidth * 0.8f
            listOf(
                PdfImageRow(
                    height = cellHeight,
                    items = imagePaths.mapIndexed { index, path ->
                        PdfImageItem(
                            path = path,
                            left = index * (cellWidth + gap),
                            top = 0f,
                            width = cellWidth,
                            height = cellHeight
                        )
                    }
                )
            )
        }
        3 -> {
            val topWidth = (contentWidth - gap) / 2f
            val topHeight = topWidth * 0.58f
            val bottomHeight = contentWidth * 0.39f
            listOf(
                PdfImageRow(
                    height = topHeight,
                    items = listOf(
                        PdfImageItem(
                            path = imagePaths[0],
                            left = 0f,
                            top = 0f,
                            width = topWidth,
                            height = topHeight
                        ),
                        PdfImageItem(
                            path = imagePaths[1],
                            left = topWidth + gap,
                            top = 0f,
                            width = topWidth,
                            height = topHeight
                        )
                    )
                ),
                PdfImageRow(
                    height = bottomHeight,
                    items = listOf(
                        PdfImageItem(
                            path = imagePaths[2],
                            left = 0f,
                            top = 0f,
                            width = contentWidth,
                            height = bottomHeight
                        )
                    )
                )
            )
        }
        4 -> {
            val fixedHeight = ((contentWidth - gap) / 2f) * 0.62f
            buildPdfGridRows(
                paths = imagePaths,
                columns = 2,
                contentWidth = contentWidth,
                gap = gap,
                fixedHeight = fixedHeight
            )
        }
        else -> buildPdfGridRows(
            paths = imagePaths,
            columns = 3,
            contentWidth = contentWidth,
            gap = gap,
            fixedHeight = null
        )
    }
}

private fun buildPdfSmallRows(
    imagePaths: List<String>,
    contentWidth: Float,
    gap: Float
): List<PdfImageRow> {
    if (imagePaths.size == 1) {
        val smallSize = contentWidth * 0.34f
        return listOf(
            PdfImageRow(
                height = smallSize,
                items = listOf(
                    PdfImageItem(
                        path = imagePaths[0],
                        left = 0f,
                        top = 0f,
                        width = smallSize,
                        height = smallSize
                    )
                )
            )
        )
    }
    return buildPdfGridRows(
        paths = imagePaths,
        columns = 3,
        contentWidth = contentWidth,
        gap = gap,
        fixedHeight = null
    )
}

private fun buildPdfTwoColumnRows(
    imagePaths: List<String>,
    contentWidth: Float,
    gap: Float
): List<PdfImageRow> {
    val columns = if (imagePaths.size == 1) 1 else 2
    val fixedHeight = if (columns == 1) {
        contentWidth * 0.36f
    } else {
        ((contentWidth - gap) / 2f) * 0.73f
    }
    return buildPdfGridRows(
        paths = imagePaths,
        columns = columns,
        contentWidth = contentWidth,
        gap = gap,
        fixedHeight = fixedHeight
    )
}

private fun buildPdfFocusRows(
    imagePaths: List<String>,
    contentWidth: Float,
    gap: Float,
    focusLast: Boolean
): List<PdfImageRow> {
    if (imagePaths.size == 1) {
        val fullHeight = contentWidth * 0.56f
        return listOf(
            PdfImageRow(
                height = fullHeight,
                items = listOf(
                    PdfImageItem(
                        path = imagePaths[0],
                        left = 0f,
                        top = 0f,
                        width = contentWidth,
                        height = fullHeight
                    )
                )
            )
        )
    }

    val focusIndex = if (focusLast) imagePaths.lastIndex else 0
    val focusPath = imagePaths[focusIndex]
    val sidePaths = imagePaths.filterIndexed { index, _ -> index != focusIndex }

    val focusWeight = 1.85f
    val sideWeight = 1f
    val availableWidth = contentWidth - gap
    val focusWidth = availableWidth * (focusWeight / (focusWeight + sideWeight))
    val sideWidth = availableWidth - focusWidth
    val rowHeight = contentWidth * 0.54f

    val sideTopPath = sidePaths.getOrNull(0)
    val sideSecondPath = sidePaths.getOrNull(1)
    val sideItems = mutableListOf<PdfImageItem>()
    if (sideTopPath != null) {
        if (sideSecondPath == null) {
            sideItems += PdfImageItem(
                path = sideTopPath,
                left = 0f,
                top = 0f,
                width = sideWidth,
                height = rowHeight
            )
        } else {
            val sideCellHeight = (rowHeight - gap) / 2f
            sideItems += PdfImageItem(
                path = sideTopPath,
                left = 0f,
                top = 0f,
                width = sideWidth,
                height = sideCellHeight
            )
            sideItems += PdfImageItem(
                path = sideSecondPath,
                left = 0f,
                top = sideCellHeight + gap,
                width = sideWidth,
                height = sideCellHeight
            )
        }
    }

    val topRowItems = mutableListOf<PdfImageItem>()
    if (focusLast) {
        topRowItems += sideItems.map { it.copy(left = it.left) }
        topRowItems += PdfImageItem(
            path = focusPath,
            left = sideWidth + gap,
            top = 0f,
            width = focusWidth,
            height = rowHeight
        )
    } else {
        topRowItems += PdfImageItem(
            path = focusPath,
            left = 0f,
            top = 0f,
            width = focusWidth,
            height = rowHeight
        )
        val sideOffset = focusWidth + gap
        topRowItems += sideItems.map { it.copy(left = it.left + sideOffset) }
    }

    val result = mutableListOf(
        PdfImageRow(
            height = rowHeight,
            items = topRowItems
        )
    )

    val remaining = sidePaths.drop(2)
    if (remaining.isNotEmpty()) {
        result += buildPdfGridRows(
            paths = remaining,
            columns = 3,
            contentWidth = contentWidth,
            gap = gap,
            fixedHeight = null
        )
    }
    return result
}

private fun buildPdfGridRows(
    paths: List<String>,
    columns: Int,
    contentWidth: Float,
    gap: Float,
    fixedHeight: Float?
): List<PdfImageRow> {
    if (paths.isEmpty()) return emptyList()
    val safeColumns = columns.coerceAtLeast(1)
    val cellWidth = if (safeColumns == 1) {
        contentWidth
    } else {
        (contentWidth - (safeColumns - 1) * gap) / safeColumns
    }
    val cellHeight = fixedHeight ?: cellWidth

    return paths.chunked(safeColumns).map { rowPaths ->
        PdfImageRow(
            height = cellHeight,
            items = rowPaths.mapIndexed { index, path ->
                PdfImageItem(
                    path = path,
                    left = index * (cellWidth + gap),
                    top = 0f,
                    width = cellWidth,
                    height = cellHeight
                )
            }
        )
    }
}

private fun splitTextToLines(
    text: String,
    paint: Paint,
    maxWidth: Float
): List<String> {
    val result = mutableListOf<String>()
    val paragraphs = text.split('\n')

    paragraphs.forEach { paragraph ->
        var remaining = paragraph.trimEnd()
        if (remaining.isEmpty()) {
            result += ""
        } else {
            while (remaining.isNotEmpty()) {
                var count = paint.breakText(remaining, true, maxWidth, null).coerceAtLeast(1)
                var line = remaining.substring(0, count)
                if (count < remaining.length) {
                    val lastWhitespace = line.lastIndexOf(' ')
                    if (lastWhitespace > 0) {
                        line = line.substring(0, lastWhitespace)
                        count = line.length
                    }
                }
                result += line
                remaining = remaining.substring(count).trimStart()
            }
        }
    }

    return result
}

private fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text

    var current = text
    while (current.isNotEmpty() && paint.measureText("$current...") > maxWidth) {
        current = current.dropLast(1)
    }
    return if (current.isEmpty()) "..." else "$current..."
}

private fun sanitizeFileName(rawTitle: String): String {
    val trimmed = rawTitle.trim().ifBlank { "Note" }
    val sanitized = trimmed.replace(Regex("[\\\\/:*?\"<>|\\n\\r\\t]"), "_")
    return sanitized.take(60)
}

private fun resolvePdfCardSizeMode(note: Note, paperMode: PdfPaperMode): PdfCardSizeMode {
    return when (paperMode) {
        PdfPaperMode.SMALL -> PdfCardSizeMode.SMALL
        PdfPaperMode.MEDIUM -> PdfCardSizeMode.MEDIUM
        PdfPaperMode.LARGE -> PdfCardSizeMode.LARGE
        PdfPaperMode.AUTO -> {
            val wordCount = estimateTextWordCount(note.content)
            val imageCount = note.imagePaths.size
            when {
                wordCount <= 30 && imageCount <= 4 -> PdfCardSizeMode.SMALL
                wordCount in 31..100 && imageCount in 5..9 -> PdfCardSizeMode.MEDIUM
                else -> PdfCardSizeMode.LARGE
            }
        }
    }
}

private fun estimateTextWordCount(text: String): Int {
    val latinWordCount = Regex("[A-Za-z0-9]+").findAll(text).count()
    val cjkCharacterCount = Regex("[\\p{IsHan}]").findAll(text).count()
    return latinWordCount + cjkCharacterCount
}

private fun formatTime(timestamp: Long): String {
    val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    return formatter.format(Date(timestamp))
}

@Composable
private fun XStyleTheme(
    themeMode: AppThemeMode,
    content: @Composable () -> Unit
) {
    val isDarkMode = resolveIsDarkTheme(themeMode)
    val typography = Typography(
        headlineMedium = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp
        )
    )

    val colorScheme = if (isDarkMode) {
        androidx.compose.material3.darkColorScheme(
            background = Color(0xFF08090A),
            surface = Color(0xFF131518),
            primary = Color(0xFF5CB9FF),
            onPrimary = Color.Black,
            onSurface = Color.White,
            onBackground = Color.White
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            background = Color(0xFFF4F6FA),
            surface = Color(0xFFFFFFFF),
            primary = Color(0xFF1B2A40),
            onPrimary = Color.White,
            onSurface = Color(0xFF151922),
            onBackground = Color(0xFF151922)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

private fun saveThemeMode(prefs: SharedPreferences, mode: AppThemeMode) {
    prefs.edit().putString(KEY_THEME_MODE, mode.storageValue).apply()
}

@Composable
private fun resolveIsDarkTheme(themeMode: AppThemeMode): Boolean {
    return when (themeMode) {
        AppThemeMode.BLACK -> true
        AppThemeMode.WHITE -> false
        AppThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }
}
