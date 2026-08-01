Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-DotEnvValue {
    param(
        [Parameter(Mandatory)]
        [string]$Name
    )

    $escapedName = [regex]::Escape($Name)

    $line = Get-Content -Path ".env" |
        Where-Object {
            $_ -match "^\s*${escapedName}="
        } |
        Select-Object -Last 1

    if ($null -eq $line) {
        throw "Environment variable $Name was not found in .env"
    }

    $separatorIndex = $line.IndexOf("=")

    $value = $line.Substring($separatorIndex + 1).Trim()

    if (
        $value.Length -ge 2 -and
        (
            (
                $value.StartsWith('"') -and
                $value.EndsWith('"')
            ) -or
            (
                $value.StartsWith("'") -and
                $value.EndsWith("'")
            )
        )
    ) {
        $value = $value.Substring(
            1,
            $value.Length - 2
        )
    }

    return $value
}

function Convert-SecureStringToPlainText {
    param(
        [Parameter(Mandatory)]
        [Security.SecureString]$SecureValue
    )

    $pointer =
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR(
            $SecureValue
        )

    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR(
            $pointer
        )
    }
    finally {
        if ($pointer -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR(
                $pointer
            )
        }
    }
}

function Assert-Equal {
    param(
        [Parameter(Mandatory)]
        $Expected,

        [Parameter(Mandatory)]
        $Actual,

        [Parameter(Mandatory)]
        [string]$Message
    )

    if ($Expected -ne $Actual) {
        throw "$Message. Expected: $Expected. Actual: $Actual"
    }
}

function Assert-Contains {
    param(
        [Parameter(Mandatory)]
        [object[]]$Values,

        [Parameter(Mandatory)]
        [string]$Expected,

        [Parameter(Mandatory)]
        [string]$Message
    )

    if ($Values -notcontains $Expected) {
        $actualValues = $Values -join ", "

        throw "$Message. Expected: $Expected. Actual: $actualValues"
    }
}

