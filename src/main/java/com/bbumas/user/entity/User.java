package com.bbumas.user.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "`user`")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String username;
    
    private String password;
    
    private String email;
    
    private boolean special;
    
    private LocalDateTime createdAt;

    private String name;

    private String phone;

    private boolean isStaff;
    
    @Transient // DB에는 저장되지 않음
    private String passwordCheck;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Lombok이 작동하지 않는 경우를 대비한 직접 구현 Builder
    public static UserBuilder builder() {
        return new UserBuilder();
    }
    
    public static class UserBuilder {
        private String username;
        private String password;
        private String email;
        private boolean special;
        private String name;

        private String phone;
        private boolean isStaff;
        private LocalDateTime createdAt;

        public UserBuilder username(String username) {
            this.username = username;
            return this;
        }
        
        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }
        
        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }
        
        public UserBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UserBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public UserBuilder passwordCheck(String passwordCheck) {
            this.passwordCheck = passwordCheck;
            return this;
        }

        public UserBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserBuilder special(boolean special) {
            this.special = special;
            return this;
        }

        public UserBuilder isStaff(boolean isStaff) {
            this.isStaff = isStaff;
            return this;
        }
        
        public User build() {
            User user = new User();
            user.username = this.username;
            user.password = this.password;
            user.email = this.email;
            user.special = this.special;
            user.name = this.name;
            user.phone = this.phone;
            user.isStaff = this.isStaff;
            user.createdAt = LocalDateTime.now(); // createdAt은 DB에 저장될 때 자동으로 설정됨
            return user;
        }

    }
    
    // 기본 getter/setter - Lombok이 작동하지 않는 경우를 대비
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isSpecial() {
        return special;
    }
    
    public void setSpecial(boolean special) {
        this.special = special;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}