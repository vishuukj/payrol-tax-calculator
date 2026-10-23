$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force lib, build/classes, build/test-classes | Out-Null
    $dependencies = @{
        'sqlite-jdbc-3.51.0.0.jar' = 'org/xerial/sqlite-jdbc/3.51.0.0/sqlite-jdbc-3.51.0.0.jar'
    }
    foreach ($name in $dependencies.Keys) {
        $destination = Join-Path 'lib' $name
        if (!(Test-Path $destination)) {
            $temporary = "$destination.download"
            Invoke-WebRequest -UseBasicParsing -Uri ("https://repo.maven.apache.org/maven2/" + $dependencies[$name]) -OutFile $temporary
            Move-Item -LiteralPath $temporary -Destination $destination -Force
        }
    }
    $sources = @(Get-ChildItem src/main/java -Recurse -Filter *.java | ForEach-Object FullName)
    & javac --release 17 -cp 'lib/*' -d build/classes @sources
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
    Copy-Item src/main/resources/* build/classes -Recurse -Force
    & jar --create --file build/payroll.jar --main-class payroll.Main -C build/classes .
    if ($LASTEXITCODE -ne 0) { throw 'Jar packaging failed' }
    $tests = @(Get-ChildItem src/test/java -Recurse -Filter *.java | ForEach-Object FullName)
    & javac --release 17 -cp 'build/classes;lib/*' -d build/test-classes @tests
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
    & java -cp 'build/test-classes;build/classes;lib/*' payroll.PayrollTest
    if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
    Write-Host 'Build and tests passed.'
} finally { Pop-Location }
