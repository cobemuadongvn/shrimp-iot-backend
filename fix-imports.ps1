$files = @(
    "d:\shrimp-iot-complete-work\src\main\java\com\example\shrimpiot\aquaculture\controller\PondController.java",
    "d:\shrimp-iot-complete-work\src\main\java\com\example\shrimpiot\auth\service\AuthService.java",
    "d:\shrimp-iot-complete-work\src\main\java\com\example\shrimpiot\auth\service\UserService.java",
    "d:\shrimp-iot-complete-work\src\main\java\com\example\shrimpiot\chat\service\ChatService.java",
    "d:\shrimp-iot-complete-work\src\main\java\com\example\shrimpiot\monitoring\service\AlertService.java"
)

$modelWildcards = @"
import com.example.shrimpiot.model.*;
import com.example.shrimpiot.aquaculture.model.*;
import com.example.shrimpiot.auth.model.*;
import com.example.shrimpiot.device.model.*;
import com.example.shrimpiot.monitoring.model.*;
import com.example.shrimpiot.chat.model.*;
import com.example.shrimpiot.automation.model.*;
"@

$repoWildcards = @"
import com.example.shrimpiot.repository.*;
import com.example.shrimpiot.aquaculture.repository.*;
import com.example.shrimpiot.auth.repository.*;
import com.example.shrimpiot.device.repository.*;
import com.example.shrimpiot.monitoring.repository.*;
import com.example.shrimpiot.chat.repository.*;
import com.example.shrimpiot.automation.repository.*;
"@

foreach ($file in $files) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw
        $content = $content -replace 'import com\.example\.shrimpiot\.model\.\*;', $modelWildcards
        $content = $content -replace 'import com\.example\.shrimpiot\.repository\.\*;', $repoWildcards
        Set-Content -Path $file -Value $content -NoNewline
    }
}
