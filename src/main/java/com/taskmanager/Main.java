package com.taskmanager;

import com.taskmanager.exception.TaskNotFoundException;
import com.taskmanager.model.*;
import com.taskmanager.pattern.AuditLogger;
import com.taskmanager.repository.*;
import com.taskmanager.service.TaskService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main
{
    private static final Scanner scanner = new Scanner(System.in);
    private static TaskService service;
    private static AuditLogger auditLogger;

    public static void main (String[] args)
    {
        System.out.println("==========================");
        System.out.println("|     Task manager       |");
        System.out.println("==========================");

        setupApplication();
        seedDemoData();

        boolean running = true;
        while (running)
        {
            printMenu();
            String choice = scanner.nextLine().trim();

            try
            {
                running = handleMenuChoice(choice);
            }
            catch (TaskNotFoundException ex)
            {
                System.out.println("Error: " + ex.getMessage());
            }
            catch (IllegalArgumentException ex)
            {
                System.out.println("Invalid input: " + ex.getMessage());
            }
            catch (Exception ex)
            {
                System.out.println("Unexpected error: " + ex.getMessage());
            }
        }

        System.out.println("\nBye bye :)");
    }

    private static void setupApplication()
    {
        TaskRepository repository = new FileTaskRepository("tasks.csv");
        service = new TaskService(repository);
        auditLogger = new AuditLogger();
        service.addObserver(auditLogger);

        System.out.println("Storage: File (tasks.csv)");
        System.out.println("Audit logger: active\n");
    }

    private static void seedDemoData()
    {
        if (!service.getAllTasks().isEmpty()) return;

        System.out.println("--- Seeding demo data ---");

        service.addTask(new WorkTask(service.nextId(), "Design database schema",
                "ERD for the new project", LocalDate.now().plusDays(3),
                "Backend", WorkTask.Urgency.HIGH));

        service.addTask(new WorkTask(service.nextId(), "Fix login bug",
                "Users report session timeout too early", LocalDate.now().minusDays(1),
                "Frontend", WorkTask.Urgency.CRITICAL));

        service.addTask(new PersonalTask(service.nextId(), "Buy groceries",
                "Milk, eggs, bread", LocalDate.now().plusDays(1), 1));

        service.addTask(new PersonalTask(service.nextId(), "Read Java book",
                "Chapter 8: Generics", LocalDate.now().plusDays(7), 3));

        service.addTask(new WorkTask(service.nextId(), "Write unit tests",
                "Cover service layer", LocalDate.now().plusDays(5),
                "Backend", WorkTask.Urgency.MEDIUM));

        System.out.println();
    }

    private static void printMenu()
    {
        System.out.println("\n=========================");
        System.out.printf("  %s%n", service.getSummary());
        System.out.println(" 1. List all tasks");
        System.out.println(" 2. Add Work task");
        System.out.println("'3. Add Personal task");
        System.out.println(" 4. Complete a task");
        System.out.println(" 5. Delete a task");
        System.out.println(" 6. Show overdue tasks");
        System.out.println(" 7. Tasks by category");
        System.out.println(" 8. High priority work tasks");
        System.out.println(" 9. Tasks sorted by due date");
        System.out.println(" A. View Audit log");
        System.out.println(" Q. Quit");
        System.out.println("Choice: ");
    }

    private static boolean handleMenuChoice(String choice) throws TaskNotFoundException
    {
        return switch (choice.toUpperCase())
        {
            case "1" -> { listAllTasks();   yield true; }
            case "2" -> { addWorkTask();   yield true; }
            case "3" -> { addPersonalTask();   yield true; }
            case "4" -> { completeTask();   yield true; }
            case "5" -> { deleteTask();   yield true; }
            case "6" -> { showOverdueTasks();   yield true; }
            case "7" -> { showByCategory();   yield true; }
            case "8" -> { showHighPriorityWork();   yield true; }
            case "9" -> { showSortedByDate();   yield true; }
            case "A" -> { auditLogger.printLog();   yield true; }
            case "Q" -> false;
            default -> { System.out.println("Unknown option."); yield true; }
        };
    }

    private static void listAllTasks()
    {
        List<Task> tasks = service.getAllTasks();
        if (tasks.isEmpty()) { System.out.println("No tasks yet."); return; }
        System.out.println("\n=== All Tasks ===");
        tasks.forEach(System.out::println);
    }

    private static LocalDate parseDate(String input)
    {
        if (input == null || input.isBlank()) return null;

        try
        {
            return LocalDate.parse(input.trim());
        }
        catch (DateTimeParseException ex)
        {
            System.out.println("Invalid date format, setting no due date.");
            return null;
        }
    }

    private static int readInt()
    {
        try
        {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Please enter a valid number.");
        }
    }

    private static void addWorkTask()
    {
        System.out.print("Title: ");
        String title = scanner.nextLine();
        System.out.print("Description: ");
        String desc = scanner.nextLine();
        System.out.print("Project name: ");
        String project = scanner.nextLine();
        System.out.print("Due date (YYYY-MM-DD, or blank): ");
        LocalDate due = parseDate(scanner.nextLine());
        System.out.print("Urgency (LOW/MEDIUM/HIGH/CRITICAL): ");
        WorkTask.Urgency urgency = WorkTask.Urgency.valueOf(scanner.nextLine().trim().toUpperCase());

        Task task = new WorkTask(service.nextId(), title, desc, due, project, urgency);
        service.addTask(task);
        System.out.println("Work task added: " + task);
    }

    private static void addPersonalTask()
    {
        System.out.print("Title: ");
        String title = scanner.nextLine();
        System.out.print("Description: ");
        String desc = scanner.nextLine();
        System.out.print("Due date (YYYY-MM-DD, or blank): ");
        LocalDate due = parseDate(scanner.nextLine());
        System.out.print("Effort level (1=easy … 5=very hard): ");
        int effort = Integer.parseInt(scanner.nextLine().trim());

        Task task = new PersonalTask(service.nextId(), title, desc, due, effort);
        service.addTask(task);
        System.out.println("Personal task added: " + task);
    }

    private static void completeTask() throws TaskNotFoundException
    {
        System.out.print("Task ID to complete: ");
        int id = readInt();
        service.completeTask(id);
        System.out.println("Task #" + id + " marked as completed.");
    }

    private static void deleteTask() throws TaskNotFoundException
    {
        System.out.print("Task ID to delete: ");
        int id = readInt();
        service.deleteTask(id);
        System.out.println("Task #" + id + " deleted.");
    }

    private static void showOverdueTasks()
    {
        List<Task> overdue = service.getOverdueTasks();
        System.out.println("\n=== Overdue Tasks (" + overdue.size() + ") ===");
        if (overdue.isEmpty()) System.out.println("None! Great job.");
        else overdue.forEach(System.out::println);
    }

    private static void showByCategory()
    {
        Map<String, List<Task>> byCategory = service.getTasksByCategory();
        System.out.println("\n=== Tasks by Category ===");
        byCategory.forEach((category, tasks) -> {
            System.out.println("\n[" + category + "] — " + tasks.size() + " task(s):");
            tasks.forEach(t -> System.out.println("  " + t));
        });
    }

    private static void showHighPriorityWork()
    {
        var tasks = service.getHighPriorityWorkTasks();
        System.out.println("\n=== High/Critical Priority Work Tasks ===");
        if (tasks.isEmpty()) System.out.println("None.");
        else tasks.forEach(System.out::println);
    }

    private static void showSortedByDate()
    {
        System.out.println("\n=== Tasks Sorted by Due Date ===");
        service.getTaskSortedByDueDate().forEach(System.out::println);
    }
}