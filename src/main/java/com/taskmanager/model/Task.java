package com.taskmanager.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * OOP: Abstract base class (abs + inher)
 * All Task Types (Work, Personal) extend this class.
 * Abstr forces subclasses (child-classes) to implement getPriority()
 */
public abstract class Task
{
    private int id;
    private String title;
    private String description;
    private boolean completed;
    private LocalDate dueDate;
    private String category;

    public Task(int id, String title, String description, LocalDate dueDate, String category)
    {
        this.id = id;
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        this.description = description;
        this.completed = false;
        this.dueDate = dueDate;
        this.category = category;
    }

    public abstract String getPriority();

    public boolean isOverdue()
    {
        return !completed && dueDate != null && dueDate.isBefore(LocalDate.now());
    }

    public void complete()
    {
        this.completed = true;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = Objects.requireNonNull(title); }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isCompleted() { return completed; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public String toString()
    {
        String status = completed ? "Y" : (isOverdue() ? "OVERDUE" : "o");
        return String.format("[%s] #%d [%s] %s | Due: %s | Priority: %s", status, id, category, title, dueDate != null ? dueDate : "No date", getPriority());
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        return id == ((Task) o).id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }
}