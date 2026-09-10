package com.example.focuslab.focus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val FOCUS_DATA_STORE_NAME = "focus_lab"

val Context.focusDataStore: DataStore<Preferences> by preferencesDataStore(
    name = FOCUS_DATA_STORE_NAME
)
