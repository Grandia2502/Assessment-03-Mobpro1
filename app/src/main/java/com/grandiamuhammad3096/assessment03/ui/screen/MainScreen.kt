@file:Suppress("DEPRECATION")

package com.grandiamuhammad3096.assessment03.ui.screen

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.grandiamuhammad3096.assessment03.BuildConfig
import com.grandiamuhammad3096.assessment03.R
import com.grandiamuhammad3096.assessment03.model.Photo
import com.grandiamuhammad3096.assessment03.model.User
import com.grandiamuhammad3096.assessment03.network.ApiStatus
import com.grandiamuhammad3096.assessment03.network.CropTarget
import com.grandiamuhammad3096.assessment03.network.UserDataStore
import com.grandiamuhammad3096.assessment03.ui.crop.MyCropperActivity
import com.grandiamuhammad3096.assessment03.ui.theme.Assessment03Theme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val dataStore = UserDataStore(context)
    val user by dataStore.userFlow.collectAsState(User())

    val vm: MainViewModel = viewModel()
    val errorMessage by vm.errorMessage.collectAsState(null)
    
    var showProfile by remember { mutableStateOf(false) }

    var showConfirmDelete by remember { mutableStateOf(false) }
    var idToDelete by remember { mutableStateOf<String?>(null) }

    // Create dialog state
    var showCreateDialog by remember { mutableStateOf(false) }
    var createBitmap: Bitmap? by remember { mutableStateOf(null) }

    // Edit dialog state
    var editing: Photo? by remember { mutableStateOf(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editBitmap: Bitmap? by remember { mutableStateOf(null) }

    // Cropper (gunakan untuk create & edit)
    var cropTarget by remember { mutableStateOf(CropTarget.CREATE) }

    val cropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            val bmp = decodeUriToBitmap(context.contentResolver, uri)
            when (cropTarget) {
                CropTarget.CREATE -> { createBitmap = bmp; showCreateDialog = true }
                CropTarget.EDIT   -> { editBitmap = bmp; showEditDialog = true }
            }
        }
    }

    val scope = rememberCoroutineScope()

    val legacyGmsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(res.data)
                val account = task.getResult(ApiException::class.java)
                val email = account?.email           // ⬅️ pakai email saja
                val name  = account?.displayName
                val photo = account?.photoUrl?.toString()

                Log.d("SIGN-IN", "Legacy OK (email only). email=$email")

                if (!email.isNullOrBlank()) {
                    scope.launch {
                        dataStore.saveData(User(name ?: "", email, photo ?: ""))
                        vm.syncPending(email)
                        vm.retrieveData(email)
                        Toast.makeText(context, "Halo $email", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Gagal membaca email Google", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                // 12501 = SIGN_IN_CANCELLED, 12500 = SIGN_IN_FAILED, 7 = NETWORK_ERROR, 10 = DEVELOPER_ERROR
                Log.e("SIGN-IN", "Legacy ApiException: ${e.statusCode}", e)
                Toast.makeText(context, "Login gagal (${e.statusCode})", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.d("SIGN-IN", "Legacy canceled or no data. code=${res.resultCode}")
            Toast.makeText(context, "Login dibatalkan", Toast.LENGTH_SHORT).show()
        }
    }


    Scaffold (
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.app_name))
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                actions = {
//                    val fallbackLauncher = rememberLauncherForActivityResult(
//                        ActivityResultContracts.StartIntentSenderForResult()
//                    ) { res ->
//                        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
//                            val signInClient = Identity.getSignInClient(context)
//                            try {
//                                val cred = signInClient.getSignInCredentialFromIntent(res.data)
//                                // 1) Utamakan ID Token → lebih konsisten
//                                val idToken = cred.googleIdToken
//                                val emailFromToken = idToken?.let { extractEmailFromIdToken(it) }
//
//                                // 2) Cadangan: cred.id (sering berisi email, tapi tidak dijamin)
//                                val email = emailFromToken ?: cred.id
//                                val name = cred.displayName
//                                val photo = cred.profilePictureUri?.toString()
//
//                                Log.d("SIGN-IN", "Fallback OK. token?=${idToken != null} email=$email name=$name")
//
//                                if (email.isBlank()) {
//                                    Toast.makeText(context, "Tidak bisa membaca email dari Google", Toast.LENGTH_LONG).show()
//                                    return@rememberLauncherForActivityResult
//                                }
//
//                                scope.launch {
//                                    dataStore.saveData(User(name ?: "", email, photo ?: ""))
//                                    vm.syncPending(email)
//                                    vm.retrieveData(email)
//                                    Toast.makeText(context, "Halo $email", Toast.LENGTH_SHORT).show()
//                                }
//                            } catch (e: ApiException) {
//                                Log.e("SIGN-IN", "Fallback ApiException: ${e.statusCode}", e)
//                                Toast.makeText(context, "Login gagal (${e.statusCode})", Toast.LENGTH_LONG).show()
//                            } catch (t: Throwable) {
//                                Log.e("SIGN-IN", "Fallback error", t)
//                                Toast.makeText(context, "Login gagal: ${t.message}", Toast.LENGTH_LONG).show()
//                            }
//                        } else {
//                            Log.d("SIGN-IN", "Fallback canceled or no data. code=${res.resultCode}")
//                            Toast.makeText(context, "Login dibatalkan", Toast.LENGTH_SHORT).show()
//                        }
//                    }

                    IconButton(onClick = {
                        Log.d("SIGNIN", "WEB_CLIENT_ID=" + BuildConfig.GOOGLE_WEB_CLIENT_ID)
                        if (user.email.isEmpty()) {
//                            scope.launch {
//                                signIn(
//                                    context = context,
//                                    dataStore = dataStore,
//                                    fallbackLauncher = fallbackLauncher,
//                                    onAfterLogin = { email ->
//                                        vm.syncPending(email)
//                                        vm.retrieveData(email)
//                                    }
//                                )
//                            }
                            legacySignIn(context, legacyGmsLauncher)
                        } else {
                            showProfile = true
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.account_circle_24),
                            contentDescription = stringResource(R.string.profil),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                cropTarget = CropTarget.CREATE
                cropLauncher.launch(Intent(context, MyCropperActivity::class.java))
            }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.tambah_foto)
                )
            }
        }
    ) { innerPadding ->
        ScreenContent(
            vm,
            userEmail = user.email,
            Modifier.padding(innerPadding),
            onEditRequest = { photo ->
                editing = photo
                editBitmap = null // awalnya pakai gambar lama dari server
                showEditDialog = true
            },
            onDeleteRequest = { photoId ->
                idToDelete = photoId
                showConfirmDelete = true
            },
            onRetry = {
                if (user.email.isNotBlank()) vm.retrieveData(user.email) }
        )

        if (showProfile) {
            ProfileDialog(
                user = user,
                onDismissRequest = { showProfile = false }
            ) {
                scope.launch { legacySignOut(context, onDone = dataStore) }
                showProfile = false
            }
        }

        if (showCreateDialog) {
            PhotoDialog(
                bitmap = createBitmap,
                imageUrl = null,
                initialTitle = "",
                initialDescription = "",
                onChangePhoto = {
                    cropTarget = CropTarget.CREATE
                    cropLauncher.launch(Intent(context, MyCropperActivity::class.java))
                },
                onDismissRequest = {
                    showCreateDialog = false
                    createBitmap = null
                },
                onConfirmation = { title, desc ->
                    if (createBitmap != null) {
                        vm.createLocalPhoto(title, desc, createBitmap!!)
                        showCreateDialog = false
                        createBitmap = null
                        if (user.email.isNotBlank()) vm.syncPending(user.email)
                    } else {
                        Toast.makeText(context, "Pilih foto terlebih dahulu", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Dialog EDIT
        if (showEditDialog && editing != null) {
            PhotoDialog(
                bitmap = editBitmap, // jika null, gunakan imageUrl lama
                imageUrl = editing!!.imageUrl,
                initialTitle = editing!!.title,
                initialDescription = editing!!.description ?: "",
                onChangePhoto = {
                    cropTarget = CropTarget.EDIT
                    cropLauncher.launch(Intent(context, MyCropperActivity::class.java))
                },
                onDismissRequest = {
                    showEditDialog = false
                    editBitmap = null
                    editing = null
                },
                onConfirmation = { title, description ->
                    vm.editLocalPhoto(
                        localId = editing!!.id,  // id sekarang = localId
                        title = title,
                        description = description,
                        bitmap = editBitmap
                    )
                    showEditDialog = false
                    editBitmap = null
                    editing = null
                }
            )
        }

        // Dialog konfirmasi sebelum benar-benar hapus
        if (showConfirmDelete && idToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showConfirmDelete = false
                    idToDelete = null
                },
                title = { Text(stringResource(id = R.string.konfirmasi_hapus)) },
                text = { Text(stringResource(id = R.string.pertanyaan_hapus)) },
                confirmButton = {
                    TextButton(onClick = {
                        // Panggil ViewModel.deleteData
                        vm.deleteLocal(idToDelete!!)
                        showConfirmDelete = false
                        idToDelete = null
                    }) {
                        Text(stringResource(id = R.string.hapus))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showConfirmDelete = false
                        idToDelete = null
                    }) {
                        Text(stringResource(id = R.string.batal))
                    }
                }
            )
        }

        errorMessage?.let {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            vm.clearMessage()
        }
    }
}

@Composable
fun ScreenContent(
    vm: MainViewModel,
    userEmail: String,
    modifier: Modifier = Modifier,
    onEditRequest: (Photo) -> Unit,
    onDeleteRequest: (String) -> Unit,
    onRetry: () -> Unit
) {
    val data by vm.photos.collectAsState(emptyList())
    val status by vm.status.collectAsState()

    LaunchedEffect(userEmail) {
        if (userEmail.isNotBlank()) {
            vm.retrieveData(userEmail)
            vm.syncPending(userEmail)
        } else {
            vm.useCacheOnly()
        }
    }

    when (status) {
        ApiStatus.LOADING -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        ApiStatus.SUCCESS -> {
            if (data.isEmpty()) {
                // Empty state
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_photos_galeri),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(data) { photo ->
                        ListItem(
                            photo = photo,
                            onEdit = { onEditRequest(photo) },
                            onDelete = onDeleteRequest
                        )
                    }
                }
            }
        }

        ApiStatus.FAILED -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(id = R.string.error))
                Button(
                    onClick = { onRetry() },
                    modifier = Modifier.padding(top = 16.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    Text(text = stringResource(id = R.string.try_again))
                }
            }
        }
    }
}

