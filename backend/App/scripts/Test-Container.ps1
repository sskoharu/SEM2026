param(
    [string]$Image = 'sem2026-rag:cierre-verificado',
    [int]$Port = 18778
)

$ErrorActionPreference = 'Stop'
$name = 'sem2026-smoke-' + [Guid]::NewGuid().ToString('N')
$password = [Guid]::NewGuid().ToString('N')
$basic = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("smoke:$password"))

function Assert-Status([string]$Path, [int]$Expected, [hashtable]$Options = @{}) {
    $response = Invoke-WebRequest "http://127.0.0.1:$Port$Path" -SkipHttpErrorCheck @Options
    if ([int]$response.StatusCode -ne $Expected) {
        throw "$Path returned $($response.StatusCode); expected $Expected"
    }
    [pscustomobject]@{ Path = $Path; Status = [int]$response.StatusCode; Expected = $Expected }
}

try {
    docker run -d --rm --pull=never --name $name -p "127.0.0.1:${Port}:8080" `
        -e PORT=8080 -e LLM_API_KEY=synthetic-test-key -e QDRANT_API_KEY=synthetic-test-key `
        -e REDIS_PASSWORD=synthetic-test-password -e RAG_API_USERNAME=smoke `
        -e "RAG_API_PASSWORD=$password" -e SPRING_AI_VECTORSTORE_QDRANT_HOST=127.0.0.1 `
        -e SPRING_AI_VECTORSTORE_QDRANT_PORT=1 -e SPRING_DATA_REDIS_HOST=127.0.0.1 `
        -e SPRING_DATA_REDIS_PORT=1 $Image | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Container could not start.' }
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        try {
            $response = Invoke-WebRequest "http://127.0.0.1:$Port/actuator/health/liveness" -TimeoutSec 1
            if ($response.StatusCode -eq 200) { $ready = $true; break }
        } catch { Start-Sleep -Milliseconds 500 }
    }
    if (-not $ready) { throw 'Liveness did not become available.' }
    Assert-Status '/actuator/health/liveness' 200
    # Redis deliberately unavailable: readiness must retain the dependency check.
    Assert-Status '/actuator/health/readiness' 503
    Assert-Status '/api/v1/rag/search?q=garantia' 401
    Assert-Status '/api/v1/rag/query' 401 @{ Method = 'Post'; ContentType = 'application/json'; Body = '{"pregunta":""}' }
    Assert-Status '/api/v1/rag/query' 400 @{
        Method = 'Post'; ContentType = 'application/json'; Body = '{"pregunta":""}'
        Headers = @{ Authorization = "Basic $basic" }
    }
} finally {
    docker stop $name 2>$null | Out-Null
}
