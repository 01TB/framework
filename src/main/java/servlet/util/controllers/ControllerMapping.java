package servlet.util.controllers;

import java.util.Map;

import servlet.util.ControllerInfo;

// Classe interne pour encapsuler les informations de mapping d'un controller
public class ControllerMapping {
    private final ControllerInfo controllerInfo;
    private final Map<String, String> pathParams;

    public ControllerMapping(ControllerInfo controllerInfo, Map<String, String> pathParams) {
        this.controllerInfo = controllerInfo;
        this.pathParams = pathParams;
    }

    public ControllerInfo getControllerInfo() {
        return controllerInfo;
    }

    public Map<String, String> getPathParams() {
        return pathParams;
    }
}