private fun legacySignIn(context: Context, launcher: ActivityResultLauncher<Intent>) {
    // Tidak minta ID token → tidak perlu Web Client ID → minim gesekan
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()       // ⬅️ cukup ini
        .build()
    val client = GoogleSignIn.getClient(context, gso)

    // (opsional) bersihkan state lama supaya intent segar
    client.signOut().addOnCompleteListener {
        launcher.launch(client.signInIntent)
    }
}

private fun legacySignOut(context: Context, onDone: UserDataStore) {
    val client = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
    )
    client.signOut().addOnCompleteListener { onDone }
}

//// Helper menemukan Activity dari Context (agar tidak pakai Application context)
//private tailrec fun Context.asActivity(): Activity? = when (this) {
//    is Activity -> this
//    is ContextWrapper -> baseContext.asActivity()
//    else -> null
//}
//
//private suspend fun signIn(
//    context: Context,
//    dataStore: UserDataStore,
//    fallbackLauncher: ActivityResultLauncher<IntentSenderRequest>,
//    onAfterLogin: (email: String) -> Unit = {} // mis. vm.syncPending + vm.retrieveData
//) {
//    val activity = context.asActivity()
//    if (activity == null) {
//        withContext(Dispatchers.Main) {
//            Toast.makeText(
//                context, "Gagal login: context bukan Activity", Toast.LENGTH_LONG
//            ).show()
//        }
//        return
//    }
//
//    val cm = CredentialManager.create(activity)
//
//    val googleIdOption = GetGoogleIdOption.Builder()
//        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
//        .setFilterByAuthorizedAccounts(false)
//        .build()
//
//    val request = GetCredentialRequest.Builder()
//        .addCredentialOption(googleIdOption)
//        .build()
//
//    try {
//        val result = withContext(Dispatchers.Main) {
//            cm.getCredential(activity, request)
//        }
//        val email = handleSignIn(activity, result, dataStore)
//        if (!email.isNullOrBlank()) onAfterLogin(email)
//    } catch (e: NoCredentialException) {
//        val signInClient = Identity.getSignInClient(activity)
//        val intentReq = GetSignInIntentRequest.builder()
//            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
//            .build()
//
//        val pendingIntent = withContext(Dispatchers.IO) {
//            signInClient.getSignInIntent(intentReq).await()
//        }
//        withContext(Dispatchers.Main) {
//            Log.d("SIGN-IN", "Fallback PendingIntent OK (await)")
//            fallbackLauncher.launch(
//                IntentSenderRequest.Builder(pendingIntent).build()
//            )
//        }
//    } catch (e: GetCredentialException) {
//        Log.e("SIGN-IN", "CM Error: ${e.errorMessage}")
//        withContext(Dispatchers.Main) {
//            Toast.makeText(
//                context, "Gagal login: ${e.message}", Toast.LENGTH_LONG
//            ).show()
//        }
//    } catch (t: Throwable) {
//        Log.e("SIGN-IN", "Unexpected:", t)
//        withContext(Dispatchers.Main) {
//            Toast.makeText(
//                context, "Kesalahan tak terduga: ${t.message}", Toast.LENGTH_LONG
//            ).show()
//        }
//    }
//}
//
//private suspend fun handleSignIn(
//    context: Context,
//    result: GetCredentialResponse,
//    dataStore: UserDataStore
//): String? {
//    val credential = result.credential
//    if (credential is CustomCredential &&
//        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
//        return try {
//            val googleId = GoogleIdTokenCredential.createFrom(credential.data)
//            val idToken = googleId.idToken
//            val payload = idToken.split(".")[1]
//            val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP))
//            val email = JSONObject(json).optString("email", "")
//            val nama = googleId.displayName?: ""
//            val photoUrl = googleId.profilePictureUri?.toString() ?:""
//
//            dataStore.saveData(User(nama, email, photoUrl))
//            Log.d("SIGN-IN", "Login OK: $email")
//            email
//        } catch (e: GoogleIdTokenParsingException) {
//            Log.e("SIGN-IN", "Error: ${e.message}")
//            withContext(Dispatchers.Main) {
//                Toast.makeText(context, "Gagal parsing Google token", Toast.LENGTH_LONG).show()
//            }
//            null
//        }
//    } else {
//        Log.e("SIGN-IN", "Error: unrecognized custom credential type: ${credential.type}")
//        return null
//    }
//}
//
//private suspend fun signOut(context: Context, dataStore: UserDataStore) {
//    try {
//        val credentialManager = CredentialManager.create(context)
//        credentialManager.clearCredentialState(
//            ClearCredentialStateRequest()
//        )
//        dataStore.saveData(User())
//    } catch (e: ClearCredentialException) {
//        Log.e("SIGN-IN", "Error: ${e.errorMessage}")
//    }
//}
//
//private fun extractEmailFromIdToken(idToken: String): String? {
//    return try {
//        val parts = idToken.split(".")
//        if (parts.size < 2) return null
//        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
//        JSONObject(payload).optString("email", null.toString())
//    } catch (_: Throwable) { null }
//}

