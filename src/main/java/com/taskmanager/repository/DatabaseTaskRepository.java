package com.taskmanager.repository;

import com.taskmanager.exception.TaskNotFoundException;
import com.taskmanager.model.PersonalTask;
import com.taskmanager.model.Task;
import com.taskmanager.model.WorkTask;

import javax.swing.plaf.nimbus.State;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * JDBC Concept: Connects sql list database and performs CRUB ops.
 */
public class DatabaseTaskRepository implements TaskRepository
{
    private final Connection connection;

    public DatabaseTaskRepository(String dbPath) throws SQLException
    {
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        createTablesIfNeeded();
    }

    private void createTablesIfNeeded() throws SQLException
    {
        String sql = """
                CREATE TABLE IF NOT EXISTS tasks (
                    id  INTEGER PRIMARY KEY,
                    type TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT,
                    due_date TEXT,
                    completed INTEGER DEFAULT 0,
                    extra1 TEXT,
                    extra2 TEXT
                )
                """;

        try (Statement stmt = connection.createStatement())
        {
            stmt.execute(sql);
        }
    }

    @Override
    public void save(Task task)
    {
        String sql = "INSERT INTO tasks (id, type, title, description, due_date, completed, extra1, extra2) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql))
        {
            fillStatement(ps, task);
            ps.executeUpdate();
        }
        catch (SQLException ex)
        {
            throw new RuntimeException("Failed to save task", ex);
        }
    }

    @Override
    public Optional<Task> findById(int id)
    {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM tasks WHERE id = ?"))
        {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        }
        catch (SQLException ex)
        {
            throw new RuntimeException("Failed to find task", ex);
        }

        return Optional.empty();
    }

    @Override
    public List<Task> findAll()
    {
        List<Task> tasks = new ArrayList<>();
        try (Statement stmt = connection.createStatement();  ResultSet rs = stmt.executeQuery("SELECT * FROM tasks ORDER BY id"))
        {
            while (rs.next()) tasks.add(mapRow(rs));
        }
        catch (SQLException ex)
        {
            throw new RuntimeException("Failed to load tasks", ex);
        }

        return tasks;
    }

    @Override
    public void delete(int id) throws TaskNotFoundException
    {
        if (findById(id).isEmpty()) throw new TaskNotFoundException(id);
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM tasks WHERE id = ?"))
        {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        catch (SQLException ex)
        {
            throw new RuntimeException("Failed to delete task", ex);
        }
    }

    @Override
    public void update (Task task) throws TaskNotFoundException
    {
        if (findById(task.getId()).isEmpty()) throw new TaskNotFoundException(task.getId());
        String sql = "UPDATE tasks SET title=?, description=?, due_date=?, completed=?, extra1=?, extra2=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getDueDate() != null ? task.getDueDate().toString() : null);
            ps.setInt(4, task.isCompleted() ? 1: 0);
            if (task instanceof WorkTask wt)
            {
                ps.setString(5, wt.getProject());
                ps.setString(6, wt.getUrgency().name());
            }
            else if (task instanceof PersonalTask pt)
            {
                ps.setString(5, String.valueOf(pt.getEffortLevel()));
                ps.setString(6, null);
            }
            ps.setInt(7, task.getId());
            ps.executeUpdate();
        }
        catch (SQLException ex)
        {
            throw new RuntimeException("Failed to update task", ex);
        }
    }

    @Override
    public int nextId()
    {
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM tasks"))
        {
            return rs.next() ? rs.getInt("next_id") : 1;
        }
        catch (SQLException ex)
        {
            throw new RuntimeException("Failed to get nex ID: ", ex);
        }
    }

    private void fillStatement(PreparedStatement ps, Task task) throws SQLException
    {
        ps.setInt(1, task.getId());
        ps.setString(2, task.getClass().getSimpleName());
        ps.setString(3, task.getTitle());
        ps.setString(4, task.getDescription());
        ps.setString(5, task.getDueDate() != null ? task.getDueDate().toString() : null);
        ps.setInt(6, task.isCompleted() ? 1 : 0);

        if (task instanceof WorkTask wt)
        {
            ps.setString(7, wt.getPriority());
            ps.setString(8, wt.getUrgency().name());
        }
        else if (task instanceof PersonalTask pt)
        {
            ps.setString(7, String.valueOf(pt.getEffortLevel()));
            ps.setString(8, null);
        }
    }

    private Task mapRow(ResultSet rs) throws SQLException
    {
        int id = rs.getInt("id");
        String type = rs.getString("type");
        String title = rs.getString("title");
        String desc = rs.getString("description");
        String dueDateStr = rs.getString("due_date");
        LocalDate dueDate = dueDateStr != null ? LocalDate.parse(dueDateStr) : null;
        boolean completed = rs.getInt("completed") == 1;
        String extra1 = rs.getString("extra1");
        String extra2 = rs.getString("extra2");

        Task task;
        if ("WorkTask".equals(type))
        {
            task = new WorkTask(id, title, desc, dueDate, extra1, WorkTask.Urgency.valueOf(extra2));
        }
        else
        {
            task = new PersonalTask(id, title, desc, dueDate, Integer.parseInt(extra1));
        }

        if (completed) task.complete();
        return task;
    }

    public void close()
    {
        try
        {
            connection.close();
        }
        catch (SQLException ex)
        {
            System.err.println("Exception happnened while closing!" + ex);
        }
    }
}