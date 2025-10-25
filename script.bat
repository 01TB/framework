@echo off
setlocal

:: Variables pour la librairie FrontServlet
set "SRC_DIR=src\main\java"
set "BUILD_DIR=build"
set "LIB_DIR=lib"
set "SERVLET_API_JAR=%LIB_DIR%\servlet-api.jar"
set "CLASSPATH=%SERVLET_API_JAR%;%BUILD_DIR%\classes"

:: Nom du JAR à générer
set "JAR_NAME=servlet.jar"

:: Helper: afficher usage
if "%~1"=="help" (
    echo Usage: %~n0 [build^|clean^|help]
    echo   build - compile les sources et creer %JAR_NAME%
    echo   clean - supprime le dossier %BUILD_DIR%
    goto :eof
)

if "%~1"=="clean" (
    echo Nettoyage: suppression de "%BUILD_DIR%"
    if exist "%BUILD_DIR%" (
        rmdir /s /q "%BUILD_DIR%"
        if errorlevel 1 (
            echo Erreur lors de la suppression de "%BUILD_DIR%"
            exit /b 1
        ) else (
            echo Nettoyage termine.
            exit /b 0
        )
    ) else (
        echo Rien a nettoyer.
        exit /b 0
    )
)

:: Créer répertoire de build
if exist "%BUILD_DIR%" (
    rmdir /s /q "%BUILD_DIR%"
)
mkdir "%BUILD_DIR%"
mkdir "%BUILD_DIR%\classes"

:: Compiler les sources .java
echo Compilation des sources depuis "%SRC_DIR%"...

if not exist "%SRC_DIR%" (
    echo ERREUR: dossier source "%SRC_DIR%" introuvable.
    exit /b 1
)

:: Trouver tous les fichiers .java
setlocal enabledelayedexpansion
set "JAVA_FILES="
set "FILE_COUNT=0"
for /r "%SRC_DIR%" %%F in (*.java) do (
    set /a FILE_COUNT+=1
    set "JAVA_FILES=!JAVA_FILES! "%%F""
)
if "%FILE_COUNT%"=="0" (
    echo Aucune source Java trouvee dans "%SRC_DIR%".
    exit /b 1
)

:: Compiler tous les fichiers .java en une seule commande
echo Lancement de javac...
javac -d "%BUILD_DIR%\classes" -classpath "%CLASSPATH%" %JAVA_FILES%
if errorlevel 1 (
    echo Erreur pendant la compilation.
    exit /b 1
)
echo Compilation terminee avec succes.

:: Créer le JAR contenant les classes compilées
pushd "%BUILD_DIR%\classes"
jar -cvf "%~dp0%JAR_NAME%" * > nul
popd

echo JAR genere: %~dp0%JAR_NAME%
endlocal
exit /b 0