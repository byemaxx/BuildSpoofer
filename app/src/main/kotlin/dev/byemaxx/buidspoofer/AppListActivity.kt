package dev.byemaxx.buidspoofer

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.byemaxx.buidspoofer.databinding.ActivityAppListBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppListBinding
    private lateinit var adapter: AppListAdapter
    private var allApps: List<AppInfo> = emptyList()

    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.inflateMenu(R.menu.menu_app_list)

        val searchItem = binding.toolbar.menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as androidx.appcompat.widget.SearchView
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText ?: ""
                updateList()
                return true
            }
        })

        adapter = AppListAdapter { appInfo ->
            showTemplateSelectionDialog(appInfo)
        }
        binding.recyclerView.adapter = adapter

        binding.switchSystemApps.setOnCheckedChangeListener { _, _ ->
            updateList()
        }

        loadApps()
    }

    private fun loadApps() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        
        CoroutineScope(Dispatchers.IO).launch {
            val pm = packageManager
            val installedPackages = pm.getInstalledApplications(0)
            val prefs = TemplateManager.getSharedPreferences(this@AppListActivity)
            
            val defaultTemplate = TemplateManager.getDefaultTemplate(prefs)
            val customTemplates = TemplateManager.getCustomTemplates(prefs)
            val allTemplates = listOf(defaultTemplate, TemplateManager.PIXEL_10_PRO_XL) + customTemplates

            val apps = mutableListOf<AppInfo>()
            for (appInfo in installedPackages) {
                if (appInfo.packageName == packageName) continue

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val hasLauncherIntent = pm.getLaunchIntentForPackage(appInfo.packageName) != null
                
                val assignedId = TemplateManager.getAppTemplateId(prefs, appInfo.packageName)
                val assignedName = assignedId?.let { id -> 
                    allTemplates.find { it.id == id }?.name 
                }

                apps.add(AppInfo(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo),
                    isSystem = isSystem && !hasLauncherIntent,
                    assignedTemplateName = assignedName
                ))
            }
            
            // Sort by assigned (active first), then name
            allApps = apps.sortedWith(compareBy<AppInfo> { it.assignedTemplateName == null }.thenBy { it.appName.lowercase() })

            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                updateList()
            }
        }
    }

    private fun updateList() {
        val showSystem = binding.switchSystemApps.isChecked
        val q = currentQuery.lowercase().trim()
        val filtered = allApps.filter { 
            (showSystem || !it.isSystem) && 
            (q.isEmpty() || it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q))
        }
        adapter.submitList(filtered)
    }

    private fun showTemplateSelectionDialog(appInfo: AppInfo) {
        val prefs = TemplateManager.getSharedPreferences(this)
        val defaultTemplate = TemplateManager.getDefaultTemplate(prefs)
        val customTemplates = TemplateManager.getCustomTemplates(prefs)
        val templates = listOf(defaultTemplate, TemplateManager.PIXEL_10_PRO_XL) + customTemplates

        val names = mutableListOf(getString(R.string.follow_global_template))
        names.addAll(templates.map { it.name })

        val currentAssignedId = TemplateManager.getAppTemplateId(prefs, appInfo.packageName)
        val selectedIndex = if (currentAssignedId == null) {
            0
        } else {
            templates.indexOfFirst { it.id == currentAssignedId } + 1
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(appInfo.appName)
            .setSingleChoiceItems(names.toTypedArray(), selectedIndex) { dialog, which ->
                val newTemplateId = if (which == 0) null else templates[which - 1].id
                val newTemplateName = if (which == 0) null else templates[which - 1].name
                
                TemplateManager.setAppTemplateId(prefs, appInfo.packageName, newTemplateId, this)
                
                // Update local list
                appInfo.assignedTemplateName = newTemplateName
                adapter.notifyDataSetChanged() // or a targeted update
                
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
