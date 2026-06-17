$basePath = "d:\shrimp-iot-complete-work\src\main\java\com\example\shrimpiot"

$allJavaFiles = Get-ChildItem -Path $basePath -Recurse -Filter *.java

$wildcards = @"
import com.example.shrimpiot.model.*;
import com.example.shrimpiot.repository.*;
import com.example.shrimpiot.service.*;
import com.example.shrimpiot.controller.*;
import com.example.shrimpiot.dto.*;
import com.example.shrimpiot.exception.*;
import com.example.shrimpiot.aquaculture.model.*;
import com.example.shrimpiot.aquaculture.repository.*;
import com.example.shrimpiot.aquaculture.controller.*;
import com.example.shrimpiot.auth.model.*;
import com.example.shrimpiot.auth.repository.*;
import com.example.shrimpiot.auth.service.*;
import com.example.shrimpiot.auth.controller.*;
import com.example.shrimpiot.device.model.*;
import com.example.shrimpiot.device.repository.*;
import com.example.shrimpiot.device.service.*;
import com.example.shrimpiot.device.controller.*;
import com.example.shrimpiot.monitoring.model.*;
import com.example.shrimpiot.monitoring.repository.*;
import com.example.shrimpiot.monitoring.service.*;
import com.example.shrimpiot.monitoring.controller.*;
import com.example.shrimpiot.chat.model.*;
import com.example.shrimpiot.chat.repository.*;
import com.example.shrimpiot.chat.service.*;
import com.example.shrimpiot.chat.controller.*;
import com.example.shrimpiot.automation.model.*;
import com.example.shrimpiot.automation.repository.*;
import com.example.shrimpiot.automation.service.*;
import com.example.shrimpiot.automation.controller.*;
"@

foreach ($file in $allJavaFiles) {
    if ($file.Name -eq "ShrimpIotApplication.java") { continue }
    $content = Get-Content $file.FullName -Raw
    # We will insert the wildcards immediately after the package declaration.
    # First, let's remove any of our previously inserted wildcards just in case.
    # Actually, simpler: replace the package statement with itself + wildcards
    if ($content -match "(?m)^package\s+[^;]+;") {
        $pkg = $matches[0]
        # To avoid duplicating wildcards if this script is run twice, we'll check
        if (-not ($content.Contains("import com.example.shrimpiot.aquaculture.model.*;"))) {
            $content = $content -replace "(?m)^package\s+[^;]+;", "$pkg`r`n`r`n$wildcards"
            Set-Content -Path $file.FullName -Value $content -NoNewline
        }
    }
}
