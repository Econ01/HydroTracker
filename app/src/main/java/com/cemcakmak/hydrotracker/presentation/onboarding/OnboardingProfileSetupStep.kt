package com.cemcakmak.hydrotracker.presentation.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.utils.ImageUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupStep(
    name: String,
    profileImageUri: Uri? = null,
    onNameChanged: (String) -> Unit,
    onImageSelected: (Uri?) -> Unit,
    onNavigateToCrop: (Uri) -> Unit,
    title: String,
    description: String
) {
    val context = LocalContext.current
    val bottomSheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Pass the temporary file URI to the cropper.
            val tempFile = File(context.cacheDir, "temp_profile_photo.jpg")
            if (tempFile.exists()) {
                onNavigateToCrop(Uri.fromFile(tempFile))
            }
        }
        showBottomSheet = false
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch camera immediately
            val photoFile = File(context.cacheDir, "temp_profile_photo.jpg")
            val photoUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraLauncher.launch(photoUri)
        }
    }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            // Pass the original URI to the cropper.
            onNavigateToCrop(selectedUri)
        }
        showBottomSheet = false
    }

    OnboardingStepLayout(
        title = title,
        description = description
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile Photo Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Photo Display
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val profileBitmap = remember(profileImageUri) {
                        profileImageUri?.let { uri ->
                            try {
                                if (uri.scheme == "file") {
                                    val file = File(uri.path!!)
                                    if (file.exists()) {
                                        android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                    } else null
                                } else {
                                    ImageUtils.loadProfileImageBitmap(context)
                                }
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                    
                    Surface(
                        modifier = Modifier
                            .size(120.dp)
                            .clickable { showBottomSheet = true },
                        shape = CircleShape,
                        color = if (profileBitmap != null) Color.Transparent else MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileBitmap != null) {
                                Image(
                                    bitmap = profileBitmap.asImageBitmap(),
                                    contentDescription = stringResource(R.string.cd_profile_photo),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = stringResource(R.string.profile_add_photo),
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = stringResource(R.string.profile_add_photo),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (profileImageUri != null) {
                    TextButton(
                        onClick = { 
                            ImageUtils.deleteProfileImage(context)
                            onImageSelected(null) 
                        }
                    ) {
                        Text(stringResource(R.string.profile_remove_photo))
                    }
                }
            }
            
            // Name Input Section
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.profile_name_prompt),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { newName ->
                        if (newName.length <= 15) {
                            onNameChanged(newName)
                        }
                    },
                    label = { Text(stringResource(R.string.profile_name_label)) },
                    placeholder = { Text(stringResource(R.string.profile_name_placeholder)) },
                    supportingText = { 
                        Text(stringResource(R.string.character_count, name.length, 15))
                    },
                    isError = name.isBlank(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                if (name.isBlank()) {
                    Text(
                        text = stringResource(R.string.profile_name_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
    
    // Bottom Sheet for photo selection
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.profile_select_photo),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Gallery option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.profile_choose_gallery),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.profile_choose_gallery_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Camera option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Check camera permission
                            when (PackageManager.PERMISSION_GRANTED) {
                                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                                    // Create a temporary file for the camera image
                                    val photoFile = File(context.cacheDir, "temp_profile_photo.jpg")
                                    val photoUri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        photoFile
                                    )
                                    cameraLauncher.launch(photoUri)
                                }
                                else -> {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.profile_take_photo),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.profile_take_photo_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview
@Composable
fun ProfileSetupStepPreview() {
    var name by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    ProfileSetupStep(
        name = name,
        profileImageUri = imageUri,
        onNameChanged = { name = it },
        onImageSelected = { imageUri = it },
        onNavigateToCrop = { imageUri = it },
        title = "Complete Your Profile",
        description = "Add your name and a profile photo to personalize your experience."
    )
}