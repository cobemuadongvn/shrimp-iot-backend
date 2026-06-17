$files = Get-ChildItem -Path "d:\shrimp-iot-complete-work\src\main\java" -Recurse -Filter *.java
foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    $changed = $false
    
    if ($content -match "com\.example\.shrimpiot\.service\.RelayStateService") {
        $content = $content -replace "com\.example\.shrimpiot\.service\.RelayStateService", "com.example.shrimpiot.device.service.RelayStateService"
        $changed = $true
    }
    if ($content -match "com\.example\.shrimpiot\.model\.RelayState") {
        $content = $content -replace "com\.example\.shrimpiot\.model\.RelayState", "com.example.shrimpiot.device.model.RelayState"
        $changed = $true
    }
    if ($content -match "com\.example\.shrimpiot\.model\.DeviceRelay") {
        $content = $content -replace "com\.example\.shrimpiot\.model\.DeviceRelay", "com.example.shrimpiot.device.model.DeviceRelay"
        $changed = $true
    }
    if ($content -match "com\.example\.shrimpiot\.model\.ControlScenario") {
        $content = $content -replace "com\.example\.shrimpiot\.model\.ControlScenario", "com.example.shrimpiot.automation.model.ControlScenario"
        $changed = $true
    }
    
    if ($file.Name -in @("PondController.java", "AuthService.java", "UserService.java", "ChatService.java", "AlertService.java")) {
        if (-not $content.Contains("import com.example.shrimpiot.device.service.*;")) {
            $wildcards = @"
import com.example.shrimpiot.service.*;
import com.example.shrimpiot.controller.*;
import com.example.shrimpiot.auth.service.*;
import com.example.shrimpiot.auth.controller.*;
import com.example.shrimpiot.device.service.*;
import com.example.shrimpiot.device.controller.*;
import com.example.shrimpiot.monitoring.service.*;
import com.example.shrimpiot.monitoring.controller.*;
import com.example.shrimpiot.chat.service.*;
import com.example.shrimpiot.chat.controller.*;
import com.example.shrimpiot.automation.service.*;
import com.example.shrimpiot.automation.controller.*;
"@
            $content = $content -replace "(?m)^package\s+[^;]+;", "`${0}`r`n`r`n$wildcards"
            $changed = $true
        }
    }
    
    if ($changed) {
        Set-Content -Path $file.FullName -Value $content -NoNewline
    }
}
