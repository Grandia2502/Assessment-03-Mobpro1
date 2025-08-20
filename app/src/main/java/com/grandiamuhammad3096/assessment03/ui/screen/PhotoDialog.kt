package com.grandiamuhammad3096.assessment03.ui.screen

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.grandiamuhammad3096.assessment03.R
import com.grandiamuhammad3096.assessment03.ui.theme.Assessment03Theme

@Composable
fun PhotoDialog(
    bitmap: Bitmap?,
    imageUrl: String?,
    initialTitle: String,
    initialDescription: String,
    onChangePhoto: () -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmation: (String, String) -> Unit,
    requirePhoto: Boolean = false,
    onRemoveTempPhoto: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(initialTitle) }
    var desc by remember { mutableStateOf(initialDescription) }

    // Apakah tombol Simpan boleh aktif?
    val hasAnyPhoto = (bitmap != null) || (!imageUrl.isNullOrBlank())
    val canSave = title.isNotBlank() && (!requirePhoto || hasAnyPhoto)

    // Label dinamis
    val changeLabel = if (!hasAnyPhoto) {
        stringResource(R.string.pilih_foto)
    } else stringResource(R.string.ganti_foto)

    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                } else if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.loading_img),
                        error = painterResource(id = R.drawable.broken_img),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                } else {
                    // Placeholder kosong
                    AsyncImage(
                        model = null,
                        contentDescription = null,
                        placeholder = painterResource(id = R.drawable.loading_img),
                        error = painterResource(id = R.drawable.broken_img),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(text = stringResource(id = R.string.judul)) },
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(text = stringResource(id = R.string.deskripsi)) },
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row (
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { onChangePhoto() },
                            modifier = Modifier
                                .padding(2.dp),
                        ) {
                            Text(text = changeLabel)
                        }
                        OutlinedButton(
                            onClick = { onDismissRequest() },
                            modifier = Modifier
                                    .weight(1f)
                                    .padding(8.dp),
                            contentPadding = PaddingValues(horizontal = 32.dp)
                        ) {
                            Text(text = stringResource(R.string.batal))
                        }
                    }
                    Row (
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (onRemoveTempPhoto != null && bitmap != null) {
                            OutlinedButton(onClick = onRemoveTempPhoto,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(text = stringResource(R.string.hapus_foto))
                            }
                        }
                        OutlinedButton(
                            onClick = { onConfirmation(title.trim(), desc.trim()) },
                            enabled = canSave,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(text = stringResource(R.string.simpan))
                        }

                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun AddDialogPreview() {
    Assessment03Theme {
        PhotoDialog(
            bitmap = null,
            onDismissRequest = {},
            onConfirmation = { _, _ -> },
            imageUrl = "http://example.com/img/jpg",
            initialTitle = "Contoh",
            initialDescription = "Deskripsi Contoh",
            onChangePhoto = {}
        )
    }
}