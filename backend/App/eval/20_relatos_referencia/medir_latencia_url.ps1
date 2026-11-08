param(
    [string]$BaseUrl = "https://asistente-inteligente-4kiq.onrender.com",
    [int]$PausaSeg = 7
)
# Mide la latencia de POST /api/v1/claims/start para los 20 casos de casos.csv.
# Solo registra tiempos y estado HTTP; la correccion funcional se evalua en resultados_url_publica_*.csv.
$casos = Import-Csv (Join-Path $PSScriptRoot "casos.csv") -Encoding UTF8
$fecha = Get-Date -Format "yyyy-MM-dd"
$salida = Join-Path $PSScriptRoot "latencias_url_publica_$fecha.csv"
$filas = @()
foreach ($c in $casos) {
    $cuerpo = @{ incidentDescription = $c.relato } | ConvertTo-Json -Compress
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($cuerpo)
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $http = 0
    try {
        $r = Invoke-WebRequest -Uri "$BaseUrl/api/v1/claims/start" -Method Post -Body $bytes `
            -ContentType "application/json; charset=utf-8" -UseBasicParsing -TimeoutSec 180
        $http = [int]$r.StatusCode
    } catch {
        if ($_.Exception.Response) { $http = [int]$_.Exception.Response.StatusCode }
    }
    $sw.Stop()
    $filas += [pscustomobject]@{ id = $c.id; http = $http; ms = $sw.ElapsedMilliseconds }
    Write-Host "$($c.id) http=$http ms=$($sw.ElapsedMilliseconds)"
    Start-Sleep -Seconds $PausaSeg
}
$filas | Export-Csv $salida -NoTypeInformation -Encoding UTF8
function Resumen($etq, $datos) {
    $ms = @($datos | ForEach-Object { $_.ms } | Sort-Object)
    $n = $ms.Count
    if ($n -eq 0) { return }
    $p95 = $ms[[math]::Min($n - 1, [math]::Ceiling(0.95 * $n) - 1)]
    $med = $ms[[math]::Floor(($n - 1) / 2)]
    Write-Host "$etq n=$n mediana=$med p95=$p95 max=$($ms[-1]) (ms)"
}
Resumen "todos(200/400)" ($filas | Where-Object { $_.http -in 200, 400 })
Resumen "completos(200)" ($filas | Where-Object { $_.http -eq 200 })
Resumen "rechazos(400)" ($filas | Where-Object { $_.http -eq 400 })
