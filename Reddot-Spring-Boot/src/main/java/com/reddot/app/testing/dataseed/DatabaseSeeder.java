package com.reddot.app.testing.dataseed;

import com.github.javafaker.Faker;
import com.reddot.app.entity.*;
import com.reddot.app.entity.enumeration.ROLENAME;
import com.reddot.app.entity.enumeration.VOTETYPE;
import com.reddot.app.exception.ResourceNotFoundException;
import com.reddot.app.repository.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Log4j2
@Component
@Transactional
public class DatabaseSeeder implements CommandLineRunner {
    private final PasswordEncoder encoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BadgeRepository badgeRepository;
    private final TagRepository tagRepository;
    private final VoteTypeRepository voteTypeRepository;
    private final QuestionRepository questionRepository;
    private final VoteRepository voteRepository;
    private final CommentRepository commentRepository;
    private final BookmarkRepository bookmarkRepository;
    private final String userName = "test user";
    private final String adminName = "test admin";


    public DatabaseSeeder(PasswordEncoder encoder, UserRepository userRepository, RoleRepository roleRepository, BadgeRepository badgeRepository, TagRepository tagRepository, VoteTypeRepository voteTypeRepository, QuestionRepository questionRepository, VoteRepository voteRepository, CommentRepository commentRepository, BookmarkRepository bookmarkRepository) {
        this.encoder = encoder;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.badgeRepository = badgeRepository;
        this.tagRepository = tagRepository;
        this.voteTypeRepository = voteTypeRepository;
        this.questionRepository = questionRepository;
        this.voteRepository = voteRepository;
        this.commentRepository = commentRepository;
        this.bookmarkRepository = bookmarkRepository;
    }


    @Transactional
    @Override
    public void run(String... args) {

        if (roleRepository.findAll().isEmpty()) {
            seedRole();
        }

        if (badgeRepository.findAll().isEmpty()) {
            seedBadge();
        }

        if (tagRepository.findAll().isEmpty()) {
            seedTag();
        }

        if (voteTypeRepository.findAll().isEmpty()) {
            seedVoteType();
        }

        if (userRepository.findAll().isEmpty()) {
            seedUser();
        }

        if (questionRepository.findAll().isEmpty()) {
            seedQuestion();
            seedComment();
            seedVote();
            seedBookmark();
        }

        log.info("");
        log.info("<<<<<<< GETTING ALL ROLEs");
        roleRepository.findAll().forEach(role ->
                log.info(role.getName()));

        log.info("<<<<<<< GETTING ALL USERs");
        userRepository.findAll().forEach(u ->
                log.info(u.getUsername()));
    }

    private void seedVoteType() {
        log.info("");
        log.info("########## SEEDING VOTE TYPE ##########");
        voteTypeRepository.save(new VoteType(VOTETYPE.UPVOTE));
        voteTypeRepository.save(new VoteType(VOTETYPE.DOWNVOTE));
        log.info("2 VOTE TYPES added to the database.");
    }

