package com.example.focuslab.focus

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.focuslab.focus.model.CURATED_CATEGORY_EMOJIS
import com.example.focuslab.focus.model.CategoryDraftError
import com.example.focuslab.focus.model.FocusCategory
import com.example.focuslab.focus.model.MAX_CATEGORY_TITLE_LENGTH
import com.example.focuslab.focus.model.validateCategoryDraft

@Composable
fun CategoryEditor(
    categories: List<FocusCategory>,
    enabled: Boolean,
    onAddCategory: (title: String, emoji: String) -> Unit,
    onEditCategory: (id: String, title: String, emoji: String) -> Unit,
    onDeleteCategory: (id: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var editedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val editedCategory = categories.firstOrNull { it.id == editedCategoryId }

    when {
        adding -> key("add-category") {
            CategoryDraftDialog(
                title = "Добавить категорию",
                initialTitle = "",
                initialEmoji = CURATED_CATEGORY_EMOJIS.first(),
                categories = categories,
                editedCategoryId = null,
                enabled = enabled,
                onSave = { draftTitle, draftEmoji ->
                    onAddCategory(draftTitle, draftEmoji)
                    adding = false
                },
                onDismiss = { adding = false }
            )
        }

        editedCategory != null -> key(editedCategory.id) {
            CategoryDraftDialog(
                title = "Изменить категорию",
                initialTitle = editedCategory.title,
                initialEmoji = editedCategory.emoji,
                categories = categories,
                editedCategoryId = editedCategory.id,
                enabled = enabled,
                onSave = { draftTitle, draftEmoji ->
                    onEditCategory(editedCategory.id, draftTitle, draftEmoji)
                    editedCategoryId = null
                },
                onDismiss = { editedCategoryId = null }
            )
        }

        else -> AlertDialog(
            onDismissRequest = onDismiss,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .navigationBarsPadding()
                .imePadding(),
            title = { Text("Категории") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    categories.forEach { category ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${category.emoji} ${category.title}",
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("category_editor_item_${category.id}"),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                TextButton(
                                    onClick = { editedCategoryId = category.id },
                                    enabled = enabled,
                                    modifier = Modifier.testTag("category_editor_edit_${category.id}")
                                ) {
                                    Text("Изменить")
                                }
                                TextButton(
                                    onClick = { onDeleteCategory(category.id) },
                                    enabled = enabled && categories.size > 1,
                                    modifier = Modifier.testTag("category_editor_delete_${category.id}")
                                ) {
                                    Text("Удалить")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { adding = true },
                    enabled = enabled,
                    modifier = Modifier.testTag("category_editor_add")
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Composable
private fun CategoryDraftDialog(
    title: String,
    initialTitle: String,
    initialEmoji: String,
    categories: List<FocusCategory>,
    editedCategoryId: String?,
    enabled: Boolean,
    onSave: (title: String, emoji: String) -> Unit,
    onDismiss: () -> Unit
) {
    var draftTitle by rememberSaveable { mutableStateOf(initialTitle) }
    var draftEmoji by rememberSaveable { mutableStateOf(initialEmoji) }
    var showValidation by rememberSaveable { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val validationError = validateCategoryDraft(
        title = draftTitle,
        emoji = draftEmoji,
        existingCategories = categories,
        editedCategoryId = editedCategoryId
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .navigationBarsPadding()
            .imePadding(),
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = {
                        draftTitle = it
                        showValidation = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_editor_title"),
                    label = { Text("Название") },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    ),
                    isError = showValidation && validationError != null,
                    supportingText = {
                        Text(
                            when {
                                showValidation -> validationError.toMessage()
                                else -> "От 1 до $MAX_CATEGORY_TITLE_LENGTH символов"
                            }
                        )
                    }
                )
                Spacer(Modifier.height(12.dp))
                Text("Эмодзи")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CURATED_CATEGORY_EMOJIS.forEach { emoji ->
                        FilterChip(
                            selected = draftEmoji == emoji,
                            onClick = {
                                draftEmoji = emoji
                                showValidation = true
                            },
                            enabled = enabled,
                            label = { Text(emoji) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    showValidation = true
                    if (validationError == null) onSave(draftTitle.trim(), draftEmoji)
                },
                enabled = enabled && validationError == null,
                modifier = Modifier.testTag("category_editor_save")
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

private fun CategoryDraftError?.toMessage(): String = when (this) {
    CategoryDraftError.BlankTitle -> "Введите название"
    CategoryDraftError.TitleTooLong -> "Не больше $MAX_CATEGORY_TITLE_LENGTH символов"
    CategoryDraftError.DuplicateTitle -> "Категория с таким названием уже есть"
    CategoryDraftError.InvalidEmoji -> "Выберите эмодзи из списка"
    null -> "Готово к сохранению"
}
