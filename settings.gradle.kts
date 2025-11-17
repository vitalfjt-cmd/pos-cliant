pluginManagement {
    repositories {
        // GoogleのMavenリポジトリを定義 (AndroidXプラグインに必須)
        google()
        // その他の一般的なプラグインリポジトリ
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // GoogleのMavenリポジトリを定義 (ライブラリの依存関係に必須)
        google()
        // その他の一般的なライブラリリポジトリ
        mavenCentral()
    }
}

rootProject.name = "PosClientApp"
include(":app")