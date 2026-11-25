param(
    [string]$Mysql = 'C:/laragon/bin/mysql/mysql-8.0.30-winx64/bin/mysql.exe',
    [int]$PuertoBase = 18080,
    [string]$MysqlUsuario = 'root',
    [string]$Java = 'java.exe',
    [Alias('ApkDebug')][string]$ApkAcademico = '',
    [switch]$RedInvertida,
    [switch]$Docker,
    [switch]$EvidenciaVisual,
    [switch]$EscrituraMovil
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$marca = [Guid]::NewGuid().ToString('N').Substring(0, 10)
$prefijo = "induztek_it_${marca}_"
$evidencia = Join-Path $env:TEMP "induztek-integracion-$marca"
[IO.Directory]::CreateDirectory($evidencia) | Out-Null
$procesos = @()
$dockerCli = $null
$proyecto = "induztek-qa-$marca"
$enlace = "http://127.0.0.1:$PuertoBase"
$modulos = [ordered]@{
    'eureka-server' = $PuertoBase + 681
    'ms-usuarios' = $PuertoBase + 1
    'ms-equipos' = $PuertoBase + 2
    'ms-instrumentos' = $PuertoBase + 3
    'ms-protocolos' = $PuertoBase + 4
    'api-gateway' = $PuertoBase
}

function Nueva-Clave {
    $bytes = New-Object byte[] 9
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    "Ind!$([Convert]::ToBase64String($bytes).Replace("+", "-").Replace("/", "_"))"
}

function Api($metodo, $ruta, $esperado, $token, $cuerpo) {
    $parametros = @{ Uri="$enlace$ruta"; Method=$metodo; UseBasicParsing=$true; TimeoutSec=12 }
    if ($token) { $parametros.Headers = @{Authorization="Bearer $token"} }
    if ($null -ne $cuerpo) {
        $parametros.Body = [Text.Encoding]::UTF8.GetBytes(($cuerpo | ConvertTo-Json -Depth 12 -Compress))
        $parametros.ContentType = 'application/json; charset=utf-8'
    }
    try { $r = Invoke-WebRequest @parametros }
    catch {
        $respuesta = $_.Exception.Response
        if (!$respuesta -or [int]$respuesta.StatusCode -ne $esperado) {
            $detalle = $_.ErrorDetails.Message
            throw "HTTP $metodo $ruta esperaba $esperado y obtuvo $([int]$respuesta.StatusCode). $detalle"
        }
        Write-Host "$metodo $ruta -> $esperado"
        return
    }
    if ([int]$r.StatusCode -ne $esperado) { throw "HTTP $metodo $ruta devolvio $($r.StatusCode), esperaba $esperado" }
    Write-Host "$metodo $ruta -> $esperado"
    if ($r.Content.Length -gt 0) { $r.Content | ConvertFrom-Json }
}

function Renovar-Sesion($correo, $temporal, [switch]$ConClave) {
    $inicio = Api POST '/api/auth/login' 200 $null @{correo=$correo;contrasena=$temporal}
    if (!$inicio.requiereCambioContrasena) { throw 'La primera sesion debe exigir cambio obligatorio' }
    $null = Api GET '/api/usuarios/me' 403 $inicio.token $null
    $nueva = Nueva-Clave
    $null = Api PATCH '/api/usuarios/me/contrasena' 204 $inicio.token @{actual=$temporal;nueva=$nueva;confirmar=$nueva}
    $renovada = Api POST '/api/auth/login' 200 $null @{correo=$correo;contrasena=$nueva}
    if ($renovada.requiereCambioContrasena -or !$renovada.token) { throw 'La sesion no quedo habilitada' }
    if ($ConClave) { return @{Token=$renovada.token; Clave=$nueva} }
    $renovada.token
}

function Esperar-Puerto([int]$puerto, [Diagnostics.Process]$proceso) {
    $vence = [DateTime]::UtcNow.AddSeconds(80)
    while ([DateTime]::UtcNow -lt $vence) {
        if ($proceso.HasExited) { throw "El proceso del puerto $puerto finalizo. Revisa $evidencia" }
        $cliente = New-Object Net.Sockets.TcpClient
        try {
            $cliente.Connect('127.0.0.1', $puerto)
            return
        } catch { Start-Sleep -Milliseconds 650 }
        finally { $cliente.Dispose() }
    }
    throw "El puerto $puerto no estuvo listo. Revisa $evidencia"
}

try {
    $jwt = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($jwt) } finally { $rng.Dispose() }
    $env:JWT_SECRET = [Convert]::ToBase64String($jwt)
    $claves = @{}
    foreach ($id in 1..5) {
        $claves[$id] = Nueva-Clave
        Set-Item -Path "Env:DEMO_PASSWORD_$id" -Value $claves[$id]
    }
    if ($Docker) {
        $dockerCli = (Get-Command docker.exe -ErrorAction Stop).Source
        $env:DB_PASSWORD = Nueva-Clave
        $env:MYSQL_ROOT_PASSWORD = Nueva-Clave
        $env:GATEWAY_PORT = $PuertoBase.ToString()
        $env:SPRING_PROFILES_ACTIVE = 'seed'
        $argsCompose = @('compose', '--ansi', 'never', '-p', $proyecto)
        $argsInicio = $argsCompose + @('--progress', 'plain', 'up', '--build', '--wait', '--wait-timeout', '240')
        $arranque = Start-Process $dockerCli -ArgumentList $argsInicio -WorkingDirectory $repo -Wait -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $evidencia 'compose-up.log') -RedirectStandardError (Join-Path $evidencia 'compose-up.err')
        if ($arranque.ExitCode -ne 0) { throw "Compose no terminó el arranque. Revisa $evidencia" }
        $vence = [DateTime]::UtcNow.AddSeconds(60)
        $disponible = $false
        do {
            try {
                $null = Invoke-WebRequest -UseBasicParsing -Uri "$enlace/api/usuarios/me" -TimeoutSec 5
            } catch {
                $disponible = [int]$_.Exception.Response.StatusCode -eq 401
            }
            if (!$disponible) { Start-Sleep -Seconds 2 }
        } while (!$disponible -and [DateTime]::UtcNow -lt $vence)
        if (!$disponible) { throw 'El Gateway de Compose no resolvió Usuarios' }
        Write-Host "Proyecto Docker listo: $proyecto"
    } else {
        $sql = foreach ($archivo in Get-ChildItem (Join-Path $repo 'database') -Filter '*.sql' | Where-Object Name -lt '090' | Sort-Object Name) {
            (Get-Content -Encoding utf8 -Raw $archivo.FullName) -replace '\binduztek_(usuarios|equipos|instrumentos|protocolos)\b', ($prefijo + '$1')
        }
        $OutputEncoding = [Text.UTF8Encoding]::new($false)
        $sql | & $Mysql --no-defaults --protocol=TCP --host=127.0.0.1 "--user=$MysqlUsuario" --default-character-set=utf8mb4
        if ($LASTEXITCODE -ne 0) { throw 'Error al preparar esquemas aislados de MySQL' }
        Write-Host "Esquemas de prueba: $prefijo*"

        $env:DB_USERNAME = $MysqlUsuario
        $env:DB_PASSWORD = $env:MYSQL_PWD
        $env:EUREKA_URL = "http://127.0.0.1:$($modulos['eureka-server'])/eureka/"
        $env:EUREKA_INSTANCE_PREFER_IP_ADDRESS = 'true'
        $env:EUREKA_CLIENT_REGISTRY_FETCH_INTERVAL_SECONDS = '3'
        $env:EUREKA_SERVER_RESPONSE_CACHE_UPDATE_INTERVAL_MS = '2000'
        foreach ($db in 'usuarios','equipos','instrumentos','protocolos') {
            Set-Item -Path "Env:DB_$($db.ToUpperInvariant())_URL" -Value "jdbc:mysql://127.0.0.1:3306/$prefijo$db"
        }
        foreach ($modulo in $modulos.Keys) {
            $env:SERVER_PORT = $modulos[$modulo].ToString()
            $env:SPRING_PROFILES_ACTIVE = if ($modulo -eq 'ms-usuarios') { 'seed' } else { 'default' }
            $jar = Get-ChildItem (Join-Path $repo "$modulo/target") -Filter '*.jar' | Select-Object -First 1
            if (!$jar) { throw "Empaqueta $modulo antes de ejecutar esta prueba" }
            $jarPrueba = Join-Path $evidencia $jar.Name
            Copy-Item -LiteralPath $jar.FullName -Destination $jarPrueba
            $p = Start-Process $Java -ArgumentList @('-Xmx256m', '-jar', ('"' + $jarPrueba + '"')) -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $evidencia "$modulo.log") -RedirectStandardError (Join-Path $evidencia "$modulo.err")
            $procesos += $p
            Esperar-Puerto $modulos[$modulo] $p
        }
        $vence = [DateTime]::UtcNow.AddSeconds(45)
        do {
            try {
                $registro = Invoke-RestMethod -Uri ($env:EUREKA_URL + 'apps') -Headers @{Accept='application/json'} -TimeoutSec 5
                $nombres = @($registro.applications.application | ForEach-Object name)
                if (@('MS-USUARIOS','MS-EQUIPOS','MS-INSTRUMENTOS','MS-PROTOCOLOS','API-GATEWAY' | Where-Object { $_ -notin $nombres }).Count -eq 0) { break }
            } catch {}
            Start-Sleep -Seconds 2
        } while ([DateTime]::UtcNow -lt $vence)
        if (@('MS-USUARIOS','MS-EQUIPOS','MS-INSTRUMENTOS','MS-PROTOCOLOS','API-GATEWAY' | Where-Object { $_ -notin $nombres }).Count -gt 0) { throw 'Registro de Eureka incompleto' }
        Write-Host "Eureka: $($nombres -join ', ')"
        Start-Sleep -Seconds 7
    }

    $sesionJefe = Renovar-Sesion 'jefe@gmail.com' $claves[4] -ConClave
    $jefe = $sesionJefe.Token
    $sesionTecnico = Renovar-Sesion 'terreno@gmail.com' $claves[1] -ConClave
    $tecnico = $sesionTecnico.Token
    $sesionIngeniero = Renovar-Sesion 'ingeniero@gmail.com' $claves[3] -ConClave
    $ingeniero = $sesionIngeniero.Token
    $correo = "demo-$marca@example.invalid"
    $cliente = Api POST '/api/clientes' 201 $jefe @{nombre='Empresa Demostracion';rut='12.345.678-5';correo=$correo;giro='Servicios'}
    $temporalCliente = 'Induztek2026!'
    $null = Api PATCH "/api/clientes/$($cliente.id)/activar" 200 $jefe @{contrasena=$temporalCliente}
    $inst = Api POST '/api/instalaciones' 201 $jefe @{nombre='Planta Integracion';idCliente=$cliente.id}
    $eq = Api POST '/api/equipos' 201 $jefe @{tag='TR-INTEGRACION';tipoEquipo='Transformador';marca='Demostracion';serie='DEMO-1';instalacionId=$inst.id}
    $listaTecnico = Api GET "/api/instalaciones/$($inst.id)/equipos" 200 $tecnico $null
    if (@($listaTecnico.content.id) -notcontains $eq.id) { throw 'El Técnico no encontró el equipo de la planta' }
    $null = Api GET "/api/equipos/$($eq.id)" 200 $tecnico $null
    $ins = Api POST '/api/instrumentos' 201 $jefe @{nombre='Megohmetro Demo';marca='Demostracion';serie='DEMO-2';fechaVencimiento='31-12-2099'}
    $pro = Api POST '/api/protocolos' 201 $tecnico @{equipoId=$eq.id;instrumentoId=$ins.id;temperatura=20;humedad=50;tipoPrueba='AISLACION';valores=@{lectura1Min=10;lectura10Min=20}}
    $null = Api PATCH "/api/protocolos/$($pro.id)/cerrar" 200 $tecnico $null
    $edicion = @{equipoId=$eq.id;instrumentoId=$ins.id;temperatura=20;humedad=22;tipoPrueba='AISLACION';
        valores=@{lectura1Min=10;lectura10Min=20};
        textoIngresado=@{temperatura='020';humedad='22.0';lectura1Min='10.00';lectura10Min='20'}}
    $null = Api PUT "/api/protocolos/$($pro.id)" 200 $tecnico $edicion
    $formato = Api GET "/api/protocolos/$($pro.id)" 200 $tecnico $null
    if ($formato.textoIngresado.temperatura -cne '020' -or $formato.textoIngresado.humedad -cne '22.0' -or
        $formato.textoIngresado.lectura1Min -cne '10.00' -or $formato.textoIngresado.lectura10Min -cne '20') {
        throw 'La edición no conservó el texto original de los decimales'
    }
    $null = Api GET "/api/protocolos/equipo/$($eq.id)" 200 $tecnico $null
    Add-Type -AssemblyName System.Net.Http
    $http = [Net.Http.HttpClient]::new()
    try {
        $http.Timeout = [TimeSpan]::FromSeconds(12)
        $http.DefaultRequestHeaders.Authorization = [Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $tecnico)
        $uriActa = "$enlace/api/protocolos/$($pro.id)/acta"
        $entregas = @($http.PostAsync($uriActa, $null), $http.PostAsync($uriActa, $null))
        $actas = foreach ($entrega in $entregas) {
            $respuestaActa = $entrega.GetAwaiter().GetResult()
            try {
                if ([int]$respuestaActa.StatusCode -ne 201) {
                    throw "Entrega concurrente de acta obtuvo $([int]$respuestaActa.StatusCode)"
                }
                $respuestaActa.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
            } finally { $respuestaActa.Dispose() }
        }
    } finally { $http.Dispose() }
    if (!$actas[0].id -or $actas[0].id -ne $actas[1].id) {
        throw 'Las entregas simultáneas deben devolver el mismo identificador de acta'
    }
    $acta = $actas[0]
    Write-Host "POST /api/protocolos/$($pro.id)/acta -> 201 en ambas solicitudes concurrentes"
    $null = Api PATCH "/api/protocolos/$($pro.id)/formalizar" 403 $tecnico $null
    $null = Api PATCH "/api/protocolos/$($pro.id)/formalizar" 200 $ingeniero $null
    $null = Api PUT "/api/protocolos/$($pro.id)" 409 $tecnico $edicion
    $actaRepetida = Api POST "/api/protocolos/$($pro.id)/acta" 201 $tecnico $null
    $detalleFormalizado = Api GET "/api/protocolos/$($pro.id)" 200 $tecnico $null
    if (!$acta.id -or $actaRepetida.id -ne $acta.id -or $detalleFormalizado.acta.id -ne $acta.id -or
        $detalleFormalizado.protocolo.estado -ne 'FORMALIZADO') {
        throw 'El reintento debe conservar el acta existente y el protocolo formalizado'
    }
    $sesionCliente = Renovar-Sesion $correo $temporalCliente -ConClave
    $nuevoCliente = $sesionCliente.Token
    $propios = Api GET '/api/equipos/mis-equipos' 200 $nuevoCliente $null
    $historial = Api GET "/api/protocolos/equipo/$($eq.id)" 200 $nuevoCliente $null
    if (@($propios.content.id) -notcontains $eq.id -or @($historial.content.id) -notcontains $pro.id) { throw 'Faltan recursos propios del cliente' }
    $null = Api GET '/api/equipos/1' 404 $nuevoCliente $null
    $null = Api GET '/api/protocolos/equipo/1' 404 $nuevoCliente $null
    $otroCliente = Renovar-Sesion 'cliente@gmail.com' $claves[5]
    $null = Api GET "/api/protocolos/$($pro.id)" 404 $otroCliente $null
    $null = Api DELETE "/api/equipos/$($eq.id)" 409 $jefe $null
    $null = Api DELETE "/api/instrumentos/$($ins.id)" 409 $jefe $null
    $null = Api DELETE "/api/clientes/$($cliente.id)" 409 $jefe $null
    $clienteLibre = Api POST '/api/clientes' 201 $jefe @{nombre='Cliente sin formalizar';rut='9.876.543-3';correo="libre-$marca@example.invalid";giro='Servicios'}
    $plantaLibre = Api POST '/api/instalaciones' 201 $jefe @{nombre='Planta sin formalizar';idCliente=$clienteLibre.id}
    $equipoLibre = Api POST '/api/equipos' 201 $jefe @{tag='TR-SIN-FORMALIZAR';tipoEquipo='Transformador';marca='Demo';serie='LIBRE-1';instalacionId=$plantaLibre.id}
    $proLibre = Api POST '/api/protocolos' 201 $tecnico @{equipoId=$equipoLibre.id;instrumentoId=$ins.id;temperatura=20;humedad=22;tipoPrueba='AISLACION';valores=@{lectura1Min=10;lectura10Min=20}}
    $null = Api PATCH "/api/protocolos/$($proLibre.id)/cerrar" 200 $tecnico $null
    $null = Api DELETE "/api/instalaciones/$($plantaLibre.id)" 409 $tecnico $null
    $null = Api DELETE "/api/equipos/$($equipoLibre.id)" 204 $jefe $null
    $null = Api GET "/api/equipos/$($equipoLibre.id)" 404 $jefe $null
    $null = Api GET "/api/protocolos/$($proLibre.id)" 200 $tecnico $null
    $null = Api PATCH "/api/protocolos/$($proLibre.id)/formalizar" 409 $ingeniero $null
    $null = Api PATCH "/api/protocolos/$($proLibre.id)/anular" 200 $tecnico $null
    $null = Api DELETE "/api/instalaciones/$($plantaLibre.id)" 204 $ingeniero $null
    $null = Api GET "/api/instalaciones/$($plantaLibre.id)" 404 $tecnico $null
    $plantaTecnico = Api POST '/api/instalaciones' 201 $jefe @{nombre='Planta vacia del tecnico';idCliente=$clienteLibre.id}
    $null = Api DELETE "/api/instalaciones/$($plantaTecnico.id)" 204 $tecnico $null
    $plantaConCliente = Api POST '/api/instalaciones' 201 $jefe @{nombre='Planta con cliente para archivar';idCliente=$clienteLibre.id}
    $equipoConCliente = Api POST '/api/equipos' 201 $jefe @{tag='TR-CLIENTE-ARCHIVO';tipoEquipo='Transformador';marca='Demo';serie='LIBRE-3';instalacionId=$plantaConCliente.id}
    $null = Api DELETE "/api/clientes/$($clienteLibre.id)" 204 $jefe $null
    $null = Api POST '/api/protocolos' 409 $tecnico @{equipoId=$equipoConCliente.id;instrumentoId=$ins.id;temperatura=20;humedad=22;tipoPrueba='AISLACION';valores=@{lectura1Min=10;lectura10Min=20}}
    $instrumentoLibre = Api POST '/api/instrumentos' 201 $jefe @{nombre='Instrumento sin uso';marca='Demo';serie='LIBRE-2';fechaVencimiento='31-12-2099'}
    $null = Api DELETE "/api/instrumentos/$($instrumentoLibre.id)" 204 $jefe $null
    $null = Api GET "/api/instrumentos/$($instrumentoLibre.id)" 404 $jefe $null
    $equipoConcurrente = Api POST '/api/equipos' 201 $jefe @{tag='TR-CARRERA';tipoEquipo='Transformador';marca='Demo';serie='CARRERA-1';instalacionId=$inst.id}
    $proConcurrente = Api POST '/api/protocolos' 201 $tecnico @{equipoId=$equipoConcurrente.id;instrumentoId=$ins.id;temperatura=20;humedad=22;tipoPrueba='AISLACION';valores=@{lectura1Min=10;lectura10Min=20}}
    $null = Api PATCH "/api/protocolos/$($proConcurrente.id)/cerrar" 200 $tecnico $null
    $http = [Net.Http.HttpClient]::new()
    $http.Timeout = [TimeSpan]::FromSeconds(15)
    $borrar = [Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::Delete, "$enlace/api/equipos/$($equipoConcurrente.id)")
    $formalizar = [Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::new('PATCH'), "$enlace/api/protocolos/$($proConcurrente.id)/formalizar")
    try {
        $borrar.Headers.Authorization = [Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $jefe)
        $formalizar.Headers.Authorization = [Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $ingeniero)
        $borrado = $http.SendAsync($borrar)
        $revision = $http.SendAsync($formalizar)
        $respuestaBorrado = $borrado.GetAwaiter().GetResult()
        $respuestaRevision = $revision.GetAwaiter().GetResult()
        try {
            $estados = "$([int]$respuestaBorrado.StatusCode),$([int]$respuestaRevision.StatusCode)"
            if ($estados -notin '204,409','409,200') { throw "Resultado concurrente inesperado: $estados" }
        } finally {
            $respuestaBorrado.Dispose()
            $respuestaRevision.Dispose()
        }
    } finally {
        $borrar.Dispose()
        $formalizar.Dispose()
        $http.Dispose()
    }
    if ($estados -eq '204,409') {
        $null = Api PATCH "/api/protocolos/$($proConcurrente.id)/anular" 200 $tecnico $null
    }
    Write-Host "Borrado y formalización concurrentes coordinados ($estados)."
    if ($ApkAcademico) {
        if ($PuertoBase -ne 8080) { throw 'Para el APK académico por defecto usa -PuertoBase 8080' }
        . (Join-Path $PSScriptRoot 'prueba-movil.ps1')
        $clavesInternas = @{Tecnico=$sesionTecnico.Clave; Ingeniero=$sesionIngeniero.Clave; Jefe=$sesionJefe.Clave}
        Probar-AndroidGateway $ApkAcademico $correo $sesionCliente.Clave $clavesInternas $evidencia -RedInvertida:$RedInvertida -EvidenciaVisual:$EvidenciaVisual -EscrituraMovil:$EscrituraMovil
        if ($EscrituraMovil) {
            $vista = Api GET "/api/protocolos/equipo/$($eq.id)" 200 $tecnico $null
            $moviles = @($vista.content | Where-Object { $_.id -ne $pro.id })
            if ($moviles.Count -ne 1 -or $moviles[0].estado -ne 'FORMALIZADO') {
                throw 'El protocolo registrado en Android debe quedar formalizado'
            }
            $detalleMovil = Api GET "/api/protocolos/$($moviles[0].id)" 200 $tecnico $null
            if (!$detalleMovil.acta.id -or $detalleMovil.protocolo.comentarioVariacion -ne 'Validacion movil QA' -or
                [decimal]$detalleMovil.protocolo.temperatura -ne 18.25 -or [decimal]$detalleMovil.protocolo.humedad -ne 55 -or
                [decimal]$detalleMovil.mediciones[0].valores.lectura1Min -ne 15 -or
                [decimal]$detalleMovil.mediciones[0].valores.lectura10Min -ne 30 -or
                $detalleMovil.textoIngresado.humedad -cne '55.0' -or $detalleMovil.textoIngresado.lectura1Min -cne '15.00') {
                throw 'Faltan el acta, el comentario o las mediciones registrados en Android'
            }
        }
    }
    Write-Host "PRUEBA INTEGRAL APROBADA. Logs: $evidencia"
} finally {
    if ($dockerCli) {
        $null = Start-Process $dockerCli -ArgumentList ($argsCompose + @('logs','--no-color')) -WorkingDirectory $repo -Wait -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $evidencia 'compose-servicios.log') -RedirectStandardError (Join-Path $evidencia 'compose-servicios.err')
        $detencion = Start-Process $dockerCli -ArgumentList ($argsCompose + @('down','--rmi','local','-t','10')) -WorkingDirectory $repo -Wait -PassThru -WindowStyle Hidden
        if ($detencion.ExitCode -ne 0) { Write-Warning "Compose no retiró los contenedores e imágenes del proyecto $proyecto" }
    }
    foreach ($p in $procesos) { if (!$p.HasExited) { Stop-Process -Id $p.Id -ErrorAction SilentlyContinue } }
    Write-Host "Evidencia: $evidencia"
}
