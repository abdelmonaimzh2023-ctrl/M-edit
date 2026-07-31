package com.montage.app.ui.projects

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.montage.app.data.db.AppDatabase
import com.montage.app.data.entity.Project
import com.montage.app.databinding.ActivityProjectsBinding
import com.montage.app.ui.editor.EditorActivity
import kotlinx.coroutines.launch

class ProjectsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProjectsBinding
    private lateinit var adapter: ProjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ProjectAdapter { project ->
            openProject(project)
        }

        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter

        binding.fabNewProject.setOnClickListener {
            createNewProject()
        }

        loadProjects()
    }

    private fun loadProjects() {
        val dao = AppDatabase.getDatabase(this).projectDao()
        lifecycleScope.launch {
            dao.getAllProjects().collect { projects ->
                adapter.submitList(projects)
                binding.projectCount.text = "${projects.size} مشروع"
                if (projects.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.emptyView.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun createNewProject() {
        // في البداية نختار فيديو بسيطاً، لاحقاً سنضيف نافذة مخصصة
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        startActivityForResult(intent, 101)
    }

    private fun openProject(project: Project) {
        val intent = Intent(this, EditorActivity::class.java)
        intent.putExtra("project_id", project.id)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                lifecycleScope.launch {
                    val project = Project(
                        name = "مشروع جديد",
                        videoUri = uri.toString()
                    )
                    val id = AppDatabase.getDatabase(this@ProjectsActivity).projectDao().insert(project)
                    val intent = Intent(this@ProjectsActivity, EditorActivity::class.java)
                    intent.putExtra("project_id", id)
                    startActivity(intent)
                }
            }
        }
    }
}
