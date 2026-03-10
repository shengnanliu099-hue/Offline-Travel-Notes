package com.travelnotes.offline

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.travelnotes.offline.data.Note
import com.travelnotes.offline.ui.DraftImage
import com.travelnotes.offline.ui.MainViewModel
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

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

@Composable
private fun TravelNotesScreen(viewModel: MainViewModel = viewModel()) {
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MainViewModel.MAX_IMAGES_PER_NOTE)
    ) { uris: List<Uri> ->
        viewModel.addPickedImages(uris)
    }

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
            containerColor = Color.Transparent
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
                    EditorCard(
                        isEditing = viewModel.isEditing,
                        title = viewModel.titleInput,
                        content = viewModel.contentInput,
                        images = viewModel.draftImages,
                        onTitleChange = viewModel::onTitleChange,
                        onContentChange = viewModel::onContentChange,
                        onPickImages = {
                            pickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRemoveImage = viewModel::removeDraftImage,
                        onMoveImage = viewModel::moveDraftImage,
                        onSave = viewModel::saveNote,
                        onCancelEdit = viewModel::cancelEdit
                    )
                }

                item {
                    HorizontalDivider(color = Color(0xFF2B2C30))
                }

                itemsIndexed(viewModel.notes, key = { _, note -> note.id }) { index, note ->
                    AnimatedNoteItem(
                        index = index,
                        note = note,
                        onEdit = { viewModel.startEdit(note) },
                        onDelete = { viewModel.deleteNote(note) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
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
private fun EditorCard(
    isEditing: Boolean,
    title: String,
    content: String,
    images: List<DraftImage>,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onPickImages: () -> Unit,
    onRemoveImage: (DraftImage) -> Unit,
    onMoveImage: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141518))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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

            AnimatedVisibility(
                visible = isEditing,
                enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 4 },
                exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 4 }
            ) {
                Text(
                    text = stringResource(R.string.editor_editing_notice),
                    color = Color(0xFF4DB6FF),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.label_title)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                label = { Text(stringResource(R.string.label_content_with_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = RoundedCornerShape(14.dp)
            )

            if (images.isNotEmpty()) {
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(visible = isEditing) {
                        TextButton(onClick = onCancelEdit) {
                            Text(stringResource(R.string.btn_cancel), color = Color(0xFFB8C0CC))
                        }
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
}

@Composable
private fun DraftImagesRow(
    images: List<DraftImage>,
    onRemoveImage: (DraftImage) -> Unit,
    onMoveImage: (Int, Int) -> Unit
) {
    val density = LocalDensity.current
    val reorderStepPx = with(density) { 44.dp.toPx() }
    var dragDistanceX by remember { mutableFloatStateOf(0f) }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(items = images, key = { _, image -> image.id }) { index, image ->
            Box(
                modifier = Modifier
                    .size(98.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1A1D20))
                    .pointerInput(index, images.size, image.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                dragDistanceX = 0f
                            },
                            onDragEnd = {
                                dragDistanceX = 0f
                            },
                            onDragCancel = {
                                dragDistanceX = 0f
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            dragDistanceX += dragAmount.x

                            if (dragDistanceX >= reorderStepPx && index < images.lastIndex) {
                                onMoveImage(index, index + 1)
                                dragDistanceX = 0f
                            } else if (dragDistanceX <= -reorderStepPx && index > 0) {
                                onMoveImage(index, index - 1)
                                dragDistanceX = 0f
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
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
        NoteCard(note = note, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
private fun NoteCard(note: Note, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111316))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (note.title.isBlank()) stringResource(R.string.untitled_note) else note.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTime(note.updatedAt),
                    color = Color(0xFF939BA7),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note.content,
                    color = Color(0xFFE5E8EE),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (note.imagePaths.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(note.imagePaths) { path ->
                        val file = File(path)
                        if (file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = stringResource(R.string.cd_note_photo),
                                modifier = Modifier
                                    .size(width = 140.dp, height = 96.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C1F24)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            if (note.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = note.tags.joinToString(separator = "  ") { "#$it" },
                    color = Color(0xFF5CB9FF),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.cd_edit),
                        tint = Color(0xFFCFD5DF)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_edit), color = Color(0xFFCFD5DF))
                }
                TextButton(onClick = onDelete) {
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

private fun imageModel(image: DraftImage): Any {
    return when (image) {
        is DraftImage.Existing -> File(image.path)
        is DraftImage.New -> image.uri
    }
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
