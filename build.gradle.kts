// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("org.sonarqube") version "6.0.1.5171"
}

sonar {
  properties {
    property("sonar.projectKey", "Yimura_FridaMgr_8180167f-e574-4541-bc70-1863a696da99")
    property("sonar.projectName", "FridaMgr")
  }
}
