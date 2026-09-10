package com.example.focuslab.focus.model

const val MAX_CATEGORY_TITLE_LENGTH: Int = 24

val CURATED_CATEGORY_EMOJIS: List<String> = listOf(
    "📚", "💻", "📖", "🎨", "✍️",
    "🧠", "🎯", "🧪", "🎓", "🎵",
    "🎹", "⌨️", "🏃", "🏋️", "🧘",
    "🌱", "🚀", "🔬", "🗣️", "📝"
)

enum class CategoryDraftError {
    BlankTitle,
    TitleTooLong,
    DuplicateTitle,
    InvalidEmoji
}

fun validateCategoryDraft(
    title: String,
    emoji: String,
    existingCategories: List<FocusCategory>,
    editedCategoryId: String? = null
): CategoryDraftError? {
    val trimmedTitle = title.trim()
    if (trimmedTitle.isEmpty()) return CategoryDraftError.BlankTitle
    if (trimmedTitle.length > MAX_CATEGORY_TITLE_LENGTH) {
        return CategoryDraftError.TitleTooLong
    }
    val normalizedTitle = normalizeCategoryTitle(trimmedTitle)
    if (existingCategories.any { category ->
            category.id != editedCategoryId &&
                normalizeCategoryTitle(category.title) == normalizedTitle
        }
    ) {
        return CategoryDraftError.DuplicateTitle
    }
    if (emoji !in CURATED_CATEGORY_EMOJIS) return CategoryDraftError.InvalidEmoji
    return null
}
