package com.reddot.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.reddot.app.entity.enumeration.STATUS;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Getter
@Setter
@MappedSuperclass
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseEntity implements Persistable<Integer>, Serializable {
    @Transient
    @EqualsAndHashCode.Include
    @JsonIgnore
    // this is a temporary key used for the equals and hashcode methods
    private final String temporaryKey = UUID.randomUUID().toString();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private STATUS status = STATUS.PUBLIC;

    @Transient
    private boolean isNew = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // generic filter list method
    public static <T> List<T> filterList(List<T> items, Predicate<T> predicate) {
        if (items == null)
            return Collections.emptyList();
        return items.stream().filter(predicate).collect(Collectors.toList());
    }

    @JsonIgnore
    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @PrePersist
    public void prePersist() {
        this.isNew = false;
        LocalDateTime now = LocalDateTime.now();
        this.setCreatedAt(now);
    }

    @PreUpdate
    public void preUpdate() {
        LocalDateTime now = LocalDateTime.now();
        this.setUpdatedAt(now);
    }

}

