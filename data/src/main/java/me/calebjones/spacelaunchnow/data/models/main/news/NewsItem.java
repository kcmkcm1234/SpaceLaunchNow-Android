package me.calebjones.spacelaunchnow.data.models.main.news;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import me.calebjones.spacelaunchnow.data.models.realm.RealmStr;

public class NewsItem extends RealmObject {

    @PrimaryKey
    @SerializedName("_id")
    private String id;
    @SerializedName("title")
    private String title;
    @SerializedName("news_site_long")
    private String newsSiteLong;
    @SerializedName("news_site")
    private String newsSite;
    @SerializedName("featured_image")
    private String featured_image;
    @SerializedName("url")
    private String url;
    @SerializedName("launches")
    @Expose
    public RealmList<RealmStr> launches = null;
    @SerializedName("date_published")
    private int datePublished;
    private Date lastUpdate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNewsSiteLong() {
        return newsSiteLong;
    }

    public void setNewsSiteLong(String newsSiteLong) {
        this.newsSiteLong = newsSiteLong;
    }

    public String getNewsSite() {
        return newsSite;
    }

    public void setNewsSite(String newsSite) {
        this.newsSite = newsSite;
    }

    public String getFeaturedImage() {
        return featured_image;
    }

    public void setFeaturedImage(String featured_image) {
        this.featured_image = featured_image;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getDatePublished() {
        return datePublished;
    }

    public void setDatePublished(int datePublished) {
        this.datePublished = datePublished;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }


    public RealmList<RealmStr> getLaunches() {
        return launches;
    }

    public void setLaunches(RealmList<RealmStr> launches) {
        this.launches = launches;
    }
}