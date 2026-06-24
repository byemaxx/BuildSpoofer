package dev.byemaxx.buidspoofer

data class Template(
    val id: String,
    val name: String,
    val properties: Map<String, String>,
    val features: List<String> = emptyList(),
    val isEditable: Boolean = true
)
