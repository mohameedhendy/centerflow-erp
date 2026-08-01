Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot =
    Split-Path `
        -Parent `
        $PSScriptRoot

Set-Location $projectRoot

function Wait-ForGatewayHealth {
    for ($attempt = 1; $attempt -le 90; $attempt++) {
        try {
            $health = Invoke-RestMethod `
                -Method Get `
                -Uri "http://localhost:8080/actuator/health" `
                -TimeoutSec 5

            if ([string]$health.status -eq "UP") {
                Write-Host `
                    "API Gateway health is UP." `
                    -ForegroundColor Green

                return
            }
        }
        catch {
        }

        Start-Sleep -Seconds 1
    }

    throw "API Gateway did not become healthy"
}

function Get-HttpStatus {
    param(
        [Parameter(Mandatory)]
        [string]$Method,

        [Parameter(Mandatory)]
        [string]$Uri,

        [string]$Body
    )

    $parameters = @{
        Method      = $Method
        Uri         = $Uri
        ErrorAction = "Stop"
    }

    if (-not [string]::IsNullOrWhiteSpace($Body)) {
        $parameters.ContentType =
            "application/json"

        $parameters.Body = $Body
    }

    try {
        $response =
            Invoke-WebRequest @parameters

        return [int]$response.StatusCode
    }
    catch {
        if ($null -eq $_.Exception.Response) {
            throw
        }

        return [int]$_.Exception.Response.StatusCode
    }
}

function Assert-PortClosed {
    param(
        [Parameter(Mandatory)]
        [int]$Port,

        [Parameter(Mandatory)]
        [string]$Description
    )

    $portIsOpen =
        Test-NetConnection `
            -ComputerName "localhost" `
            -Port $Port `
            -InformationLevel Quiet `
            -WarningAction SilentlyContinue

    if ($portIsOpen) {
        throw "$Description is still exposed on localhost:$Port"
    }

    Write-Host `
        "$Description is isolated from localhost:$Port." `
        -ForegroundColor Green
}

function Assert-NoPublishedPort {
    param(
        [Parameter(Mandatory)]
        [string]$Service,

        [Parameter(Mandatory)]
        [int]$ContainerPort
    )

    $containerId = (
        & docker compose ps `
            --quiet `
            $Service |
            Out-String
    ).Trim()

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to inspect $Service container"
    }

    if ([string]::IsNullOrWhiteSpace($containerId)) {
        throw "No running container was found for $Service"
    }

    $portsJson = (
        & docker inspect `
            --format '{{json .NetworkSettings.Ports}}' `
            $containerId |
            Out-String
    ).Trim()

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to inspect network ports for $Service"
    }

    $ports = $portsJson | ConvertFrom-Json

    $containerPortKey = "$ContainerPort/tcp"

    $portProperty = $ports.PSObject.Properties |
        Where-Object {
            $_.Name -eq $containerPortKey
        } |
        Select-Object -First 1

    if ($null -eq $portProperty) {
        throw "$Service does not expose container port $ContainerPort"
    }

    $publishedBindings = $portProperty.Value

    if ($null -ne $publishedBindings) {
        $bindingDescriptions = @(
            $publishedBindings |
                ForEach-Object {
                    "$($_.HostIp):$($_.HostPort)"
                }
        )

        $bindingDescriptionsText =
            $bindingDescriptions -join ", "

        throw "$Service publishes port $ContainerPort as $bindingDescriptionsText"
    }

    Write-Host `
        "$Service has no published host port." `
        -ForegroundColor Green
}

Write-Host `
    "Validating secure Docker Compose configuration..." `
    -ForegroundColor Cyan

& docker compose config --quiet

if ($LASTEXITCODE -ne 0) {
    throw "Secure Docker Compose configuration is invalid"
}

Write-Host `
    "Validating development override configuration..." `
    -ForegroundColor Cyan

& docker compose `
    -f compose.yaml `
    -f compose.dev.yaml `
    config `
    --quiet

if ($LASTEXITCODE -ne 0) {
    throw "Development Docker Compose configuration is invalid"
}

Write-Host `
    "Removing the previously published stack..." `
    -ForegroundColor Cyan

& docker compose down --remove-orphans

if ($LASTEXITCODE -ne 0) {
    throw "Unable to stop the existing Docker Compose stack"
}

Write-Host `
    "Starting the isolated stack..." `
    -ForegroundColor Cyan

& docker compose up `
    --detach `
    --build `
    --force-recreate

if ($LASTEXITCODE -ne 0) {
    throw "Unable to start the isolated Docker Compose stack"
}

Wait-ForGatewayHealth

$expectedServices = @(
    "postgres",
    "identity-service",
    "academic-service",
    "enrollment-service",
    "finance-service",
    "notification-service",
    "api-gateway"
)

$runningServices =
    @(
        & docker compose ps `
            --status running `
            --services
    )

if ($LASTEXITCODE -ne 0) {
    throw "Unable to inspect running Docker Compose services"
}

foreach ($service in $expectedServices) {
    if ($runningServices -notcontains $service) {
        throw "$service is not running"
    }

    Write-Host `
        "$service is running." `
        -ForegroundColor Green
}

$gatewayMapping =
    (
        & docker compose port `
            api-gateway `
            8080 |
            Out-String
    ).Trim()

if ($LASTEXITCODE -ne 0) {
    throw "Unable to inspect the API Gateway port"
}

if ([string]::IsNullOrWhiteSpace($gatewayMapping)) {
    throw "API Gateway port 8080 is not published"
}

Write-Host `
    "API Gateway is published as $gatewayMapping." `
    -ForegroundColor Green

Assert-NoPublishedPort `
    -Service "postgres" `
    -ContainerPort 5432

Assert-NoPublishedPort `
    -Service "identity-service" `
    -ContainerPort 8081

Assert-NoPublishedPort `
    -Service "academic-service" `
    -ContainerPort 8082

Assert-NoPublishedPort `
    -Service "enrollment-service" `
    -ContainerPort 8083

Assert-NoPublishedPort `
    -Service "finance-service" `
    -ContainerPort 8084

Assert-NoPublishedPort `
    -Service "notification-service" `
    -ContainerPort 8085

Assert-PortClosed `
    -Port 5433 `
    -Description "PostgreSQL"

Assert-PortClosed `
    -Port 8081 `
    -Description "Identity Service"

Assert-PortClosed `
    -Port 8082 `
    -Description "Academic Service"

Assert-PortClosed `
    -Port 8083 `
    -Description "Enrollment Service"

Assert-PortClosed `
    -Port 8084 `
    -Description "Finance Service"

Assert-PortClosed `
    -Port 8085 `
    -Description "Notification Service"

$invalidLoginBody = @{
    email =
        "missing.user@centerflow.local"

    password =
        "InvalidPassword123"
} | ConvertTo-Json

$loginStatus =
    Get-HttpStatus `
        -Method "POST" `
        -Uri "http://localhost:8080/api/v1/auth/login" `
        -Body $invalidLoginBody

if ($loginStatus -ne 401) {
    throw "Gateway-to-Identity routing returned HTTP $loginStatus instead of 401"
}

Write-Host `
    "Gateway can reach the isolated Identity Service." `
    -ForegroundColor Green

Write-Host `
    "`nService isolation verification passed." `
    -ForegroundColor Green