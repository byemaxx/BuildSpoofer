package dev.byemaxx.buidspoofer

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.byemaxx.buidspoofer.databinding.ItemAppBinding

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystem: Boolean,
    var assignedTemplateName: String?
)

class AppListAdapter(
    private val onClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    private var items: List<AppInfo> = emptyList()

    fun submitList(newItems: List<AppInfo>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val appInfo = items[position]
        holder.binding.textAppName.text = appInfo.appName
        holder.binding.textPackageName.text = appInfo.packageName
        holder.binding.iconApp.setImageDrawable(appInfo.icon)
        
        if (appInfo.assignedTemplateName != null) {
            holder.binding.textAssignedTemplate.text = holder.itemView.context.getString(
                R.string.template_assigned_format, appInfo.assignedTemplateName
            )
            holder.binding.textAssignedTemplate.alpha = 1.0f
        } else {
            holder.binding.textAssignedTemplate.text = holder.itemView.context.getString(R.string.follow_global_template)
            holder.binding.textAssignedTemplate.alpha = 0.6f
        }

        holder.itemView.setOnClickListener {
            onClick(appInfo)
        }
    }

    override fun getItemCount(): Int = items.size

    class AppViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)
}
