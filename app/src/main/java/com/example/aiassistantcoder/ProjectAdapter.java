package com.example.aiassistantcoder;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

        // Open project on click
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, ResponseActivity.class);
            intent.putExtra("projectTitle", project.getTitle());
            context.startActivity(intent);
        });

        // Delete / rename handled by swipe in ProjectsFragment now,
        // so no button click listeners here.
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    public Project getItem(int position) {
        return projectList.get(position);
    }

    public static class ProjectViewHolder extends RecyclerView.ViewHolder {
        public final View foreground;   // sliding content
        public final View bgDelete;     // red overlay (left)
        public final View bgEdit;       // blue overlay (right)
        public final TextView projectTitle;
        public final TextView projectDate;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            foreground = itemView.findViewById(R.id.foreground);
            bgDelete = itemView.findViewById(R.id.bg_delete);
            bgEdit = itemView.findViewById(R.id.bg_edit);
            projectTitle = itemView.findViewById(R.id.project_title);
            projectDate = itemView.findViewById(R.id.project_date);
        }
    }
}
