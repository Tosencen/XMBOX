package com.github.tvbox.osc.bean;

public class SearchCategoryBean {
    private String name;
    private String key;
    private boolean isSelected;
    private String appName;
    private boolean isSearchOnly;

    public SearchCategoryBean() {
    }

    public SearchCategoryBean(String name, String key, boolean isSelected, String appName, boolean isSearchOnly) {
        this.name = name;
        this.key = key;
        this.isSelected = isSelected;
        this.appName = appName;
        this.isSearchOnly = isSearchOnly;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public boolean isSearchOnly() {
        return isSearchOnly;
    }

    public void setSearchOnly(boolean searchOnly) {
        isSearchOnly = searchOnly;
    }
}
