package com.example.aiassistantcoder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private final List<Project> projectList;

    public ProjectAdapter(List<Project> projectList) {
        this.projectList = projectList;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.project_item, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);
        holder.projectTitle.setText(project.getTitle());

        if (project.getCreatedAt() != null) {
            String formattedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(project.getCreatedAt());
            holder.projectDate.setText(formattedDate);
        } else {
            holder.projectDate.setText("");
        }

        // open project
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ResponseActivity.class);
            intent.putExtra("projectTitle", project.getTitle());
            v.getContext().startActivity(intent);
        });

        // delete project
        holder.deleteButton.setOnClickListener(v -> {
            ProjectRepository.getInstance().deleteProject(project);
            Toast.makeText(v.getContext(), "Project deleted", Toast.LENGTH_SHORT).show();
        });

        // rename project (pencil)
        holder.renameButton.setOnClickListener(v -> {
            Context context = v.getContext();

            EditText input = new EditText(context);
            input.setText(project.getTitle());
            input.setSelection(input.getText().length());

            new AlertDialog.Builder(context)
                    .setTitle("Rename Project")
                    .setView(input)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String newTitle = input.getText().toString().trim();
                        if (newTitle.isEmpty()) {
                            Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // update local object
                        project.setTitle(newTitle);
                        notifyItemChanged(holder.getAdapterPosition());

                        // persist to Firestore
                        ProjectRepository.getInstance()
                                .saveProjectToFirestore(project, new ProjectRepository.ProjectSaveCallback() {
                                    @Override
                                    public void onSaved(String projectId) {
                                        Toast.makeText(context, "Project renamed", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Toast.makeText(context, "Rename failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    public static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView projectTitle, projectDate;
        ImageButton deleteButton, renameButton;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            projectTitle = itemView.findViewById(R.id.project_title);
            projectDate = itemView.findViewById(R.id.project_date);
            deleteButton = itemView.findViewById(R.id.delete_project_button);
            renameButton = itemView.findViewById(R.id.rename_project_button);
        }
    }
}
