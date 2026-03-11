package com.travelnotes.offline

import android.content.ContentValues
import android.content.ClipData
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
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
            XStyleTheme {
                TravelNotesScreen()
            }
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

@Composable
private fun TravelNotesScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var capturedPreviewFile by remember { mutableStateOf<File?>(null) }
    var capturedPreviewUri by remember { mutableStateOf<Uri?>(null) }
    var isConfirmingCapturedPhoto by remember { mutableStateOf(false) }
    var noteForPdfShare by remember { mutableStateOf<Note?>(null) }
    var isSharingPdf by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF030303), Color(0xFF0E0E10), Color(0xFF121416))
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
                    containerColor = Color(0xFFEDF2F8),
                    contentColor = Color.Black
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
                    HeaderSection()
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
                        EmptyStateCard()
                    }
                } else {
                    itemsIndexed(viewModel.notes, key = { _, note -> note.id }) { index, note ->
                        AnimatedNoteItem(
                            index = index,
                            note = note,
                            onOpen = { openedNoteId = note.id }
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
            onShare = { noteForPdfShare = it }
        )

        SharePdfOptionsOverlay(
            note = noteForPdfShare,
            isSharing = isSharingPdf,
            onDismiss = {
                if (!isSharingPdf) {
                    noteForPdfShare = null
                }
            },
            onShare = { note, withWatermark, paperMode ->
                noteForPdfShare = null
                coroutineScope.launch {
                    isSharingPdf = true
                    val pdfUri = withContext(Dispatchers.IO) {
                        buildNotePdfUri(
                            context = context,
                            note = note,
                            includeWatermark = withWatermark,
                            paperMode = paperMode
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
    }
}

@Composable
private fun HeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF111214), Color(0xFF17191C), Color(0xFF0F1012))
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = stringResource(R.string.header_title),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.4.sp
        )
        Text(
            text = stringResource(R.string.header_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9DA4AE),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111316))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            Text(
                text = stringResource(R.string.empty_notes_title),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.empty_notes_hint),
                color = Color(0xFFA6ADBA),
                style = MaterialTheme.typography.bodyMedium
            )
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
    onShare: (Note, Boolean, PdfPaperMode) -> Unit
) {
    val currentNote = note ?: return
    val interactionSource = remember { MutableInteractionSource() }
    var selectedPaperMode by remember(currentNote.id) { mutableStateOf(PdfPaperMode.AUTO) }
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
                                label = { Text(pdfPaperModeLabel(mode)) },
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
                        onClick = { onShare(currentNote, includeWatermark, selectedPaperMode) },
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
    onShare: (Note) -> Unit
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
                            layoutMode = currentNote.imageLayoutMode
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
    onOpen: () -> Unit
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
        NoteListCard(note = note, onOpen = onOpen)
    }
}

@Composable
private fun NoteListCard(note: Note, onOpen: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111316))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = if (note.title.isBlank()) stringResource(R.string.untitled_note) else note.title,
                color = Color.White,
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
                    color = Color(0xFF939BA7),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = note.content,
                    color = Color(0xFFC9CFDA),
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
    layoutMode: ImageLayoutMode
) {
    val visiblePaths = if (maxPreview == Int.MAX_VALUE) imagePaths else imagePaths.take(maxPreview)
    if (visiblePaths.isEmpty()) return

    when (layoutMode) {
        ImageLayoutMode.AUTO -> NoteImagesMosaicAuto(visiblePaths)
        ImageLayoutMode.SMALL -> NoteImagesMosaicSmall(visiblePaths)
        ImageLayoutMode.TWO_COLUMNS -> NoteImagesMosaicTwoColumns(visiblePaths)
        ImageLayoutMode.LARGE_FIRST -> NoteImagesMosaicFocus(visiblePaths, focusLast = false)
        ImageLayoutMode.LARGE_LAST -> NoteImagesMosaicFocus(visiblePaths, focusLast = true)
    }
}

@Composable
private fun NoteImagesMosaicAuto(visiblePaths: List<String>) {
    when (visiblePaths.size) {
        1 -> {
            MosaicImageCell(
                path = visiblePaths[0],
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp)
            )
        }
        2 -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MosaicImageCell(
                    path = visiblePaths[0],
                    modifier = Modifier
                        .weight(1f)
                        .height(128.dp)
                )
                MosaicImageCell(
                    path = visiblePaths[1],
                    modifier = Modifier
                        .weight(1f)
                        .height(128.dp)
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
                        path = visiblePaths[0],
                        modifier = Modifier
                            .weight(1f)
                            .height(94.dp)
                    )
                    MosaicImageCell(
                        path = visiblePaths[1],
                        modifier = Modifier
                            .weight(1f)
                            .height(94.dp)
                    )
                }
                MosaicImageCell(
                    path = visiblePaths[2],
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp)
                )
            }
        }
        4 -> NoteImagesMosaicGrid(visiblePaths, columns = 2, fixedHeight = 98.dp)
        else -> NoteImagesMosaicGrid(visiblePaths, columns = 3, fixedHeight = null)
    }
}

@Composable
private fun NoteImagesMosaicSmall(visiblePaths: List<String>) {
    if (visiblePaths.size == 1) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            MosaicImageCell(
                path = visiblePaths[0],
                modifier = Modifier
                    .size(112.dp)
            )
        }
        return
    }
    NoteImagesMosaicGrid(visiblePaths, columns = 3, fixedHeight = null)
}

