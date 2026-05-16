package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.cache.Database

fun createWorkspaceSessionService(database: Database): WorkspaceSessionService {
    return WorkspaceSessionService(database, JvmWorkspaceCryptoService())
}
