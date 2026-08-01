param(
    [Parameter(Mandatory)]
    [string[]]$ComposeFiles
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$healthEndpoints = [ordered]@{
    "identity-service" =
        "http://localhost:8081/actuator/health"

    "academic-service" =
        "http://localhost:8082/actuator/health"

    "enrollment-service" =
        "http://localhost:8083/actuator/health"

    "finance-service" =
        "http://localhost:8084/actuator/health"

    "notification-service" =
        "http://localhost:8085/actuator/health"

    "api-gateway" =
        "http://localhost:8080/actuator/health"
}

foreach ($healthEndpoint in $healthEndpoints.GetEnumerator()) {
    $serviceName =
        [string]$healthEndpoint.Key

    $healthUri =
        [string]$healthEndpoint.Value

    $serviceReady = $false

    Write-Host `
        "Waiting for $serviceName..." `
        -ForegroundColor Cyan

    for ($attempt = 1; $attempt -le 120; $attempt++) {
        try {
            $healthResponse =
                Invoke-RestMethod `
                    -Method Get `
                    -Uri $healthUri `
                    -TimeoutSec 5

            if ([string]$healthResponse.status -eq "UP") {
                $serviceReady = $true

                Write-Host `
                    "$serviceName health is UP." `
                    -ForegroundColor Green

                break
            }
        }
        catch {
            # The service may still be starting.
        }

        Start-Sleep -Seconds 1
    }

    if (-not $serviceReady) {
        Write-Host `
            "$serviceName did not become healthy." `
            -ForegroundColor Red

        & docker compose @ComposeFiles ps

        & docker compose @ComposeFiles logs `
            --tail 100 `
            $serviceName

        throw "$serviceName readiness check failed"
    }
}

Write-Host `
    "All CenterFlow services are ready." `
    -ForegroundColor Green