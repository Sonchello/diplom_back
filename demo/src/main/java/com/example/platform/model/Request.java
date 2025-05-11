package com.example.platform.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.Data;

@Data
@Entity
@Table(name = "requests")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String category;

    @Column(name = "deadline_date", nullable = false)
    private LocalDateTime deadlineDate;

    @Column(name = "is_expired")
    private Boolean isExpired;

    @PrePersist
    @PreUpdate
    private void validateDeadlineDate() {
        if (deadlineDate == null) {
            throw new IllegalArgumentException("Deadline date is required");
        }
        if (deadlineDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Deadline date cannot be in the past");
        }
    }

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
    @JoinTable(
            name = "user_helped_requests",
            joinColumns = @JoinColumn(name = "request_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnoreProperties({"helpedRequests", "createdRequests", "password"})
    private List<User> helpers = new ArrayList<>();

    @Column(name = "is_archived")
    private boolean isArchived = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_helper_id")
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

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
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

    public LocalDateTime getDeadlineDate() {
        return deadlineDate;
    }

    public void setDeadlineDate(LocalDateTime deadlineDate) {
        this.deadlineDate = deadlineDate;
    }

    public Boolean getIsExpired() {
        return isExpired;
    }

    public void setIsExpired(Boolean isExpired) {
        this.isExpired = isExpired;
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