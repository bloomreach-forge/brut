package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.onehippo.cms7.essentials.components.paging.Pageable;

import javax.xml.bind.annotation.XmlAnyElement;
import java.util.ArrayList;
import java.util.List;

public class ListItemPagination<T> extends Pageable<T> {

    private List<T> items;

    public ListItemPagination() {
        items = new ArrayList<>();
    }

    private int currentPage;
    private int totalPages;

    @XmlAnyElement(lax = true)
    @Override
    public List<T> getItems() {
        return items;
    }

    public void addItem(T item) {
        items.add(item);
    }

    @JsonProperty(value = "currentPage")
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    @JsonProperty(value = "totalPages")
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

}
