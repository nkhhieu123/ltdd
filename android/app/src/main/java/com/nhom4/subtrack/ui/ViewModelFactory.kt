package com.nhom4.subtrack.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nhom4.subtrack.SubTrackApp
import com.nhom4.subtrack.ui.edit.EditViewModel
import com.nhom4.subtrack.ui.list.ListViewModel
import com.nhom4.subtrack.ui.organize.OrganizeViewModel
import com.nhom4.subtrack.ui.reports.ReportsViewModel
import com.nhom4.subtrack.ui.settings.SettingsViewModel

private fun CreationExtras.app() = this[APPLICATION_KEY] as SubTrackApp

object AppViewModels {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { ListViewModel(app()) }
        initializer { OrganizeViewModel(app()) }
        initializer { ReportsViewModel(app()) }
        initializer { SettingsViewModel(app()) }
    }

    fun edit(id: Long): ViewModelProvider.Factory = viewModelFactory {
        initializer { EditViewModel(app(), id) }
    }
}
