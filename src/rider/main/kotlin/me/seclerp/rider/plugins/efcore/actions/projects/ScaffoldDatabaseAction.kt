package me.seclerp.rider.plugins.efcore.actions.projects

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.rider.util.idea.getService
import me.seclerp.rider.plugins.efcore.clients.DatabaseClient
import me.seclerp.rider.plugins.efcore.clients.DbContextClient
import me.seclerp.rider.plugins.efcore.clients.ManagementClient
import me.seclerp.rider.plugins.efcore.commands.executeCommandUnderProgress
import me.seclerp.rider.plugins.efcore.dialogs.ScaffoldDatabaseDialogWrapper
import me.seclerp.rider.plugins.efcore.models.EfCoreVersion

class ScaffoldDatabaseAction : BaseEfCoreAction() {
    override fun ready(actionEvent: AnActionEvent, efCoreVersion: EfCoreVersion) {
        val intellijProject = actionEvent.project!!
        val dialog = buildDialogInstance(actionEvent) {
            ScaffoldDatabaseDialogWrapper(efCoreVersion, model, intellijProject, currentDotnetProjectName)
        }

        if (dialog.showAndGet()) {
            val dbContextClient = intellijProject.getService<DbContextClient>()
            val commonOptions = getCommonOptions(dialog)

            executeCommandUnderProgress(intellijProject, "Updating database...", "Database has been updated") {
                dbContextClient.scaffold(
                    efCoreVersion, commonOptions,
                    dialog.connection,
                    dialog.provider,
                    dialog.outputFolder,
                    dialog.useAttributes,
                    dialog.useDatabaseNames,
                    dialog.generateOnConfiguring,
                    dialog.usePluralizer,
                    dialog.dbContextName,
                    dialog.dbContextFolder,
                    dialog.scaffoldAllTables,
                    dialog.tablesList.map { it.data },
                    dialog.scaffoldAllSchemas,
                    dialog.schemasList.map { it.data })
            }
        }
    }
}