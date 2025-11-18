package servlet.models;

import java.util.Map;

public class ModelView {
    
    private String view;

    private Map<String,Object> data;

    public ModelView(String view) {
        this.view = view;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public void addData(String key, Object value) {
        if (this.data == null) {
            this.data = new java.util.HashMap<>();
        }
        this.data.put(key, value);
    }

    @Override
    public String toString() {
        return "ModelView [view=" + view + ", data=" + data + "]";
    }

}
