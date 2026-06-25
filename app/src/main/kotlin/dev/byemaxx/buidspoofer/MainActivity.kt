package dev.byemaxx.buidspoofer

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import dev.byemaxx.buidspoofer.databinding.ActivityMainBinding
import dev.byemaxx.buidspoofer.databinding.DialogAboutBinding
import dev.byemaxx.buidspoofer.databinding.ItemTemplateBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TemplateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = TemplateManager.getSharedPreferences(this)
        
        adapter = TemplateAdapter(this, 
            onSelected = { template ->
                TemplateManager.setActiveTemplateId(prefs, template.id, this@MainActivity)
                loadTemplates()
            },
            onEdit = { template ->
                val intent = Intent(this, EditTemplateActivity::class.java)
                intent.putExtra(EditTemplateActivity.EXTRA_TEMPLATE_ID, template.id)
                startActivity(intent)
            }
        )

        binding.recyclerView.adapter = adapter

        Thread {
            val hasRoot = TemplateManager.hasRoot()
            if (hasRoot) {
                TemplateManager.generateAndSaveDefaultTemplate(prefs)
            }
            
            runOnUiThread {
                binding.rootWarningCard.visibility = if (hasRoot) View.GONE else View.VISIBLE
                loadTemplates()
            }
        }.start()

        binding.fabAdd.setOnClickListener {
            showTemplateSelectionDialog()
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_about -> {
                    showAboutDialog()
                    true
                }
                else -> false
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadTemplates()
    }

    private fun loadTemplates() {
        val prefs = TemplateManager.getSharedPreferences(this)
        val defaultTemplate = TemplateManager.getDefaultTemplate(prefs)
        val custom = TemplateManager.getCustomTemplates(prefs)
        val all = listOf(defaultTemplate, TemplateManager.PIXEL_10_PRO_XL) + custom
        val active = TemplateManager.getActiveTemplate(prefs)
        binding.textActiveTemplate.text = active.name
        binding.textActiveHint.text = if (active.id == "default") {
            getString(R.string.system_template)
        } else {
            getString(R.string.restart_target_hint)
        }
        adapter.submitList(all, active.id)
    }

    private fun showTemplateSelectionDialog() {
        val choices = arrayOf(
            getString(R.string.base_pixel),
            getString(R.string.base_system),
            getString(R.string.base_blank)
        )
        val baseIds = arrayOf(
            TemplateManager.PIXEL_10_PRO_XL.id,
            "default",
            EditTemplateActivity.BLANK_TEMPLATE_ID
        )
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_base_template)
            .setItems(choices) { _, which ->
                val intent = Intent(this, EditTemplateActivity::class.java)
                intent.putExtra(EditTemplateActivity.EXTRA_BASE_TEMPLATE_ID, baseIds[which])
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAboutDialog() {
        val aboutBinding = DialogAboutBinding.inflate(layoutInflater)
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }

        aboutBinding.textVersion.text = getString(
            R.string.version_format,
            packageInfo.versionName ?: "—",
            packageInfo.longVersionCode
        )

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(aboutBinding.root)
            .setPositiveButton(R.string.close, null)
            .create()

        aboutBinding.buttonGithub.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))
            try {
                startActivity(intent)
            } catch (_: Exception) {
                Snackbar.make(binding.root, R.string.unable_to_open_link, Snackbar.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    fun copyTemplate(template: Template) {
        val intent = Intent(this, EditTemplateActivity::class.java)
        intent.putExtra(EditTemplateActivity.EXTRA_BASE_TEMPLATE_ID, template.id)
        intent.putExtra(
            EditTemplateActivity.EXTRA_SUGGESTED_NAME,
            getString(R.string.copy_name, template.name)
        )
        startActivity(intent)
    }

    fun deleteTemplate(template: Template) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_template_title)
            .setMessage(getString(R.string.delete_template_message, template.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                val prefs = TemplateManager.getSharedPreferences(this)
                val activeId = prefs.getString(TemplateManager.KEY_ACTIVE_TEMPLATE_ID, "default")
                
                val custom = TemplateManager.getCustomTemplates(prefs).filter { it.id != template.id }
                TemplateManager.saveCustomTemplates(prefs, custom, this)

                if (activeId == template.id) {
                    TemplateManager.setActiveTemplateId(prefs, "default", this)
                }
                loadTemplates()
                Snackbar.make(binding.root, R.string.template_deleted, Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }

    companion object {
        private const val GITHUB_URL = "https://github.com/byemaxx/BuildSpoofer"
    }
}

class TemplateAdapter(
    private val activity: MainActivity,
    private val onSelected: (Template) -> Unit,
    private val onEdit: (Template) -> Unit
) : RecyclerView.Adapter<TemplateAdapter.ViewHolder>() {

    private var templates = listOf<Template>()
    private var activeId = ""

    fun submitList(list: List<Template>, active: String) {
        templates = list
        activeId = active
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTemplateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val template = templates[position]
        val selected = template.id == activeId
        holder.binding.textName.text = template.name
        holder.binding.textType.text = when {
            template.id == "default" -> activity.getString(R.string.system_template)
            template.isEditable -> activity.getString(R.string.custom_template)
            else -> activity.getString(R.string.built_in_template)
        }
        holder.binding.textSummary.text = if (template.id == "default") {
            activity.getString(R.string.property_count, template.properties.size)
        } else {
            activity.getString(
                R.string.property_summary,
                template.properties["MODEL"] ?: template.properties["DEVICE"] ?: "Android",
                template.properties["RELEASE"] ?: "—",
                template.properties.size
            )
        }
        holder.binding.imageSelected.visibility = if (selected) View.VISIBLE else View.GONE
        holder.binding.cardTemplate.strokeWidth = if (selected) 2.dpToPx(holder.itemView) else 1.dpToPx(holder.itemView)
        holder.binding.cardTemplate.strokeColor = MaterialColors.getColor(
            holder.binding.cardTemplate,
            if (selected) com.google.android.material.R.attr.colorPrimary
            else com.google.android.material.R.attr.colorOutlineVariant
        )
        holder.binding.cardTemplate.setCardBackgroundColor(
            MaterialColors.getColor(
                holder.binding.cardTemplate,
                if (selected) com.google.android.material.R.attr.colorPrimaryContainer
                else com.google.android.material.R.attr.colorSurfaceContainerLow
            )
        )
        
        holder.binding.btnMore.setOnClickListener { view ->
            val popup = PopupMenu(activity, view)
            if (template.isEditable) {
                popup.menu.add(Menu.NONE, 1, 1, R.string.edit)
            }
            popup.menu.add(Menu.NONE, 2, 2, R.string.copy)
            if (template.isEditable) {
                popup.menu.add(Menu.NONE, 3, 3, R.string.delete)
            }
            
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        onEdit(template)
                        true
                    }
                    2 -> {
                        activity.copyTemplate(template)
                        true
                    }
                    3 -> {
                        activity.deleteTemplate(template)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
        
        val clickListener = View.OnClickListener {
            if (!selected) {
                onSelected(template)
                Snackbar.make(
                    activity.findViewById(android.R.id.content),
                    activity.getString(R.string.template_activated, template.name),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
        holder.binding.cardTemplate.setOnClickListener(clickListener)
    }

    override fun getItemCount() = templates.size

    class ViewHolder(val binding: ItemTemplateBinding) : RecyclerView.ViewHolder(binding.root)

    private fun Int.dpToPx(view: View): Int =
        (this * view.resources.displayMetrics.density).toInt()
}
