function Probar-AndroidGateway([string]$Apk, [string]$Correo, [string]$Clave, [hashtable]$ClavesInternas, [string]$Evidencia, [switch]$RedInvertida, [switch]$EvidenciaVisual, [switch]$EscrituraMovil) {
    if (!(Test-Path -LiteralPath $Apk)) { throw 'Compila el APK debug antes de probar Android' }
    $sdk = Join-Path $env:LOCALAPPDATA 'Android/Sdk'
    $adb = Join-Path $sdk 'platform-tools/adb.exe'
    $emulador = Join-Path $sdk 'emulator/emulator.exe'
    $serie = 'emulator-5566'
    if ((& $adb devices) -match $serie) { throw 'El puerto de emulador 5566 ya está en uso' }

    function Buscar-Nodos([string]$XPath, [int]$Cantidad = 1) {
        $limite = [DateTime]::UtcNow.AddSeconds(35)
        do {
            $null = & $adb -s $serie shell uiautomator dump /sdcard/induztek-qa.xml
            $salida = & $adb -s $serie shell cat /sdcard/induztek-qa.xml
            try {
                [xml]$pantalla = $salida -join [Environment]::NewLine
                $nodos = @($pantalla.SelectNodes($XPath))
                if ($nodos.Count -ge $Cantidad) { return $nodos }
            } catch {}
            Start-Sleep -Seconds 1
        } while ([DateTime]::UtcNow -lt $limite)
        if ($pantalla) {
            foreach ($etiqueta in 'Credenciales inválidas','No se pudo conectar','Mis Equipos','Iniciar Sesión','No se encontraron equipos') {
                if ($pantalla.SelectSingleNode("//node[contains(@text,'$etiqueta')]")) { Write-Host "ANDROID: pantalla contiene $etiqueta" }
            }
        }
        throw "No apareció el control Android esperado: $XPath"
    }
    function Pulsar($Nodo) {
        if ($Nodo.bounds -notmatch '^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$') { throw 'Control sin coordenadas' }
        $x = [int](([int]$Matches[1] + [int]$Matches[3]) / 2)
        $y = [int](([int]$Matches[2] + [int]$Matches[4]) / 2)
        & $adb -s $serie shell input tap $x $y | Out-Null
    }

    $local = Join-Path $Evidencia 'avd'
    $configuracion = Join-Path $local 'Induztek_Gateway.avd'
    [IO.Directory]::CreateDirectory($configuracion) | Out-Null
    $configOriginal = Join-Path $env:USERPROFILE '.android/avd/Medium_Phone.avd/config.ini'
    Get-Content -LiteralPath $configOriginal |
        Where-Object { $_ -notmatch '^(disk.dataPartition.path|fastboot.forceSnapshotBoot|hw.sdCard.path|sdcard.path|AvdId|avd.ini.displayname)=' } |
        Set-Content -LiteralPath (Join-Path $configuracion 'config.ini') -Encoding Ascii
    @('avd.ini.encoding=UTF-8', "path=$configuracion", 'target=android-37.0') |
        Set-Content -LiteralPath (Join-Path $local 'Induztek_Gateway.ini') -Encoding Ascii
    $anteriorAvd = $env:ANDROID_AVD_HOME
    $env:ANDROID_AVD_HOME = $local
    $argumentos = @('-avd','Induztek_Gateway','-port','5566','-no-window','-no-audio','-no-snapshot','-gpu','swiftshader')
    $proceso = Start-Process $emulador -ArgumentList $argumentos -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $Evidencia 'emulador.log') -RedirectStandardError (Join-Path $Evidencia 'emulador.err')
    try {
        $vence = [DateTime]::UtcNow.AddMinutes(3)
        $arrancado = $false
        do {
            if ((& $adb devices) -match "^$serie\s+device") {
                $arrancado = (& $adb -s $serie shell getprop sys.boot_completed) -eq '1'
                if ($arrancado) { break }
            }
            Start-Sleep -Seconds 4
        } while ([DateTime]::UtcNow -lt $vence)
        if (!$arrancado) { throw 'El emulador aislado no terminó de arrancar' }
        if ($RedInvertida) {
            $null = & $adb -s $serie reverse tcp:8080 tcp:8080
            if ($LASTEXITCODE -ne 0) { throw 'No se pudo enlazar el Gateway por ADB reverse' }
        }
        & $adb -s $serie install $Apk | Out-Host
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo instalar el APK' }
        $null = & $adb -s $serie shell wm dismiss-keyguard
        & $adb -s $serie shell am start -W -n cl.duocuc.miregistro/.MainActivity | Out-Host

        $accionRed = @(Buscar-Nodos '//node[contains(@text,"Permitir acceso a red local")]')[0]
        Pulsar $accionRed
        $aceptar = @(Buscar-Nodos '//node[@resource-id="com.android.permissioncontroller:id/permission_allow_button" or @text="Allow" or @text="Permitir"]')[0]
        Pulsar $aceptar
        Write-Host 'ANDROID: permiso de red local concedido por el diálogo del sistema.'

        $directorio = (Get-Item -LiteralPath $Apk).Directory
        $tipo = $directorio.Name
        $apkPruebas = Join-Path $directorio.Parent.Parent.FullName "androidTest/academico/$tipo/app-academico-$tipo-androidTest.apk"
        if (!(Test-Path -LiteralPath $apkPruebas)) { throw "Falta el APK instrumentado académico para $tipo" }
        & $adb -s $serie install $apkPruebas | Out-Host
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo instalar el APK de pruebas' }
        $captura = if ($EvidenciaVisual) { '-e evidenciaVisual true' } else { '' }
        $opciones = @()
        if ($EscrituraMovil) { $opciones += '-e escrituraMovil true' }
        $argumentosOpcionales = $opciones -join ' '
        function Ejecutar-Escenario([string]$Metodo, [string]$Usuario, [string]$Secreto, [string]$Extra = '') {
            $clase = "cl.duocuc.miregistro.GatewayClienteInstrumentadoTest#$Metodo"
            $salida = & $adb -s $serie shell "am instrument -w -e class '$clase' -e correo '$Usuario' -e clave '$Secreto' $Extra $captura $argumentosOpcionales cl.duocuc.miregistro.test/androidx.test.runner.AndroidJUnitRunner"
            $codigo = $LASTEXITCODE
            $segura = @($salida | ForEach-Object {
                ([string]$_).Replace($Usuario, '[correo]').Replace($Secreto, '[clave]')
            })
            $segura | Set-Content -LiteralPath (Join-Path $Evidencia "android-$Metodo.txt") -Encoding UTF8
            $resumen = @($segura | Where-Object { $_ -match 'OK \(1 test\)|FAILURES|AssertionError|Etapa |Tests run:' })
            $resumen | Out-Host
            if ($codigo -ne 0 -or !($resumen -match 'OK \(1 test\)')) {
                throw "No aprobó la prueba Android $Metodo"
            }
        }
        Ejecutar-Escenario 'ingresaYVeElEquipoRealConSuHistorialFormalizado' $Correo $Clave
        Write-Host 'ANDROID: login, equipo remoto e historial formalizado aprobados.'

        & $adb -s $serie shell am force-stop cl.duocuc.miregistro | Out-Null
        & $adb -s $serie shell am start -W -n cl.duocuc.miregistro/.MainActivity | Out-Null
        $null = Buscar-Nodos '//node[contains(@text,"TR-INTEGRACION")]'
        Write-Host 'ANDROID: sesión restaurada y validada por el Gateway.'
        $escenariosInternos = @(
            @{Metodo='tecnicoConsultaLaInstalacionYAbreElRegistroDePrueba'; Usuario='terreno@gmail.com'; Rol='Tecnico'}
            @{Metodo='ingenieroAbreLaBandejaRemotaDeFormalizacion'; Usuario='ingeniero@gmail.com'; Rol='Ingeniero'}
            @{Metodo='jefeTecnicoEncuentraElInstrumentoEnElCatalogoRemoto'; Usuario='jefe@gmail.com'; Rol='Jefe'}
        )
        $fallidos = foreach ($escenario in $escenariosInternos) {
            try {
                Ejecutar-Escenario $escenario.Metodo $escenario.Usuario $ClavesInternas[$escenario.Rol] '-e sesionPreviaActiva true'
                Write-Host "ANDROID: escenario aprobado para $($escenario.Rol)."
            } catch {
                $escenario.Rol
            }
        }
        $capturasFaltantes = @()
        if ($EvidenciaVisual) {
            $directorioCapturas = Join-Path $Evidencia 'capturas'
            [IO.Directory]::CreateDirectory($directorioCapturas) | Out-Null
            $archivos = @('cliente','tecnico','ingeniero','jefe-tecnico')
            if ($EscrituraMovil) { $archivos += 'acta-entregada','formalizacion' }
            foreach ($rol in $archivos) {
                $destino = Join-Path $directorioCapturas "$rol.png"
                $rutaCaptura = "/data/local/tmp/induztek-$rol.png"
                & $adb -s $serie shell test -s $rutaCaptura
                if ($LASTEXITCODE -ne 0) {
                    $capturasFaltantes += $rol
                    continue
                }
                & $adb -s $serie pull $rutaCaptura $destino | Out-Host
                if ($LASTEXITCODE -ne 0 -or !(Test-Path -LiteralPath $destino)) {
                    $capturasFaltantes += $rol
                }
            }
        }
        if ($capturasFaltantes) { Write-Warning "Capturas pendientes: $($capturasFaltantes -join ', ')" }
        if ($fallidos) { throw "Pruebas Android pendientes para: $($fallidos -join ', ')" }
        if ($capturasFaltantes) { throw 'La evidencia visual quedó incompleta' }
    } finally {
        if ((& $adb devices) -match $serie) { & $adb -s $serie emu kill | Out-Null }
        if (!$proceso.HasExited) { Stop-Process -Id $proceso.Id -ErrorAction SilentlyContinue }
        $env:ANDROID_AVD_HOME = $anteriorAvd
    }
}
