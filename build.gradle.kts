plugins {
    id("com.android.application") version "9.3.1" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
    id("org.sonarqube") version "7.5.0.8588"
}

sonar {
    properties {
        property("sonar.projectKey", "tupastilla-android")
        property("sonar.projectName", "TuPastilla Android")
    }
}

project(":app") {
    sonar {
        properties {
            // Ruta relativa al modulo app, no a la raiz. La cobertura se mide solo sobre el paquete
            // auth: sin excluir el resto, Sonar reporta 0 % porque JaCoCo no genera datos de esos paquetes.
            property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/coberturaAuth/coberturaAuth.xml")
            val fueraDelModulo = listOf("ajustes", "alarma", "alerta", "data", "historial", "hoy", "medicinas", "onboarding", "personas", "ui")
                .map { "src/main/java/com/tupastilla/$it/**" }
            val requierenDispositivo = listOf("MainActivity.kt", "auth/*Fragment.kt", "auth/FormularioAuth.kt", "auth/MensajesAuth.kt", "auth/AlmacenSesion.kt")
                .map { "src/main/java/com/tupastilla/$it" }
            property("sonar.coverage.exclusions", (fueraDelModulo + requierenDispositivo).joinToString(","))
        }
    }
}
