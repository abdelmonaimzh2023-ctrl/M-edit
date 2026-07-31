package com.montage.app.ui.projects

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.montage.app.R
import com.montage.app.data.entity.Project

class ProjectAdapter(
    private val onProjectClick: (Project) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    private var projects = listOf<Project>()

    fun submitList(list: List<Project>) {
        projects = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projects[position]
        holder.bind(project)
        holder.itemView.setOnClickListener { onProjectClick(project) }
    }

    override fun getItemCount() = projects.size

    class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
        private val name: TextView = itemView.findViewById(R.id.projectName)
        private val details: TextView = itemView.findViewById(R.id.projectDetails)

        fun bind(project: Project) {
            name.text = project.name
            details.text = "${formatDuration(project.duration)} • ${project.exportResolution}"
            // يمكن تحميل صورة مصغرة هنا لاحقاً
        }

        private fun formatDuration(millis: Long): String {
            val seconds = millis / 1000
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return String.format("%d:%02d", minutes, remainingSeconds)
        }
    }
}
