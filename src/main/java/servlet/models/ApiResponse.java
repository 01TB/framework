package servlet.models;

import java.util.List;

public class ApiResponse<T> {
    private String status;
    private int code;
    private T data;
    private String message;
    private int count;

    // Constructeur succès (avec données)
    public ApiResponse(String status, int code, T data) {
        this.status = status;
        this.code = code;
        this.data = data;
        this.count = calculateCount(data);
    }

    // Constructeur erreur
    public ApiResponse(String status, int code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = null;
        this.count = 0;
    }

    // Calcule count selon le type de data
    private int calculateCount(T data) {
        if (data == null) return 0;
        if (data instanceof List) {
            return ((List<?>) data).size();
        }
        if (data.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(data);
        }
        if(data instanceof ModelView){
            ModelView mv = (ModelView) data;
            return mv.getData().size();
        }
        return 1; // objet unique
    }

    // Getters
    public String getStatus() { return status; }
    public int getCode() { return code; }
    public T getData() { return data; }
    public String getMessage() { return message; }
    public int getCount() { return count; }

    public void setCount(int count) {
        this.count = count;
    }
}