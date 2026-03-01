package com.taskmanager.model;

import java.time.LocalDate;

/**
 * OOP Concept: Inher - Worktask as IS-a task.
 */
public class WorkTask extends Task
{
    public enum Urgency
    {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    private Urgency urgency;
    private String project;

    public WorkTask (int id, String title, String description, LocalDate dueDate, String project, Urgency urgency)
    {
        super(id, title, description, dueDate, "Work");
        this.project = project;
        this.urgency = urgency;
    }

    @Override
    public String getPriority ()
    {
        return urgency.name();
    }

    public Urgency getUrgency() { return urgency; }
    public void setUrgency(Urgency urgency) { this.urgency = urgency; }
    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }

    @Override
    public String toString()
    {
        return super.toString() + " | Project: " + project;
    }
}