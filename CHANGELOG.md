<h2>1.3.2</h2>
<p>
  <ul>
    <li>Fix: projects with platform-specific target frameworks not presented in dropdowns</li>
  </ul>
</p>
<h2>1.3.1</h2>
<p>
  <ul>
    <li>Enable support for Rider 2022.1</li>
  </ul>
</p>
<h2>1.3.0</h2>
<p>
  <ul>
    <li>Add ability to open EF Core action under Tools application menu entry</li>
    <li>Add EF Core Quick Actions window</li>
    <li>Improve Startup project detection logic (#59 by @kolosovpetro)</li>
    <li>Remove migration parent folder after Remove Last Migration if there are no migrations (#51 by @kolosovpetro)</li>
    <li>Show only migrations related to selected DbContext in Update Database's Target migration autocompletion (#50 by @kolosovpetro)</li>
  </ul>
</p>
<h2>1.2.1</h2>
<p>
  <ul>
    <li>Fix: NoSuchMethodError using the new DSL v2</li>
    <li>Fix: Disabled optional fields require validation</li>
  </ul>
</p>
<h2>1.2.0</h2>
<p>
  <ul>
    <li>Implement DbContext Scaffolding</li>
    <li>Add output folder for Add Migration dialog (#44 by @kolosovpetro)</li>
    <li>Make Target framework optional (#41 by @kolosovpetro)</li>
  </ul>
</p>
<h2>1.1.3</h2>
<p>
  <ul>
    <li>Fix: Unable to run any action under Mac OS X</li>
  </ul>
</p>
<h2>1.1.2</h2>
<p>
  <ul>
    <li>Fix: Duplicated items in dropdowns when projects has multiple target framework</li>
    <li>Fix: Duplicated items in the build configuration dropdown when solution has multiple target platforms</li>
    <li>Fix: Exceptions when trying to open any action with no build configuration in solution</li>
  </ul>
</p>
<h2>1.1.1</h2>
<p>
  <ul>
    <li>Fix: Projects with whitespaces in project file path are recognized incorrectly</li>
    <li>Fix: Default Update database target migration is selected incorrectly</li>
  </ul>
</p>
<h2>1.1.0</h2>
<p>
  <ul>
    <li>General: Support .NET Core 3.1 and .NET Standard 2.1 projects (#30 by @kolosovpetro)</li>
    <li>Upgrade to Rider 2021.3 in stable channel</li>
  </ul>
</p>
<h2>1.0.0</h2>
<p>
  <ul>
    <li>Creating migrations</li>
    <li>Removing last created migration</li>
    <li>Persisting selected migrations and startup projects between dialogs</li>
    <li>Suggesting installing dotnet `ef command` line tools if not installed (when opening solution that contains EF Core related projects)</li>
    <li>Deleting used database</li>
  </ul>
</p>
