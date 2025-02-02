package me.seclerp.rider.plugins.efcore.features.shared

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.not
import com.intellij.ui.layout.selected
import com.jetbrains.rd.ui.bedsl.extensions.valueOrEmpty
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.util.idea.runUnderProgress
import me.seclerp.rider.plugins.efcore.Event
import me.seclerp.rider.plugins.efcore.features.shared.models.MigrationsProjectData
import me.seclerp.rider.plugins.efcore.features.shared.models.StartupProjectData
import me.seclerp.rider.plugins.efcore.features.shared.services.PreferredProjectsManager
import me.seclerp.rider.plugins.efcore.rd.RiderEfCoreModel
import me.seclerp.rider.plugins.efcore.ui.iconComboBox
import me.seclerp.rider.plugins.efcore.ui.items.*
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

@Suppress("UnstableApiUsage", "MemberVisibilityCanBePrivate")
abstract class EfCoreDialogWrapper(
    titleText: String,
    private val beModel: RiderEfCoreModel,
    private val intellijProject: Project,
    private val selectedDotnetProjectName: String?,
    requireMigrationsInProject: Boolean = false,
    private val requireDbContext: Boolean = true
) : DialogWrapper(true) {

    //
    // Data binding
    val commonOptions = CommonOptionsModel()

    //
    // Internal data
    private val availableMigrationsProjects =
        beModel.getAvailableMigrationsProjects
            .sync(Unit)
            .map { MigrationsProjectItem(it.name, MigrationsProjectData(it.id, it.fullPath)) }
            .toTypedArray()

    private val availableStartupProjects =
        beModel.getAvailableStartupProjects
            .sync(Unit)
            .map { StartupProjectItem(it.name, StartupProjectData(it.id, it.fullPath, it.targetFrameworks)) }
            .toTypedArray()

    private val availableBuildConfigurations =
        intellijProject.solution.solutionProperties.configurationsAndPlatformsCollection
            .valueOrEmpty()
            .distinctBy { it.configuration } // To get around of different platforms for the same configurations
            .map { BuildConfigurationItem(it.configuration) }
            .toTypedArray()

    private var targetFrameworkModel: DefaultComboBoxModel<BaseTargetFrameworkItem> = DefaultComboBoxModel()
    private var dbContextModel: DefaultComboBoxModel<DbContextItem> = DefaultComboBoxModel()

    private val isSolutionLevelMode = selectedDotnetProjectName == null

    //
    // Events
    private val migrationsProjectChangedEvent: Event<MigrationsProjectItem> = Event()
    private val startupProjectChangedEvent: Event<StartupProjectItem> = Event()
    private val dbContextChangedEvent: Event<DbContextItem?> = Event()

    //
    // Validation
    private val validator = EfCoreDialogValidator(
        commonOptions, beModel, intellijProject, requireMigrationsInProject, dbContextModel,
        availableBuildConfigurations, targetFrameworkModel)

    //
    // Preferences
    private val preferredProjectsManager = PreferredProjectsManager(intellijProject)

    //
    // Constructor
    init {
        title = titleText

        if (requireDbContext) {
            addMigrationsProjectChangedListener(::migrationsProjectChanged)
            addDbContextChangedListener(::dbContextChanged)
        }

        addStartupProjectChangedListener(::startupProjectChanged)

        initSelectedBuildConfiguration()
    }

    private fun initSelectedBuildConfiguration() {
        val currentBuilderConfiguration = intellijProject.solution.solutionProperties.activeConfigurationPlatform.value

        commonOptions.buildConfiguration =
            availableBuildConfigurations.find {
                it.displayName == currentBuilderConfiguration?.configuration
            } ?: availableBuildConfigurations.firstOrNull()
    }

    private fun initPreferredProjects() {
        if (isSolutionLevelMode) {
            initSolutionLevelPreferredProjects()
        } else {
            initProjectLevelPreferredProjects()
        }
    }

    private fun initSolutionLevelPreferredProjects() {
        val (preferredMigrationsProject, preferredStartupProject) =
            preferredProjectsManager.getGlobalProjectPair(availableMigrationsProjects, availableStartupProjects)

        refreshProjectsPair(preferredMigrationsProject, preferredStartupProject)
    }

    private fun initProjectLevelPreferredProjects() {
        val selectedDotnetProject =
            availableMigrationsProjects.find { it.displayName == selectedDotnetProjectName }

        val (preferredMigrationsProject, preferredStartupProject) =
            preferredProjectsManager.getProjectPair(selectedDotnetProject?.data?.id, availableMigrationsProjects, availableStartupProjects)

        refreshProjectsPair(preferredMigrationsProject, preferredStartupProject)
    }

    private fun refreshProjectsPair(migrationsProject: MigrationsProjectItem?, startupProject: StartupProjectItem?) {
        migrationsProjectSetter(migrationsProject)
        startupProjectSetter(startupProject)
    }

    //
    // Methods
    protected fun addMigrationsProjectChangedListener(listener: (MigrationsProjectItem) -> Unit) {
        migrationsProjectChangedEvent += listener
    }

    protected fun addStartupProjectChangedListener(listener: (StartupProjectItem) -> Unit) {
        startupProjectChangedEvent += listener
    }

    protected fun addDbContextChangedListener(listener: (DbContextItem?) -> Unit) {
        dbContextChangedEvent += listener
    }

    override fun doOKAction() {
        super.doOKAction()

        val migrationsProject = commonOptions.migrationsProject
        val startupProject = commonOptions.startupProject

        if (migrationsProject != null && startupProject != null) {
            if (isSolutionLevelMode) {
                preferredProjectsManager.setGlobalProjectPair(migrationsProject, startupProject)
            } else {
                preferredProjectsManager.setProjectPair(migrationsProject, startupProject)
            }
        }
    }

    //
    // UI
    override fun createCenterPanel(): JComponent = createMainUI()

    protected fun createMainUI(): DialogPanel {
        return panel {
            panel {
                createPrimaryOptions()
                // TODO: Find better place to load preferences keeping component lifetime in mind
                initPreferredProjects()
                createDefaultMainRows()
                panel {
                    createAdditionalGroup()
                    createBuildOptions()
                }
            }
        }
    }

    protected open fun Panel.createDefaultMainRows() {
        createMigrationsProjectRow()
        createStartupProjectRow()
        if (requireDbContext) {
            createDbContextProjectRow()
        }
    }

    protected fun Panel.createMigrationsProjectRow() {
        row("Migrations project:") {
            iconComboBox(availableMigrationsProjects, { commonOptions.migrationsProject }, ::migrationsProjectSetter)
                .validationOnInput(validator.migrationsProjectValidation())
                .validationOnApply(validator.migrationsProjectValidation())
        }
    }

    protected fun Panel.createStartupProjectRow() {
        row("Startup project:") {
            iconComboBox(availableStartupProjects, { commonOptions.startupProject }, ::startupProjectSetter)
                .validationOnInput(validator.startupProjectValidation())
                .validationOnApply(validator.startupProjectValidation())
        }
    }

    protected fun Panel.createDbContextProjectRow() {
        row("DbContext class:") {
            iconComboBox(dbContextModel, { commonOptions.dbContext }, ::dbContextSetter)
                .validationOnInput(validator.dbContextValidation())
                .validationOnApply(validator.dbContextValidation())
        }
    }

    protected open fun Panel.createPrimaryOptions() {}

    protected open fun Panel.createAdditionalGroup() {}

    protected fun Panel.createBuildOptions() {
        groupRowsRange("Build Options") {
            var noBuildCheck: JBCheckBox? = null
            row {
                noBuildCheck = checkBox("Skip project build process (--no-build)")
                    .bindSelected(commonOptions::noBuild)
                    .component
            }

            row("Build configuration:") {
                iconComboBox(availableBuildConfigurations, { commonOptions.buildConfiguration }, ::buildConfigurationSetter)
                    .validationOnInput(validator.buildConfigurationValidation())
                    .validationOnApply(validator.buildConfigurationValidation())
            }.enabledIf(noBuildCheck!!.selected.not())

            row("Target framework:") {
                iconComboBox(targetFrameworkModel, { commonOptions.targetFramework }, ::targetFrameworkSetter)
                    .validationOnInput(validator.targetFrameworkValidation())
                    .validationOnInput(validator.targetFrameworkValidation())
            }.enabledIf(noBuildCheck!!.selected.not())
        }
    }

    //
    // Setters
    private fun migrationsProjectSetter(project: MigrationsProjectItem?) {
        if (project == commonOptions.migrationsProject) return

        commonOptions.migrationsProject = project
        migrationsProjectChangedEvent.invoke(commonOptions.migrationsProject!!)
    }

    private fun startupProjectSetter(project: StartupProjectItem?) {
        if (project == commonOptions.startupProject) return

        commonOptions.startupProject = project
        startupProjectChangedEvent.invoke(commonOptions.startupProject!!)
    }

    private fun dbContextSetter(context: DbContextItem?) {
        if (context == commonOptions.dbContext) return

        commonOptions.dbContext = context
        dbContextChangedEvent.invoke(commonOptions.dbContext)
    }

    private fun buildConfigurationSetter(configuration: BuildConfigurationItem?) {
        if (configuration == commonOptions.buildConfiguration) return

        commonOptions.buildConfiguration = configuration
    }

    private fun targetFrameworkSetter(framework: BaseTargetFrameworkItem?) {
        if (framework == commonOptions.targetFramework) return

        commonOptions.targetFramework = framework
    }

    //
    // Event listeners
    private fun migrationsProjectChanged(project: MigrationsProjectItem?) {
        dbContextModel.removeAllElements()

        if (project == null) return

        val dbContexts = beModel.getAvailableDbContexts.runUnderProgress(
            commonOptions.migrationsProject!!.displayName, intellijProject, "Loading DbContext classes...",
            isCancelable = true,
            throwFault = true
        )

        val dbContextIconItems = dbContexts!!.map { DbContextItem(it.name, it.fullName) }

        dbContextModel.addAll(dbContextIconItems)
        val firstDbContext = dbContextIconItems.firstOrNull()
        dbContextSetter(firstDbContext)
    }

    private fun startupProjectChanged(project: StartupProjectItem?) {
        targetFrameworkModel.removeAllElements()

        if (project == null) return

        val baseTargetFrameworkItems = project.data.targetFrameworks
            .map { TargetFrameworkItem(it, it) } as List<BaseTargetFrameworkItem>

        val defaultFramework = DefaultTargetFrameworkItem()

        targetFrameworkModel.addElement(defaultFramework)
        targetFrameworkModel.addAll(baseTargetFrameworkItems)
        commonOptions.targetFramework = defaultFramework
        targetFrameworkModel.selectedItem = commonOptions.targetFramework
    }

    private fun dbContextChanged(dbContext: DbContextItem?) {
        dbContextModel.selectedItem = dbContext
    }
}

