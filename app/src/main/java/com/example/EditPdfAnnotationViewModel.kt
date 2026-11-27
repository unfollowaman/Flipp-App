package com.example

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel

/** Keeps page annotations in memory across Activity configuration changes without parceling large stroke paths. */
class EditPdfAnnotationViewModel : ViewModel() {
    val annotationsByPage: SnapshotStateMap<Int, PageAnnotationSet> = mutableStateMapOf()
}
