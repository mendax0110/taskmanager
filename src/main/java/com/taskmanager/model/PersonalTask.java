package com.taskmanager.model;

import java.time.LocalDate;

/**
 * OOP: Just another subclass(child-class)
 */
public class PersonalTask extends Task
{
    private int effortLevel; // 1-5

    public PersonalTask(int id, String title, String description, LocalDate dueDate, int effortLevel)
    {
        super(id, title, description, dueDate, "Personal");
        if (effortLevel < 1 || effortLevel > 5)
        {
            throw new IllegalArgumentException("Effort level must be between 1 and 5");
        }
        this.effortLevel = effortLevel;
    }

    @Override
    public String getPriority()
    {
        return switch (effortLevel)
        {
            case 1, 2 -> "LOW";
            case 3 -> "MEDIUM";
            case 4 -> "HIGH";
            case 5 -> "CRITICAL";
            default -> "UNKNOWN";
        };
    }

    public int getEffortLevel() { return effortLevel; }
    public void setEffortLevel(int effortLevel) { this.effortLevel = effortLevel; }

    @Override
    public String toString()
    {
        return super.toString() + " | Effort: " + effortLevel + "/5";
    }
}