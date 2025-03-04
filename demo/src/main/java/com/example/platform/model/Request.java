package com.example.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "requests")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String urgency = "medium";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"createdRequests", "helpedRequests", "password"})
    private User user;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "help_start_date")
    private LocalDateTime helpStartDate;

    @Column(name = "completion_date")
    private LocalDateTime completionDate;

    @ManyToMany(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"helpedRequests", "createdRequests", "password"})
    private List<User> helpers = new ArrayList<>();

    @Column(name = "is_archived")
    private boolean isArchived = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "helper_id")
    @JsonIgnoreProperties({"createdRequests", "helpedRequests", "password"})
    private User activeHelper;

    @OneToMany(mappedBy = "request", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"request"})
    private List<HelpHistory> helpHistory;

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getHelpStartDate() {
        return helpStartDate;
    }

    public void setHelpStartDate(LocalDateTime helpStartDate) {
        this.helpStartDate = helpStartDate;
    }

    public LocalDateTime getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(LocalDateTime completionDate) {
        this.completionDate = completionDate;
    }

    public List<User> getHelpers() {
        return helpers;
    }

    public void setHelpers(List<User> helpers) {
        this.helpers = helpers;
    }

    public String getUserName() {
        return user != null ? user.getName() : "Аноним";
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(boolean archived) {
        isArchived = archived;
    }

    public User getActiveHelper() {
        return activeHelper;
    }

    public void setActiveHelper(User activeHelper) {
        this.activeHelper = activeHelper;
    }

    public List<HelpHistory> getHelpHistory() {
        return helpHistory;
    }

    public void setHelpHistory(List<HelpHistory> helpHistory) {
        this.helpHistory = helpHistory;
    }
}