    /**
     * Seed the user and related data to the database
     */
    private void seedUser() {
        log.info("");
        log.info("########## SEEDING USER ##########");
        log.info("<<<<<<< GETTING USER ROLEs");
        Role userRole = roleRepository.findByName(ROLENAME.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Role is not found."));
        Role adminRole = roleRepository.findByName(ROLENAME.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Role is not found."));

        log.info("<<<<<<< GETTING NEWBIE BADGE");
        Badge badge = badgeRepository.findByName("Newbie")
                .orElseThrow(() -> new ResourceNotFoundException("Error: Badge is not found."));

        log.info(">>>>>>> INITIALIZING USER DATA");
        Faker faker = new Faker();
        String email = faker.internet().emailAddress();
        String raw = "user";
        String password = encoder.encode(raw);
        String adminEmail = faker.internet().emailAddress();
        String raw2 = "admin";
        String adminPassword = encoder.encode(raw2);


        // seed the user
        log.info(">>>>>>>> SEEDING TEST USER");
        User seedUser = new User(userName, email, password);
        seedUser.setRoles(new HashSet<>(Set.of(userRole)));
        seedUser.addBadge(badge);
        addPerson(faker, seedUser);

        // seed the admin user
        log.info(">>>>>>>>> SEEDING TEST ADMIN USER");
        User seedAdmin = new User(adminName, adminEmail, adminPassword);
        seedAdmin.setRoles(new HashSet<>(Set.of(userRole, adminRole)));
        seedAdmin.addBadge(badge);
        addPerson(faker, seedAdmin);
        userRepository.save(seedAdmin);

        log.info("");
        log.info("########## SEEDING FOLLOW ##########");
        log.info(">>>>>>> {} FOLLOWS {}", seedUser.getUsername(), seedAdmin.getUsername());
        seedUser.follow(seedAdmin);
        userRepository.save(seedUser);
        log.info("USERS ADDED TO THE DATABASE");
        log.warn("DO NOT USE THESE USERS IN PRODUCTION");
        log.info("{} - {} - {}", seedUser.getUsername(), seedUser.getEmail(), raw);
        log.info("{} - {} - {}", seedAdmin.getUsername(), seedAdmin.getEmail(), raw2);
    }

    private void seedComment() {
        User user = userRepository.findByUsername(adminName)
                .orElseThrow(() -> new ResourceNotFoundException("Error: User is not found."));
        Question question = questionRepository.findAll().getFirst();
        log.info("");
        log.info("########## SEEDING COMMENT ##########");
        log.info(">>>>>>>>>> ADMIN COMMENT ON THIS QUESTION ...");
        Faker faker = new Faker();
        Comment comment = new Comment();
        comment.setUser(user);
        comment.setText(faker.lorem().sentence());
        question.addComment(comment);
        commentRepository.save(comment);
        log.info("DONE");
    }

    private void seedVote() {
        User user = userRepository.findByUsername(userName)
                .orElseThrow(() -> new ResourceNotFoundException("Error: User is not found."));
        Question question = questionRepository.findAll().getFirst();
        log.info("");
        log.info("########## SEEDING VOTE ##########");
        log.info(">>>>>>> ADMIN VOTEs THE QUESTION...");
        VoteType upvote = voteTypeRepository.findByType(VOTETYPE.UPVOTE)
                .orElseThrow(() -> new ResourceNotFoundException("Error: VoteType is not found."));
        Vote vote = new Vote();
        vote.setVoteType(upvote);
        vote.setQuestion(question);
        vote.setUser(user);
        voteRepository.save(vote);
        log.info("DONE");
    }

    private void seedBookmark() {
        User user = userRepository.findByUsername(userName)
                .orElseThrow(() -> new ResourceNotFoundException("Error: User is not found."));
        Question question = questionRepository.findAll().getFirst();
        log.info("");
        log.info("########## SEEDING BOOKMARK ##########");
        log.info(">>>>>>> USER bookmarks THE QUESTION");
        Bookmark bookmark = new Bookmark(user, question);
        bookmarkRepository.save(bookmark);
    }

    private void seedQuestion() {
        User user = userRepository.findByUsername(userName)
                .orElseThrow(() -> new ResourceNotFoundException("Error: User is not found."));
        log.info("");
        log.info("########## SEEDING QUESTION ##########");
        log.info(">>>>>>> SEED USER ASKs QUESTION");
        // retrieve tags
        List<Tag> tags = tagRepository.findByNameIn(List.of("java", "spring", "spring-boot"));
        Faker faker = new Faker();

        Question question = new Question();
        question.setBody(faker.lorem().paragraph());
        question.setTitle(faker.lorem().sentence() + "?");
        question.setTags(new HashSet<>(tags));
        user.ask(question);
        questionRepository.save(question);
    }

    /**
     * Add person information to the user
     *
     * @param faker com.github.javafaker.Faker
     * @param user  User
     */
    private void addPerson(Faker faker, User user) {
        Person person = new Person();
        person.setDisplayName(faker.name().fullName());
        person.setAboutMe(faker.lorem().sentence());
        person.setDob(LocalDate.now());
        person.setLocation(faker.address().fullAddress());
        person.setWebsiteUrl(faker.internet().url());
        user.setPerson(person);
    }

    /**
     * Seed the tag table
     */
    private void seedTag() {
        List<String> tags = List.of(
                "java", "spring", "spring-boot", "spring-security", "hibernate", "jpa", "thymeleaf", "html", "css", "javascript", "jquery", "react",
                "angular", "vuejs", "nodejs", "expressjs", "mongodb", "mysql", "postgresql", "mariadb", "docker", "kubernetes", "jenkins", "git", "github", "gitlab", "bitbucket",
                "maven", "gradle", "intellij-idea", "eclipse", "netbeans", "visual-studio-code", "atom", "sublime-text", "vim", "emacs", "android", "ios", "kotlin", "swift", "react-native",
                "flutter", "ionic", "xamarin", "phonegap", "cordova", "pwa", "webassembly", "webgl", "unity3d", "unreal-engine", "blender", "gimp", "inkscape", "photoshop", "illustrator",
                "figma", "sketch", "xd", "zeplin", "invision", "jira", "trello", "asana", "slack", "discord", "microsoft-teams", "zoom", "google-meet", "webex", "skype", "whatsapp", "telegram",
                "signal", "facebook", "twitter", "instagram", "linkedin", "pinterest", "tiktok", "snapchat", "youtube", "twitch", "netflix", "amazon-prime", "hbo", "disney-plus", "spotify",
                "apple-music", "google-play-music", "deezer", "soundcloud", "tunein", "pandora", "youtube-music", "amazon-music", "shazam", "google-play-store", "apple-app-store",
                "microsoft-store", "amazon-app-store", "google-cloud", "aws", "azure", "heroku", "digitalocean", "linode", "vultr", "upcloud", "cloudflare", "fastly", "akamai", "cloudfront",
                "s3", "rds", "ec2", "lambda", "sqs", "s3", "route-53", "elasticache", "dynamodb", "cloudwatch", "cloudtrail", "cloudformation", "cloudfront", "cloudsearch", "cloudhsm");
        log.info("");
        log.info("########## SEEDING TAG ##########");
        Faker faker = new Faker();
        tags.forEach(name -> {
            Tag tag = new Tag();
            tag.setName(name);
            tag.setDescription(faker.lorem().sentence());
            tagRepository.save(tag);
        });
        log.info("{} TAGs added to the database.", tags.size());
    }

    /**
     * Seed the badge table
     */
    private void seedBadge() {
        List<Badge> commonBadges = List.of(
                new Badge("Newbie", "Just joined the community", "bronze"),
                new Badge("Beginner", "Posted your first question", "bronze"),
                new Badge("Intermediate", "Posted 10 questions", "bronze"),
                new Badge("Advanced", "Posted 100 questions", "bronze"),
                new Badge("Expert", "Posted 1000 questions", "bronze"),
                new Badge("Crazy", "Posted 10000 questions", "silver "),
                new Badge("Enthusiast", "Posted 100000 questions", "silver "),
                new Badge("Loyal", "Posted 1000000 questions", "gold"),
                new Badge("Veteran", "Posted 10000000 questions", "gold"),
                new Badge("Legend", "Posted 100000000 questions", "gold"));

        List<Badge> featureBadges = List.of(
                new Badge("Good Question", "Your question has been up voted 10 times", "bronze"),
                new Badge("Great Question", "Your question has been up voted 100 times", "bronze"),
                new Badge("Awesome Question", "Your question has been up voted 1000 times", "bronze"),
                new Badge("Good Answer", "Your answer has been up voted 10 times", "bronze"),
                new Badge("Great Answer", "Your answer has been up voted 100 times", "bronze"),
                new Badge("Awesome Answer", "Your answer has been up voted 1000 times", "bronze"),
                new Badge("Good Comment", "Your comment has been up voted 10 times", "bronze"),
                new Badge("Great Comment", "Your comment has been up  voted 100 times", "bronze"),
                new Badge("Awesome Comment", "Your comment has been up voted 1000 times", "bronze"),
                new Badge("Good Contribution", "Your post has been up voted 10 times", "bronze"),
                new Badge("Great Contribution", "Your post has been up voted 100 times", "bronze"),
                new Badge("Awesome Contribution", "Your post has been up voted 1000 times", "bronze"));

        log.info("");
        log.info("N########## SEEDING BADGE ##########");
        badgeRepository.saveAll(commonBadges);
        badgeRepository.saveAll(featureBadges);
        log.info("{} BADGES added to the database.", commonBadges.size());
    }


    /**
     * Seed the role table
     */
    private void seedRole() {
        List<Role> roles = List.of(
                new Role(ROLENAME.ROLE_USER),
                new Role(ROLENAME.ROLE_MODERATOR),
                new Role(ROLENAME.ROLE_ADMIN)
        );

        log.info("");
        log.info("########## SEEDING ROLE ##########");
        roleRepository.saveAll(roles);
        log.info("3 ROLEs added to the database.");
    }
}
