package io.proj3ct.SpringEduBot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.sql.Timestamp;

@Entity(name = "cardsTable")
public class Cards {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long chatId;

    private String link;

    private String article;

    private String productName;

    private String brandName;

    private Double price;

    private Double rating;

    private Long feedbackCount;



    private Timestamp addedAt;

    private Long stock; // v.2

    public Long getStock() {
        return stock;
    }

    public void setStock(Long stocks) {
        this.stock = stocks;
    }

    private String sizeName;

    public String getSizeName() {
        return sizeName;
    }

    public void setSizeName(String sizeName) {
        this.sizeName = sizeName;
    }

    private Boolean priceNotification;

    private Boolean amountNotification;

    public Boolean getPriceNotification() {
        return priceNotification;
    }

    public void setPriceNotification(Boolean priceNotification) {
        this.priceNotification = priceNotification;
    }

    public Boolean getAmountNotification() {
        return amountNotification;
    }

    public void setAmountNotification(Boolean amountNotification) {
        this.amountNotification = amountNotification;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long userId) {
        this.chatId = userId;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getArticle() {
        return article;
    }

    public void setArticle(String article) {
        this.article = article;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Long getFeedbackCount() {
        return feedbackCount;
    }

    public void setFeedbackCount(Long feedbackCount) {
        this.feedbackCount = feedbackCount;
    }

    public Timestamp getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Timestamp registeredAt) {
        this.addedAt = registeredAt;
    }

    @Override
    public String toString() {
        return "Cards{" +
                "link='" + link + '\'' +
                ", productName='" + productName + '\'' +
                ", price=" + price +
                '}';
    }
}