@Composable
private fun NoteImagesMosaicTwoColumns(visiblePaths: List<String>) {
    val columns = if (visiblePaths.size == 1) 1 else 2
    NoteImagesMosaicGrid(visiblePaths, columns = columns, fixedHeight = 118.dp)
}

@Composable
private fun NoteImagesMosaicFocus(visiblePaths: List<String>, focusLast: Boolean) {
    if (visiblePaths.size == 1) {
        MosaicImageCell(
            path = visiblePaths[0],
            modifier = Modifier
                .fillMaxWidth()
                .height(188.dp)
        )
        return
    }

    val focusIndex = if (focusLast) visiblePaths.lastIndex else 0
    val focusPath = visiblePaths[focusIndex]
    val sidePaths = visiblePaths.filterIndexed { index, _ -> index != focusIndex }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!focusLast) {
                MosaicImageCell(
                    path = focusPath,
                    modifier = Modifier
                        .weight(1.85f)
                        .height(176.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(176.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val topPath = sidePaths.getOrNull(0)
                val secondPath = sidePaths.getOrNull(1)
                if (topPath != null) {
                    MosaicImageCell(
                        path = topPath,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(if (secondPath == null) 1f else 0.5f)
                    )
                }
                if (secondPath != null) {
                    MosaicImageCell(
                        path = secondPath,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.5f)
                    )
                }
                if (topPath == null) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            if (focusLast) {
                MosaicImageCell(
                    path = focusPath,
                    modifier = Modifier
                        .weight(1.85f)
                        .height(176.dp)
                )
            }
        }

        val remaining = sidePaths.drop(2)
        if (remaining.isNotEmpty()) {
            NoteImagesMosaicGrid(remaining, columns = 3, fixedHeight = null)
        }
    }
}

@Composable
private fun NoteImagesMosaicGrid(
    paths: List<String>,
    columns: Int,
    fixedHeight: androidx.compose.ui.unit.Dp?
) {
    if (paths.isEmpty()) return

    val rows = paths.chunked(columns)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { rowPaths ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowPaths.forEach { path ->
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
                        path = path,
                        modifier = cellModifier
                    )
                }
                repeat(columns - rowPaths.size) {
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
private fun MosaicImageCell(path: String, modifier: Modifier) {
    val file = File(path)
    Box(
        modifier = modifier
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
    paperMode: PdfPaperMode
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
    val pageWidth = metrics.pageWidth
    val pageHeight = metrics.pageHeight
    val cardPaddingX = metrics.cardPaddingX
    val cardPaddingTop = metrics.cardPaddingTop
    val footerAreaHeight = metrics.footerAreaHeight
    val scale = metrics.scale

    val cardRect = RectF(
        metrics.cardInsetX,
        metrics.cardInsetTop,
        pageWidth - metrics.cardInsetX,
        pageHeight - metrics.cardInsetBottom
    )
    val contentLeft = cardRect.left + cardPaddingX
    val contentRight = cardRect.right - cardPaddingX
    val contentWidth = contentRight - contentLeft

    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    }
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#E6E9EE")
        strokeWidth = 2f
    }
    val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#111318")
        textSize = 44f * scale
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val headerMetaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#8F97A5")
        textSize = 24f * scale
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#171A1F")
        textSize = 30f * scale
    }
    val chipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#135CAB")
        textSize = 24f * scale
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val chipBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#EAF3FF")
    }
    val imagePlaceholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#E8EBF0")
    }
    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#8A92A0")
        textSize = 22f * scale
    }
    val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#97A0AF")
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
    val pdfImagePaths = when (resolvedCardSize) {
        PdfCardSizeMode.LARGE -> note.imagePaths
        PdfCardSizeMode.SMALL, PdfCardSizeMode.MEDIUM -> note.imagePaths.take(9)
    }

    fun startPage() {
        pageNumber += 1
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        page = document.startPage(pageInfo)
        canvas = page.canvas
        canvas.drawColor(android.graphics.Color.parseColor("#F3F5F8"))
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
                val dateY = titleY + (headerMetaPaint.fontSpacing * 0.9f)
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

        y = headerBottom + (26f * scale)
        contentBottomLimit = footerTop - (18f * scale)
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

            imageRows.forEach { row ->
                ensureSpace(row.height + metrics.imageGap)
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
                        targetWidth = item.width.toInt().coerceAtLeast(1),
                        targetHeight = item.height.toInt().coerceAtLeast(1)
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
                y += row.height + metrics.imageGap
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

private fun decodeBitmapForPdf(path: String, targetWidth: Int, targetHeight: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val requestedWidth = (targetWidth.coerceAtLeast(1) * 3).coerceAtMost(4096)
    val requestedHeight = (targetHeight.coerceAtLeast(1) * 3).coerceAtMost(4096)

    var sampleSize = 1
    while (
        bounds.outWidth / sampleSize > requestedWidth ||
        bounds.outHeight / sampleSize > requestedHeight
    ) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeFile(path, options)
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
            cardInsetTop = 250f,
            cardInsetBottom = 230f,
            cardPaddingX = 34f,
            cardPaddingTop = 30f,
            footerAreaHeight = 68f,
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
private fun XStyleTheme(content: @Composable () -> Unit) {
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

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = Color(0xFF08090A),
            surface = Color(0xFF131518),
            primary = Color(0xFF5CB9FF),
            onPrimary = Color.Black,
            onSurface = Color.White,
            onBackground = Color.White
        ),
        typography = typography,
        content = content
    )
}
