package me.seclerp.rider.plugins.efcore.commands

object KnownEfCommands {
    val dotnetEf     = "dotnet ef"

    object Migrations {
        val add      = "migrations add"
        val remove   = "migrations remove"
        val bundle   = "migrations bundle"
        val list     = "migrations list"
        val script   = "migrations script"
    }

    object Database {
        val update   = "database update"
        val drop     = "database drop"
    }

    object DbContext {
        val info     = "dbcontext info"
        val list     = "dbcontext list"
        val optimize = "dbcontext optimize"
        val scaffold = "dbcontext scaffold"
        val script   = "dbcontext script"
    }
}