private fun decodeUriToBitmap(
    resolver: ContentResolver,
    uri: Uri
): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val src = ImageDecoder.createSource(resolver, uri)
        ImageDecoder.decodeBitmap(src)
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(resolver, uri)
    }
}

@Composable
fun ListItem(
    photo: Photo,
    onEdit: () -> Unit,
    onDelete: (String) -> Unit) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .border(1.dp, Color.Gray),
        contentAlignment = Alignment.BottomEnd
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.gambar, photo.title),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.loading_img),
            error = painterResource(id = R.drawable.broken_img),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .background(Color(red = 0f, green = 0f, blue = 0f, alpha = 0.5f))
                .padding(4.dp)
        ) {
            Text(text = photo.title,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            photo.description?.let {
                Text(text = it,
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    color = Color.White)
            }
        }
        // Tombol Edit & Delete di pojok kanan bawah
        Row(modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(8.dp)) {
            IconButton(onClick = onEdit,
                modifier = Modifier
                    .width(28.dp)
                    .height(28.dp)) {
                Icon(imageVector = Icons.Default.Create,
                    contentDescription = stringResource(id = R.string.edit_foto),
                    tint = Color.White)
            }
            IconButton(onClick = { onDelete(photo.id) },
                modifier = Modifier
                    .width(28.dp)
                    .height(28.dp)) {
                Icon(imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(id = R.string.hapus_foto),
                    tint = Color.White)
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun MainScreenPreview() {
    Assessment03Theme {
        MainScreen()
    }
}