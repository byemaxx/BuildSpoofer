package dev.byemaxx.buidspoofer

import android.os.Bundle
import android.text.InputType
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dev.byemaxx.buidspoofer.databinding.ActivityEditTemplateBinding
import java.util.UUID

class EditTemplateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditTemplateBinding
    private var editingTemplateId: String? = null
    private var sourceTemplate: Template? = null
    private val propertyInputs = mutableMapOf<String, TextInputEditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditTemplateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        editingTemplateId = intent.getStringExtra(EXTRA_TEMPLATE_ID)
        val baseTemplateId = intent.getStringExtra(EXTRA_BASE_TEMPLATE_ID)
        
        val prefs = TemplateManager.getSharedPreferences(this)
        val allCustom = TemplateManager.getCustomTemplates(prefs)
        val templateToEdit = allCustom.find { it.id == editingTemplateId }
        val defaultTemplate = TemplateManager.getDefaultTemplate(prefs)
        val availableTemplates = listOf(defaultTemplate, TemplateManager.PIXEL_10_PRO_XL) + allCustom
        sourceTemplate = templateToEdit
            ?: availableTemplates.find { it.id == baseTemplateId }
            ?: Template(
                id = BLANK_TEMPLATE_ID,
                name = getString(R.string.blank_template_name),
                properties = emptyMap(),
                features = emptyList()
            )

        val suggestedName = intent.getStringExtra(EXTRA_SUGGESTED_NAME)
        binding.editTemplateName.setText(
            when {
                templateToEdit != null -> templateToEdit.name
                suggestedName != null -> suggestedName
                sourceTemplate?.id == BLANK_TEMPLATE_ID -> ""
                else -> getString(R.string.custom_name, sourceTemplate?.name.orEmpty())
            }
        )

        val groups = linkedMapOf(
            R.string.group_identity to listOf(
                "MODEL", "DEVICE", "PRODUCT", "BRAND", "MANUFACTURER", "FINGERPRINT"
            ),
            R.string.group_build to listOf(
                "RELEASE", "SDK_INT", "SECURITY_PATCH", "ID", "INCREMENTAL",
                "DESCRIPTION", "TAGS", "TYPE", "USER", "HOST", "TIME", "TIME_SEC", "BUILD_DATE"
            ),
            R.string.group_hardware to listOf(
                "BOOTLOADER", "SOC_MANUFACTURER", "SOC_MODEL", "PLATFORM", "BASEBAND"
            ),
            R.string.group_advanced to listOf(
                "FIRST_API_LEVEL", "SDK_FULL", "BUILD_UUID", "CLIENT_ID"
            )
        )

        groups.forEach { (title, keys) ->
            addGroupTitle(title)
            keys.forEach { key -> addPropertyInput(key, sourceTemplate?.properties?.get(key)) }
        }

        binding.btnSave.setOnClickListener {
            val name = binding.editTemplateName.text.toString().takeIf { it.isNotBlank() }
                ?: getString(R.string.custom_template)
            
            val props = mutableMapOf<String, String>()
            for ((key, editText) in propertyInputs) {
                val value = editText.text.toString().trim()
                if (value.isNotEmpty()) {
                    props[key] = value
                }
            }

            val newTemplate = Template(
                id = editingTemplateId ?: UUID.randomUUID().toString(),
                name = name,
                properties = props,
                features = sourceTemplate?.features ?: emptyList(),
                isEditable = true
            )

            val updatedCustomList = if (editingTemplateId != null) {
                allCustom.map { if (it.id == editingTemplateId) newTemplate else it }
            } else {
                allCustom + newTemplate
            }

            TemplateManager.saveCustomTemplates(prefs, updatedCustomList, this)
            TemplateManager.setActiveTemplateId(prefs, newTemplate.id, this)
            
            finish()
        }
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }

    private fun addGroupTitle(titleRes: Int) {
        val title = TextView(this).apply {
            text = getString(titleRes)
            setTextColor(MaterialColors.getColor(this, MaterialR.attr.colorPrimary))
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20.dpToPx()
                bottomMargin = 10.dpToPx()
            }
        }
        binding.propertiesContainer.addView(title, binding.propertiesContainer.childCount - 1)
    }

    private fun addPropertyInput(key: String, value: String?) {
        val inputLayout = TextInputLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12.dpToPx()
            }
            hint = key
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            helperText = propertyHelp[key]
            isHelperTextEnabled = propertyHelp.containsKey(key)
        }

        val editText = TextInputEditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setText(value)
            setSelectAllOnFocus(false)
            inputType = if (key in numericProperties) {
                InputType.TYPE_CLASS_NUMBER
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            }
            if (key == "FINGERPRINT" || key == "DESCRIPTION" || key == "BASEBAND") {
                maxLines = 3
                isSingleLine = false
            } else {
                isSingleLine = true
            }
        }

        inputLayout.addView(editText)
        binding.propertiesContainer.addView(inputLayout, binding.propertiesContainer.childCount - 1)
        propertyInputs[key] = editText
    }

    companion object {
        const val EXTRA_TEMPLATE_ID = "extra_template_id"
        const val EXTRA_BASE_TEMPLATE_ID = "extra_base_template_id"
        const val EXTRA_SUGGESTED_NAME = "extra_suggested_name"
        const val BLANK_TEMPLATE_ID = "__blank__"

        private val numericProperties = setOf("SDK_INT", "FIRST_API_LEVEL", "TIME", "TIME_SEC")

        private val propertyHelp = mapOf(
            "MODEL" to "应用看到的设备名称，例如 Pixel 10 Pro XL",
            "DEVICE" to "设备代号，例如 mustang",
            "FINGERPRINT" to "完整系统构建指纹",
            "RELEASE" to "Android 主版本号",
            "SDK_INT" to "Android API level",
            "SECURITY_PATCH" to "格式：YYYY-MM-DD",
            "SOC_MODEL" to "SoC 型号，例如 Tensor G5",
            "BASEBAND" to "基带版本字符串"
        )
    }
}
