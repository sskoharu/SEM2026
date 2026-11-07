param(
    [Parameter(Mandatory = $true)][string]$BaseUrl,
    [string]$Csv = "$PSScriptRoot\pruebas_funcionales.csv",
    [string]$Salida = "$PSScriptRoot\resultados_relatos.csv",
    [int]$PausaSegundos = 7
)

# Ejecuta los casos con relato (los que tienen la columna 'relato') contra POST /api/v1/claims/start.
# La pausa de 7 s mantiene el ritmo bajo el limite de 10 peticiones por 60 s.
$casos = Import-Csv -Path $Csv -Encoding UTF8 | Where-Object { $_.relato }
$resultados = @()

foreach ($c in $casos) {
    $cuerpo = [System.Text.Encoding]::UTF8.GetBytes((@{ incidentDescription = $c.relato } | ConvertTo-Json -Compress))
    $http = 0
    $texto = ""
    try {
        $r = Invoke-WebRequest -Uri "$BaseUrl/api/v1/claims/start" -Method Post -Body $cuerpo `
            -ContentType "application/json; charset=utf-8" -UseBasicParsing -TimeoutSec 120
        $http = [int]$r.StatusCode
        $texto = $r.Content
    } catch {
        if ($_.Exception.Response) {
            $http = [int]$_.Exception.Response.StatusCode
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $texto = $reader.ReadToEnd()
        } else {
            $texto = $_.Exception.Message
        }
    }

    $ok = ($http -eq [int]$c.http_esperado)
    Write-Host ("{0}  esperado={1} obtenido={2}  {3}" -f $c.id, $c.http_esperado, $http, $(if ($ok) { "HTTP_OK" } else { "HTTP_DISTINTO" }))

    $resultados += [pscustomobject]@{
        id                 = $c.id
        http_esperado      = $c.http_esperado
        http_obtenido      = $http
        coincide_http      = $ok
        resultado_obtenido = ($texto -replace "\s+", " ")
    }
    Start-Sleep -Seconds $PausaSegundos
}

$resultados | Export-Csv -Path $Salida -NoTypeInformation -Encoding UTF8
Write-Host "Resultados guardados en $Salida. Revisa 'resultado_obtenido' y pasa el estado final a pruebas_funcionales.csv."
