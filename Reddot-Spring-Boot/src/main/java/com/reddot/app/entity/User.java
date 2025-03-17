package com.reddot.app.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NaturalId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The User class is an entity model object. It represents sensitive information of person account.
 */
@Setter
@Getter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Entity(name = "users")
public class User extends BaseEntity implements UserDetails {
    @Serial
    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    @NaturalId
    @Column(unique = true, nullable = false)
    @EqualsAndHashCode.Include //Business key should be used in equals() and hashCode()
    private String username;

    // TODO: implement hashing mail
    @NaturalId(mutable = true)
    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    @NonNull
    @Column(name = "password_hash")
    private String password;

    private boolean isEnabled;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Lob
    private String avatarUrl;

    private String provider;

    @Column(name = "last_access")
    private LocalDateTime lastAccess;
    @Column(name = "violation_count", nullable = false)
    private int violationCount = 0;
    // it’s unusual to consider the User as a client-side and the Person as the parent-side
    // because the person cannot exist without an actual user.
    // So the User entity should be the parent-side and the Person entity should be the client-side.
    @JsonBackReference
    @OneToOne(mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Person person;

    @ManyToMany(cascade = {
            CascadeType.REFRESH,
            CascadeType.MERGE},
            fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user",
            cascade = {
                    CascadeType.PERSIST,
                    CascadeType.MERGE,
                    CascadeType.DETACH})
    private List<Question> questions = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private Set<Bookmark> bookmarks = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private Set<UserOAuth> userOAuths = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private Set<JwtToken> jwtTokens = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user",
            fetch = FetchType.EAGER,
            cascade = {
                    CascadeType.DETACH,
                    CascadeType.MERGE,
                    CascadeType.REFRESH,
                    CascadeType.REMOVE},
            orphanRemoval = true)
    private Set<UserBadge> userBadges = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "follower",
            cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<Follow> followings = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "followed",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private Set<Follow> followers = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Membership membership;

    public User(@NonNull String username, @NonNull String email, @NonNull String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(role -> new SimpleGrantedAuthority(role.getName().toString())).collect(Collectors.toList());
    }

    public void setPerson(Person person) {
        this.person = person;
        person.setUser(this);
    }

    public void removePerson() {
        if (person != null) {
            person.setUser(null);
            this.person = null;
        }
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    public void addNotification(Notification notification) {
        this.notifications.add(notification);
        notification.setUser(this);
    }

    public void removeNotification(Notification notification) {
        this.notifications.remove(notification);
        notification.setUser(null);
    }

    // TODO: WHERE SHOULD THIS METHOD BE LOCATED?
    public void addBadge(Badge badge) {
        this.userBadges.add(new UserBadge(this, badge));
    }

    public void removeBadge(Badge badge) {
        this.userBadges.removeIf(userBadge -> userBadge.getBadge().equals(badge));
    }

    public void follow(User followed) {
        Follow follow = new Follow(this, followed);
        this.followings.add(follow);
        followed.getFollowers().add(follow);
    }

    public void unfollow(User followed) {
        Follow follow = new Follow(this, followed);
        followings.remove(follow);
        followed.getFollowers().remove(follow);
    }

    public void ask(Question question) {
        question.setUser(this);
        this.questions.add(question);
    }

    // When user is deleted, dissociate all questions in DB
    @PreRemove
    public void removeQuestionsAssociated() {
        for (Question question : questions) {
            question.setUser(null);
        }
        this.questions.clear();
    }

    public void addUserOAuth(UserOAuth userOAuth) {
        this.userOAuths.add(userOAuth);
        userOAuth.setUser(this);
    }

    public void removeUserOAuth(UserOAuth userOAuth) {
        this.userOAuths.remove(userOAuth);
        userOAuth.setUser(null);
    }

    public void addJwtToken(JwtToken jwtToken) {
        this.jwtTokens.add(jwtToken);
        jwtToken.setUser(this);
    }

    public void removeJwtToken(JwtToken jwtToken) {
        this.jwtTokens.remove(jwtToken);
        jwtToken.setUser(null);
    }
}