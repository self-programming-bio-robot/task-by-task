package services

import models.TodoItem

class TodoRepository {
    private var lastId = 0L
    private val todos = mutableMapOf<Long, TodoItem>()

    fun <R : Comparable<R>>getAll(selector: (TodoItem) -> R): Collection<TodoItem>
        = todos.values.sortedBy(selector)

    fun save(todo: TodoItem): TodoItem {
        todos[todo.id] = todo
        return todo
    }

    fun get(id: Long): TodoItem? {
        return todos[id]
    }

    fun delete(id: Long) {
        todos.remove(id)
    }

    fun nextId(): Long {
        return lastId++
    }
}
class TodoService(
    private val repository: TodoRepository
) {
    fun add(title: String, description: String = ""): TodoItem {
        val id = repository.nextId();
        val todo = TodoItem(
            id, title, description
        )
        return repository.save(todo)
    }

    fun switch(id: Long, newState: Boolean): TodoItem {
        val todo = repository.get(id) ?: throw RuntimeException("Not found todo")

        return repository.save(todo.copy(done = newState))
    }

    fun getList(): Collection<TodoItem> {
        return repository.getAll { it.done }
    }
}