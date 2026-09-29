package com.mariokernich.nextset.core.model

/** Which timer an editor works on. */
sealed interface TimerEditorTarget {
    data class Quick(val index: Int) : TimerEditorTarget

    data class Preset(val id: String) : TimerEditorTarget

    data object NewPreset : TimerEditorTarget
}
