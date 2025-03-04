package com.example.platform.model;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;

import jakarta.validation.constraints.Email;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.time.LocalDate;



@Entity

@Table(name = "users")

public class User {

    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;



    @NotBlank(message = "Имя пользователя обязательно")

    private String name;



    @Email(message = "Некорректный email")

    @NotBlank(message = "Email обязателен")

    private String email;



    @NotBlank(message = "Пароль обязателен")

    private String password;



    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)

    @JsonIgnoreProperties("user")

    private List<Request> createdRequests;



    @ManyToMany

    @JoinTable(

            name = "user_helped_requests",

            joinColumns = @JoinColumn(name = "user_id"),

            inverseJoinColumns = @JoinColumn(name = "request_id")

    )

    @JsonIgnoreProperties("helpers")

    private List<Request> helpedRequests;



    private Integer rating;



    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "birth_date")
    private LocalDate birthDate;



    // Геттеры и сеттеры

    public Long getId() {

        return id;

    }



    public void setId(Long id) {

        this.id = id;

    }



    public String getName() {

        return name;

    }



    public void setName(String name) {

        this.name = name;

    }



    public String getEmail() {

        return email;

    }



    public void setEmail(String email) {

        this.email = email;

    }



    public String getPassword() {

        return password;

    }



    public void setPassword(String password) {

        this.password = password;

    }



    public List<Request> getCreatedRequests() {

        return createdRequests;

    }



    public void setCreatedRequests(List<Request> createdRequests) {

        this.createdRequests = createdRequests;

    }



    public List<Request> getHelpedRequests() {

        return helpedRequests;

    }



    public void setHelpedRequests(List<Request> helpedRequests) {

        this.helpedRequests = helpedRequests;

    }



    public Integer getRating() {

        return rating;

    }



    public void setRating(Integer rating) {

        this.rating = rating;

    }



    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

}
