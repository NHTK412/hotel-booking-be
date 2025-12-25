package com.example.hotelbooking.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class Base {

    @CreatedDate
    @Column(name = "CreateAt")
    private LocalDateTime createAt;

    @LastModifiedDate
    @Column(name = "UpdateAt")
    private LocalDateTime updateAt;

    @Column(name = "IsDeleted")
    private Boolean isDeleted = false;
}
