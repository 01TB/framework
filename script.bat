@echo off
setlocal

:: Variables pour la librairie FrontServlet
set "SRC_DIR=src\main\java"
set "BUILD_DIR=build"
set "LIB_DIR=lib"
set "SERVLET_API_JAR=%LIB_DIR%\servlet-api.jar"

:: Nom du JAR à générer
set "JAR_NAME=servlet.jar"

:: (script simplifié — vérifications optionnelles supprimées)

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
            echo Nettoyage terminee.
            exit /b 0
        )
    ) else (
        echo Rien a nettoyer.
        exit /b 0
    )
)

:: Créer repertoire de build
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

:: Lancer javac (compilation directe via FOR /R)
echo Lancement de javac (compilation directe)...

setlocal enabledelayedexpansion
set "ERR=0"
set "FILE_COUNT=0"
for /r "%SRC_DIR%" %%F in (*.java) do (
    set /a FILE_COUNT+=1
    echo Compiling: "%%~fF"
    javac -d "%BUILD_DIR%\classes" -classpath "%SERVLET_API_JAR%" "%%~fF"
    if errorlevel 1 (
        set "ERR=1"
        echo Erreur pendant la compilation de "%%~fF"
        goto :endjavacloop
    )
)
:endjavacloop
endlocal & set "ERR=%ERR%" & set "FILE_COUNT=%FILE_COUNT%"
if "%FILE_COUNT%"=="0" (
    echo Aucune source Java trouvee dans "%SRC_DIR%".
    exit /b 1
)
if "%ERR%"=="1" (
    echo Erreur de compilation detectee.
    exit /b 1
)

:: Créer le JAR contenant les classes compilées
pushd "%BUILD_DIR%\classes"
jar -cvf "%~dp0%JAR_NAME%" * > nul
popd

endlocal
echo JAR genere: %~dp0%JAR_NAME%