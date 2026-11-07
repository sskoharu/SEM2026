param(
    [string]$Image = 'sem2026-rag:cierre-verificado',
    [string]$RedisImage = 'redis:7.4-alpine',
    [string]$OpenSsl = 'openssl',
    [int]$Port = 18779
)

# Integration test only: private Docker network, ephemeral ACL and test CA.
# No production endpoint, password or Secret Manager access is used.
$ErrorActionPreference = 'Stop'
$name = 'sem2026-redis-test-' + [Guid]::NewGuid().ToString('N')
$backend = "$name-backend"
$redis = "$name-redis"
$directory = Join-Path ([IO.Path]::GetTempPath()) $name
$password = [Guid]::NewGuid().ToString('N')
$environmentNames = @('REDIS_PASSWORD', 'REDIS_USERNAME', 'REDIS_HOST', 'REDIS_PORT',
    'SPRING_DATA_REDIS_PASSWORD', 'SPRING_DATA_REDIS_URL', 'REDISCLI_AUTH')
$savedEnvironment = @{}
foreach ($key in $environmentNames) { $savedEnvironment[$key] = [Environment]::GetEnvironmentVariable($key) }

function Assert-Readiness([string]$Scenario, [int]$Expected) {
    $response = Invoke-WebRequest "http://127.0.0.1:$Port/actuator/health/readiness" -SkipHttpErrorCheck
    if ([int]$response.StatusCode -ne $Expected) { throw "Readiness failed in scenario $Scenario" }
    [pscustomobject]@{ Scenario = $Scenario; Endpoint = 'readiness'; Status = [int]$response.StatusCode; Expected = $Expected }
}

function Start-TestBackend {
    docker run -d --rm --pull=never --name $backend --network $name `
        -p "127.0.0.1:${Port}:8080" --mount "type=bind,source=$directory,target=/tls,readonly" `
        -e PORT=8080 -e LLM_API_KEY=synthetic-test-key -e QDRANT_API_KEY=synthetic-test-key `
        -e RAG_API_USERNAME=synthetic-test-user -e RAG_API_PASSWORD=synthetic-test-password `
        -e REDIS_PASSWORD -e REDIS_USERNAME -e REDIS_HOST -e REDIS_PORT `
        -e SPRING_DATA_REDIS_SSL_ENABLED=true -e SPRING_DATA_REDIS_SSL_BUNDLE=redistest `
        -e SPRING_DATA_REDIS_TIMEOUT=2s -e SPRING_DATA_REDIS_CONNECT_TIMEOUT=2s `
        -e SPRING_SSL_BUNDLE_PEM_REDISTEST_TRUSTSTORE_CERTIFICATE=file:/tls/redis.crt `
        -e SPRING_AI_VECTORSTORE_QDRANT_HOST=127.0.0.1 -e SPRING_AI_VECTORSTORE_QDRANT_PORT=1 `
        $Image | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Cannot start test backend.' }
    for ($attempt = 0; $attempt -lt 90; $attempt++) {
        try {
            $r = Invoke-WebRequest "http://127.0.0.1:$Port/actuator/health/liveness" -TimeoutSec 1
            if ($r.StatusCode -eq 200) { return }
        } catch { Start-Sleep -Milliseconds 500 }
    }
    throw 'Test backend liveness timeout.'
}

try {
    [IO.Directory]::CreateDirectory($directory) | Out-Null
    & $OpenSsl req -x509 -newkey rsa:2048 -nodes -keyout "$directory/redis.key" `
        -out "$directory/redis.crt" -days 1 -subj /CN=redis-test `
        -addext subjectAltName=DNS:redis-test *> "$directory/openssl.log"
    if ($LASTEXITCODE -ne 0) { throw 'Cannot generate test certificate.' }
    if (-not $IsWindows) {
        # Ephemeral test key must be readable by the Redis container UID on Linux.
        [IO.File]::SetUnixFileMode("$directory/redis.key", (
            [IO.UnixFileMode]::UserRead -bor [IO.UnixFileMode]::UserWrite -bor
            [IO.UnixFileMode]::GroupRead -bor [IO.UnixFileMode]::OtherRead))
    }
    $hash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($password))).ToLowerInvariant()
    [IO.File]::WriteAllText("$directory/users.acl", "user default off`nuser test-acl on #$hash ~* &* +@all`n")
    [IO.File]::WriteAllText("$directory/redis.conf", @"
port 0
tls-port 6379
tls-cert-file /tls/redis.crt
tls-key-file /tls/redis.key
tls-ca-cert-file /tls/redis.crt
tls-auth-clients no
aclfile /tls/users.acl
save ""
appendonly no
"@)
    docker network create $name | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Cannot create isolated test network.' }
    docker run -d --rm --pull=never --name $redis --network $name --network-alias redis-test `
        --mount "type=bind,source=$directory,target=/tls,readonly" $RedisImage redis-server /tls/redis.conf | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Cannot start TLS Redis.' }

    $env:REDISCLI_AUTH = $password
    $pong = docker exec -e REDISCLI_AUTH $redis redis-cli --tls --cacert /tls/redis.crt --user test-acl PING 2>$null
    if ($LASTEXITCODE -ne 0 -or $pong -ne 'PONG') { throw 'TLS ACL PING failed.' }
    $set = docker exec -e REDISCLI_AUTH $redis redis-cli --tls --cacert /tls/redis.crt --user test-acl SET smoke:roundtrip verified EX 60 2>$null
    $get = docker exec -e REDISCLI_AUTH $redis redis-cli --tls --cacert /tls/redis.crt --user test-acl GET smoke:roundtrip 2>$null
    if ($set -ne 'OK' -or $get -ne 'verified') { throw 'TLS ACL SET/GET failed.' }
    [pscustomobject]@{ Scenario = 'TLS, ACL user and authenticated data roundtrip'; Result = 'PASS' }

    $env:REDIS_HOST = 'redis-test'
    $env:REDIS_PORT = '6379'
    $env:REDIS_USERNAME = 'test-acl'
    $env:REDIS_PASSWORD = [Guid]::NewGuid().ToString('N')
    Start-TestBackend
    Assert-Readiness 'Rejected test password' 503
    $diagnostics = docker logs $backend 2>&1 | Out-String
    if ($diagnostics -notmatch 'WRONGPASS') { throw 'Expected WRONGPASS was not observed in the isolated test.' }
    $diagnostics = $null
    docker stop $backend | Out-Null

    $env:REDIS_PASSWORD = $password
    Start-TestBackend
    Assert-Readiness 'Correct test ACL password and verified TLS' 200
    docker stop $redis | Out-Null
    Assert-Readiness 'Redis stopped after healthy connection' 503
    $live = Invoke-WebRequest "http://127.0.0.1:$Port/actuator/health/liveness"
    if ($live.StatusCode -ne 200) { throw 'Redis outage must not fail liveness.' }
    [pscustomobject]@{ Scenario = 'Redis outage'; Endpoint = 'liveness'; Status = 200; Expected = 200 }
} finally {
    docker stop $backend $redis 2>$null | Out-Null
    docker network rm $name 2>$null | Out-Null
    foreach ($key in $environmentNames) { [Environment]::SetEnvironmentVariable($key, $savedEnvironment[$key]) }
    foreach ($file in @('redis.key', 'redis.crt', 'users.acl', 'redis.conf', 'openssl.log')) {
        $path = Join-Path $directory $file
        if ([IO.File]::Exists($path)) { [IO.File]::Delete($path) }
    }
    if ([IO.Directory]::Exists($directory)) { [IO.Directory]::Delete($directory) }
}
