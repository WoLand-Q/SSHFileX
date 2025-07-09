// file: app/src/main/java/ua/ernest/sshfilex/ui/screens/RemoteBrowserScreen.kt
package ua.ernest.sshfilex.ui.screens
import androidx.compose.runtime.rememberCoroutineScope
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.launch
import ua.ernest.sshfilex.data.RemoteFile
import ua.ernest.sshfilex.data.SshRepository
import ua.ernest.sshfilex.util.humanReadable
import java.io.File
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteBrowserScreen(
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {}
) {
    val repo = remember { SshRepository }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var currentPath by rememberSaveable { mutableStateOf(".") }
    var files by remember { mutableStateOf<List<RemoteFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var folderUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    // ловим системный Back, чтобы «влезть» наверх
    BackHandler(enabled = currentPath != ".") {
        currentPath = currentPath.substringBeforeLast('/', ".")
    }

    // pick для скачивания (выбор папки сохранения)
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        folderUri = uri
    }

    // pick для загрузки (выбор локального файла)
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { picked ->
            scope.launch {
                context.contentResolver.openInputStream(picked)?.use { input ->
                    val fileName = DocumentFile.fromSingleUri(context, picked)
                        ?.name
                        ?: picked.lastPathSegment
                        ?: "upload.dat"
                    val remotePath = if (currentPath == ".") fileName else "$currentPath/$fileName"

                    isLoading = true
                    repo.upload(remotePath, input)
                        .onSuccess {
                            snackbar.showSnackbar("Загружено: $fileName")
                            // перечитаем текущую папку
                            files = repo.list(currentPath).getOrThrow()
                        }
                        .onFailure {
                            snackbar.showSnackbar("Ошибка загрузки: ${it.message}")
                        }
                    isLoading = false
                } ?: snackbar.showSnackbar("Не удалось открыть файл")
            }
        }
    }

    // при смене пути — читаем каталог
    LaunchedEffect(currentPath) {
        isLoading = true
        files = runCatching { repo.list(currentPath).getOrThrow() }
            .getOrElse {
                snackbar.showSnackbar(it.message ?: "Ошибка загрузки списка")
                emptyList()
            }
        isLoading = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (currentPath == ".") "Home"
                        else currentPath.substringAfterLast('/')
                    )
                },
                navigationIcon = {
                    if (currentPath != ".") {
                        IconButton(onClick = { currentPath = currentPath.substringBeforeLast('/', ".") }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Up")
                        }
                    } else {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    // загрузка на хост
                    IconButton(onClick = {
                        // разрешаем любые типы файлов
                        filePicker.launch(arrayOf("*/*"))
                    }) {
                        Icon(Icons.Default.Upload, contentDescription = "Upload")
                    }
                    // скачивание
                    IconButton(onClick = { folderPicker.launch(null) }) {
                        Icon(Icons.Default.Download, contentDescription = "Select folder")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            if (files.isEmpty() && !isLoading) {
                Text("Папка пуста", Modifier.align(Alignment.Center))
            }
            LazyColumn(contentPadding = PaddingValues(8.dp)) {
                items(files, key = { it.name }) { rf ->
                    RemoteFileListItem(
                        rf = rf,
                        onDirClick = { name ->
                            currentPath =
                                if (currentPath == ".") name
                                else "$currentPath/$name"
                        },
                        onDownloadClick = {
                            scope.launch {
                                val remotePath =
                                    if (currentPath == ".") rf.name
                                    else "$currentPath/${rf.name}"
                                runCatching {
                                    if (folderUri != null && !rf.isDir) {
                                        val tree = DocumentFile.fromTreeUri(context, folderUri!!)
                                            ?: error("Не удаётся получить папку")
                                        val doc = tree.createFile(
                                            "application/octet-stream", rf.name
                                        ) ?: error("Не создать файл ${rf.name}")
                                        context.contentResolver.openOutputStream(doc.uri)
                                            ?.use { os -> repo.download(remotePath, os).getOrThrow() }
                                            ?: error("Не открыть поток")
                                    } else if (!rf.isDir) {
                                        val dlDir = context
                                            .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                                            ?: error("Нет доступа к Downloads")
                                        val dst = File(dlDir, rf.name)
                                        repo.download(remotePath, dst).getOrThrow()
                                    }
                                }
                                    .onSuccess {
                                        snackbar.showSnackbar("Скачано: ${rf.name}")
                                    }
                                    .onFailure {
                                        snackbar.showSnackbar(it.message ?: "Ошибка скачивания")
                                    }
                            }
                        }
                    )
                }
            }
            if (isLoading) {
                CircularProgressIndicator(
                    Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteFileListItem(
    rf: RemoteFile,
    onDirClick: (String) -> Unit,
    onDownloadClick: () -> Unit
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = if (rf.isDir) Icons.Default.Folder
                else Icons.Default.InsertDriveFile,
                contentDescription = null
            )
        },
        headlineContent = { Text(rf.name) },
        supportingContent = {
            if (!rf.isDir) Text(rf.size.humanReadable())
        },
        trailingContent = {
            if (!rf.isDir) {
                IconButton(onClick = onDownloadClick) {
                    Icon(Icons.Default.Download, contentDescription = "Download")
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = rf.isDir) { if (rf.isDir) onDirClick(rf.name) }
            .padding(8.dp)
    )
}
