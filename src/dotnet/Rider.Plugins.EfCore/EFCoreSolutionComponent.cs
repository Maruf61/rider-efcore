using System.Collections.Generic;
using System.Linq;
using JetBrains.Core;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.Rd.Tasks;
using JetBrains.RdBackend.Common.Features;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Modules;
using JetBrains.ReSharper.Psi.Util;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.RiderTutorials.Utils;
using JetBrains.Util;
using Rider.Plugins.EfCore.Exceptions;
using Rider.Plugins.EfCore.Extensions;
using Rider.Plugins.EfCore.Rd;

namespace Rider.Plugins.EfCore
{
    [SolutionComponent]
    public class EfCoreSolutionComponent
    {
        private readonly ISolution _solution;

        public EfCoreSolutionComponent(ISolution solution, ILogger logger)
        {
            _solution = solution;

            var riderProjectOutputModel = solution.GetProtocolSolution().GetRiderEfCoreModel();

            riderProjectOutputModel.GetAvailableMigrationsProjects.Set(GetAvailableMigrationsProjects);
            riderProjectOutputModel.GetAvailableStartupProjects.Set(GetAvailableStartupProjects);
            riderProjectOutputModel.HasAvailableMigrations.Set(HasAvailableMigrations);
            riderProjectOutputModel.GetAvailableMigrations.Set(GetAvailableMigrations);
            riderProjectOutputModel.GetAvailableDbContexts.Set(GetAvailableDbContexts);
        }

        private RdTask<List<MigrationsProjectInfo>> GetAvailableMigrationsProjects(Lifetime lifetime, Unit _)
        {
            using (ReadLockCookie.Create())
            {
                var allProjectNames = EfCoreHelper.GetSupportedMigrationProjects(_solution)
                    .Select(project => new MigrationsProjectInfo(
                        project.Guid,
                        project.Name,
                        project.ProjectFileLocation.FullPath,
                        project.GetDefaultNamespace() ?? string.Empty))
                    .ToList();

                return RdTask<List<MigrationsProjectInfo>>.Successful(allProjectNames);
            }
        }

        private RdTask<List<StartupProjectInfo>> GetAvailableStartupProjects(Lifetime lifetime, Unit _)
        {
            using (ReadLockCookie.Create())
            {
                var allProjectNames = EfCoreHelper.GetSupportedStartupProjects(_solution)
                    .Select(project => new StartupProjectInfo(
                        project.Guid,
                        project.Name,
                        project.ProjectFileLocation.FullPath,
                        project.TargetFrameworkIds
                            .Where(frameworkId => !frameworkId.IsNetStandard)
                            .Select(frameworkId => frameworkId.MapTargetFrameworkId())
                            .ToList(),
                        project.GetDefaultNamespace() ?? string.Empty))
                    .ToList();

                return RdTask<List<StartupProjectInfo>>.Successful(allProjectNames);
            }
        }

        private RdTask<bool> HasAvailableMigrations(Lifetime lifetime, MigrationsIdentity identity)
        {
            using (CompilationContextCookie.GetExplicitUniversalContextIfNotSet())
            using (ReadLockCookie.Create())
            {
                var project = _solution.GetProjectByName(identity.ProjectName);
                if (project is null)
                {
                    return RdTask<bool>.Faulted(new ProjectNotFoundException(identity.ProjectName));
                }

                var projectHasMigrations = project.GetPsiModules()
                    ?.SelectMany(module => module.FindInheritorsOf(project, EfCoreKnownTypeNames.MigrationBaseClass))
                    .Select(cl => cl.ToMigrationInfo())
                    .Any(migrationInfo =>
                        migrationInfo != null && migrationInfo.DbContextClassFullName == identity.DbContextClassFullName);

                return RdTask<bool>.Successful(projectHasMigrations ?? false);
            }
        }

        private RdTask<List<MigrationInfo>> GetAvailableMigrations(Lifetime lifetime, MigrationsIdentity identity)
        {
            using (CompilationContextCookie.GetExplicitUniversalContextIfNotSet())
            using (ReadLockCookie.Create())
            {
                var project = _solution.GetProjectByName(identity.ProjectName);
                if (project is null)
                {
                    return RdTask<List<MigrationInfo>>.Faulted(new ProjectNotFoundException(identity.ProjectName));
                }

                var foundMigrations = project
                    .GetPsiModules()
                    .SelectMany(module => module.FindInheritorsOf(project, EfCoreKnownTypeNames.MigrationBaseClass))
                    .Distinct(migrationClass => migrationClass.GetFullClrName()) // To get around of multiple modules (multiple target frameworks)
                    .Select(migrationClass => migrationClass.ToMigrationInfo())
                    .Where(m => m.DbContextClassFullName == identity.DbContextClassFullName)
                    .ToList();

                return RdTask<List<MigrationInfo>>.Successful(foundMigrations);
            }
        }

        private RdTask<List<DbContextInfo>> GetAvailableDbContexts(Lifetime lifetime, string projectName)
        {
            using (CompilationContextCookie.GetExplicitUniversalContextIfNotSet())
            using (ReadLockCookie.Create())
            {
                var project = _solution.GetProjectByName(projectName);
                if (project is null)
                {
                    return RdTask<List<DbContextInfo>>.Faulted(new ProjectNotFoundException(projectName));
                }

                var foundDbContexts = project
                    .GetPsiModules()
                    .SelectMany(module => module.FindInheritorsOf(project, EfCoreKnownTypeNames.DbContextBaseClass))
                    .Distinct(dbContextClass => dbContextClass.GetFullClrName()) // To get around of multiple modules (multiple target frameworks)
                    .Select(dbContextClass =>
                        new DbContextInfo(dbContextClass.ShortName, dbContextClass.GetFullClrName()))
                    .ToList();

                return RdTask<List<DbContextInfo>>.Successful(foundDbContexts);
            }
        }
    }
}