function Wait-ForHealth {
    param(
        [Parameter(Mandatory)]
        [string]$ServiceName,

        [Parameter(Mandatory)]
        [string]$Uri
    )

    for ($attempt = 1; $attempt -le 60; $attempt++) {
        try {
            $health = Invoke-RestMethod `
                -Method Get `
                -Uri $Uri `
                -TimeoutSec 5

            if ([string]$health.status -eq "UP") {
                Write-Host `
                    "$ServiceName health is UP." `
                    -ForegroundColor Green

                return
            }
        }
        catch {
        }

        Start-Sleep -Seconds 1
    }

    throw "$ServiceName did not become healthy"
}

function Get-HttpStatus {
    param(
        [Parameter(Mandatory)]
        [string]$Method,

        [Parameter(Mandatory)]
        [string]$Uri,

        [hashtable]$Headers = @{},

        [string]$Body
    )

    $requestParameters = @{
        Method          = $Method
        Uri             = $Uri
        Headers         = $Headers
        UseBasicParsing = $true
        ErrorAction     = "Stop"
    }

    if (-not [string]::IsNullOrWhiteSpace($Body)) {
        $requestParameters.ContentType =
            "application/json"

        $requestParameters.Body = $Body
    }

    try {
        $response = Invoke-WebRequest @requestParameters

        return [int]$response.StatusCode
    }
    catch {
        if ($null -eq $_.Exception.Response) {
            throw
        }

        return [int]$_.Exception.Response.StatusCode
    }
}

function Invoke-IdentityDatabaseScalar {
    param(
        [Parameter(Mandatory)]
        [string]$Sql
    )

    $result = docker compose exec -T postgres `
        psql `
        -U $script:PostgresAdminUser `
        -d $script:IdentityDatabaseName `
        -At `
        -v ON_ERROR_STOP=1 `
        -c $Sql

    if ($LASTEXITCODE -ne 0) {
        throw "Identity database query failed"
    }

    return ($result | Out-String).Trim()
}

function Invoke-IdentityDatabaseCommand {
    param(
        [Parameter(Mandatory)]
        [string]$Sql
    )

    docker compose exec -T postgres `
        psql `
        -U $script:PostgresAdminUser `
        -d $script:IdentityDatabaseName `
        -v ON_ERROR_STOP=1 `
        -c $Sql |
        Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "Identity database command failed"
    }
}

cd C:\Projects\centerflow-erp

$script:PostgresAdminUser =
    Get-DotEnvValue `
        -Name "POSTGRES_ADMIN_USER"

$script:IdentityDatabaseName =
    Get-DotEnvValue `
        -Name "IDENTITY_DB_NAME"

$gatewayBaseUri = "http://localhost:8080"

$targetUserId = $null
$targetEmail = $null
$adminPassword = $null
$secureAdminPassword = $null
$bootstrapDisabled = $false

$adminCountSql = @"
SELECT COUNT(*)
FROM user_roles user_role
JOIN roles role
    ON role.id = user_role.role_id
WHERE role.name = 'ADMIN';
"@

$adminCount = [int](
    Invoke-IdentityDatabaseScalar `
        -Sql $adminCountSql
)

$bootstrapRequired = $adminCount -eq 0

if ($bootstrapRequired) {
    Write-Host `
        "No administrator exists. The first administrator will be created." `
        -ForegroundColor Yellow

    $adminEmail = Read-Host `
        "Enter the new administrator email"
}
else {
    Write-Host `
        "$adminCount administrator account(s) already exist." `
        -ForegroundColor Yellow

    $adminEmail = Read-Host `
        "Enter an existing administrator email"
}

$secureAdminPassword = Read-Host `
    "Enter the administrator password" `
    -AsSecureString

$adminPassword =
    Convert-SecureStringToPlainText `
        -SecureValue $secureAdminPassword

if ([string]::IsNullOrWhiteSpace($adminEmail)) {
    throw "Administrator email is required"
}

if ([string]::IsNullOrWhiteSpace($adminPassword)) {
    throw "Administrator password is required"
}

if ($bootstrapRequired) {
    if (
        $adminPassword.Length -lt 12 -or
        $adminPassword.Length -gt 64
    ) {
        throw "Administrator password must be between 12 and 64 characters"
    }

    if (
        $adminPassword -notmatch '\p{L}' -or
        $adminPassword -notmatch '\d'
    ) {
        throw "Administrator password must contain at least one letter and one number"
    }
}

try {
    if ($bootstrapRequired) {
        $env:IDENTITY_ADMIN_BOOTSTRAP_ENABLED =
            "true"

        $env:IDENTITY_ADMIN_BOOTSTRAP_EMAIL =
            $adminEmail

        $env:IDENTITY_ADMIN_BOOTSTRAP_PASSWORD =
            $adminPassword

        Write-Host `
            "`nStarting Identity Service with one-time administrator bootstrap..." `
            -ForegroundColor Cyan
    }
    else {
        $env:IDENTITY_ADMIN_BOOTSTRAP_ENABLED =
            "false"

        $env:IDENTITY_ADMIN_BOOTSTRAP_EMAIL =
            ""

        $env:IDENTITY_ADMIN_BOOTSTRAP_PASSWORD =
            ""
    }

    docker compose up `
        --detach `
        --build `
        --force-recreate `
        identity-service `
        api-gateway

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to rebuild Identity Service and API Gateway"
    }

    Wait-ForHealth `
        -ServiceName "Identity Service" `
        -Uri "http://localhost:8081/actuator/health"

    Wait-ForHealth `
        -ServiceName "API Gateway" `
        -Uri "http://localhost:8080/actuator/health"

    $adminLoginBody = @{
        email    = $adminEmail
        password = $adminPassword
    } | ConvertTo-Json

    $adminLogin = Invoke-RestMethod `
        -Method Post `
        -Uri "${gatewayBaseUri}/api/v1/auth/login" `
        -ContentType "application/json" `
        -Body $adminLoginBody

    $adminAccessToken =
        [string]$adminLogin.accessToken

    if ([string]::IsNullOrWhiteSpace($adminAccessToken)) {
        throw "Administrator access token was not returned"
    }

    Assert-Contains `
        -Values @($adminLogin.roles) `
        -Expected "ADMIN" `
        -Message "Administrator token does not contain ADMIN role"

    Write-Host `
        "Administrator login passed." `
        -ForegroundColor Green

    $adminHeaders = @{
        Authorization = "Bearer $adminAccessToken"
    }

    $suffix = [guid]::NewGuid().ToString("N").Substring(0, 10)

    $targetEmail =
        "rbac.runtime.${suffix}@example.com"

    $targetPassword =
        "RuntimeUser123"

    $registrationBody = @{
        email    = $targetEmail
        password = $targetPassword
    } | ConvertTo-Json

    $registration = Invoke-RestMethod `
        -Method Post `
        -Uri "${gatewayBaseUri}/api/v1/auth/register" `
        -ContentType "application/json" `
        -Body $registrationBody

    $targetUserId =
        [string]$registration.id

    if ([string]::IsNullOrWhiteSpace($targetUserId)) {
        throw "Temporary user ID was not returned"
    }

    Assert-Equal `
        -Expected "STUDENT" `
        -Actual ([string]$registration.role) `
        -Message "Registered user did not receive STUDENT role"

    Write-Host `
        "Temporary STUDENT user was registered." `
        -ForegroundColor Green

    $studentLoginBody = @{
        email    = $targetEmail
        password = $targetPassword
    } | ConvertTo-Json

    $studentLogin = Invoke-RestMethod `
        -Method Post `
        -Uri "${gatewayBaseUri}/api/v1/auth/login" `
        -ContentType "application/json" `
        -Body $studentLoginBody

    $studentAccessToken =
        [string]$studentLogin.accessToken

    Assert-Contains `
        -Values @($studentLogin.roles) `
        -Expected "STUDENT" `
        -Message "Initial user token does not contain STUDENT role"

    $studentHeaders = @{
        Authorization = "Bearer $studentAccessToken"
    }

    $studentFinanceStatus =
        Get-HttpStatus `
            -Method Get `
            -Uri "${gatewayBaseUri}/api/v1/finance/pricing-plans" `
            -Headers $studentHeaders

    Assert-Equal `
        -Expected 403 `
        -Actual $studentFinanceStatus `
        -Message "STUDENT should not access Finance APIs"

    Write-Host `
        "STUDENT finance restriction passed." `
        -ForegroundColor Green

    $roleAssignmentBody = @{
        roles = @(
            "ACCOUNTANT"
        )
    } | ConvertTo-Json

    $roleAssignment = Invoke-RestMethod `
        -Method Put `
        -Uri "${gatewayBaseUri}/api/v1/auth/admin/users/${targetUserId}/roles" `
        -Headers $adminHeaders `
        -ContentType "application/json" `
        -Body $roleAssignmentBody

    Assert-Contains `
        -Values @($roleAssignment.roles) `
        -Expected "ACCOUNTANT" `
        -Message "ACCOUNTANT role was not assigned"

    Write-Host `
        "ADMIN role assignment passed." `
        -ForegroundColor Green

    $oldStudentTokenStatus =
        Get-HttpStatus `
            -Method Get `
            -Uri "${gatewayBaseUri}/api/v1/finance/pricing-plans" `
            -Headers $studentHeaders

    Assert-Equal `
        -Expected 403 `
        -Actual $oldStudentTokenStatus `
        -Message "Previously issued STUDENT token should remain restricted"

    Write-Host `
        "Old STUDENT token remained restricted." `
        -ForegroundColor Green

    $accountantLogin = Invoke-RestMethod `
        -Method Post `
        -Uri "${gatewayBaseUri}/api/v1/auth/login" `
        -ContentType "application/json" `
        -Body $studentLoginBody

    $accountantAccessToken =
        [string]$accountantLogin.accessToken

    Assert-Contains `
        -Values @($accountantLogin.roles) `
        -Expected "ACCOUNTANT" `
        -Message "New token does not contain ACCOUNTANT role"

    $accountantHeaders = @{
        Authorization =
            "Bearer $accountantAccessToken"
    }

    $accountantFinanceStatus =
        Get-HttpStatus `
            -Method Get `
            -Uri "${gatewayBaseUri}/api/v1/finance/pricing-plans" `
            -Headers $accountantHeaders

    Assert-Equal `
        -Expected 200 `
        -Actual $accountantFinanceStatus `
        -Message "ACCOUNTANT should access Finance APIs"

    Write-Host `
        "ACCOUNTANT finance access passed." `
        -ForegroundColor Green

    $targetRolesSql = @"
SELECT STRING_AGG(role.name, ',' ORDER BY role.name)
FROM user_roles user_role
JOIN roles role
    ON role.id = user_role.role_id
WHERE user_role.user_id = '${targetUserId}';
"@

    $databaseRoles =
        Invoke-IdentityDatabaseScalar `
            -Sql $targetRolesSql

    Assert-Equal `
        -Expected "ACCOUNTANT" `
        -Actual $databaseRoles `
        -Message "Database role assignment is inconsistent"

    Write-Host `
        "Identity database role state is consistent." `
        -ForegroundColor Green

    $env:IDENTITY_ADMIN_BOOTSTRAP_ENABLED =
        "false"

    $env:IDENTITY_ADMIN_BOOTSTRAP_EMAIL =
        ""

    $env:IDENTITY_ADMIN_BOOTSTRAP_PASSWORD =
        ""

    docker compose up `
        --detach `
        --force-recreate `
        identity-service

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to disable administrator bootstrap"
    }

    Wait-ForHealth `
        -ServiceName "Identity Service" `
        -Uri "http://localhost:8081/actuator/health"

    $bootstrapDisabled = $true

    $containerEnvironment = @(
        docker inspect `
            --format '{{range .Config.Env}}{{println .}}{{end}}' `
            centerflow-identity-service
    )

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to inspect Identity Service environment"
    }

    $bootstrapEnabledLine =
        $containerEnvironment |
            Where-Object {
                $_ -like
                "IDENTITY_ADMIN_BOOTSTRAP_ENABLED=*"
            } |
            Select-Object -Last 1

    $bootstrapPasswordLine =
        $containerEnvironment |
            Where-Object {
                $_ -like
                "IDENTITY_ADMIN_BOOTSTRAP_PASSWORD=*"
            } |
            Select-Object -Last 1

    Assert-Equal `
        -Expected "IDENTITY_ADMIN_BOOTSTRAP_ENABLED=false" `
        -Actual $bootstrapEnabledLine `
        -Message "Administrator bootstrap is still enabled"

    Assert-Equal `
        -Expected "IDENTITY_ADMIN_BOOTSTRAP_PASSWORD=" `
        -Actual $bootstrapPasswordLine `
        -Message "Bootstrap password remains in the container environment"

    Write-Host `
        "Administrator bootstrap secret was removed." `
        -ForegroundColor Green

    $adminLoginAfterDisable = Invoke-RestMethod `
        -Method Post `
        -Uri "${gatewayBaseUri}/api/v1/auth/login" `
        -ContentType "application/json" `
        -Body $adminLoginBody

    Assert-Contains `
        -Values @($adminLoginAfterDisable.roles) `
        -Expected "ADMIN" `
        -Message "Administrator could not log in after disabling bootstrap"

    Write-Host `
        "Administrator persisted after bootstrap was disabled." `
        -ForegroundColor Green

    Write-Host `
        "`nRBAC runtime verification passed." `
        -ForegroundColor Green
}
catch {
    Write-Host `
        "`nRBAC runtime verification failed." `
        -ForegroundColor Red

    Write-Host $_.Exception.Message `
        -ForegroundColor Red

    throw
}
finally {
    if (
        -not [string]::IsNullOrWhiteSpace(
            $targetUserId
        )
    ) {
        try {
            $cleanupSql =
                "DELETE FROM users WHERE id = '${targetUserId}';"

            Invoke-IdentityDatabaseCommand `
                -Sql $cleanupSql

            Write-Host `
                "Temporary RBAC user was removed." `
                -ForegroundColor Yellow
        }
        catch {
            Write-Host `
                "Warning: temporary RBAC user cleanup failed." `
                -ForegroundColor Yellow
        }
    }

    if (-not $bootstrapDisabled) {
        $env:IDENTITY_ADMIN_BOOTSTRAP_ENABLED =
            "false"

        $env:IDENTITY_ADMIN_BOOTSTRAP_EMAIL =
            ""

        $env:IDENTITY_ADMIN_BOOTSTRAP_PASSWORD =
            ""

        try {
            docker compose up `
                --detach `
                --force-recreate `
                identity-service |
                Out-Null
        }
        catch {
            Write-Host `
                "Warning: Identity Service bootstrap cleanup failed." `
                -ForegroundColor Yellow
        }
    }

    Remove-Item `
        Env:IDENTITY_ADMIN_BOOTSTRAP_ENABLED `
        -ErrorAction SilentlyContinue

    Remove-Item `
        Env:IDENTITY_ADMIN_BOOTSTRAP_EMAIL `
        -ErrorAction SilentlyContinue

    Remove-Item `
        Env:IDENTITY_ADMIN_BOOTSTRAP_PASSWORD `
        -ErrorAction SilentlyContinue

    $adminPassword = $null
    $secureAdminPassword = $null
}