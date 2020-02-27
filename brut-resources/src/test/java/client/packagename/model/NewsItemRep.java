package client.packagename.model;


import com.fasterxml.jackson.annotation.JsonProperty;

public class NewsItemRep {

    private String title;

    public NewsItemRep represent(NewsDocument bean) {
        setTitle(bean.getTitle());
        return this;
    }

    @JsonProperty(value = "title")
    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }
}
