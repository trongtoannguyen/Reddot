package com.reddot.app.service.user;

import com.reddot.app.assembler.UserAssembler;
import com.reddot.app.dto.request.ProfileUpdateRequest;
import com.reddot.app.dto.request.RegisterRequest;
import com.reddot.app.dto.request.UpdatePasswordRequest;
import com.reddot.app.dto.response.UserProfileDTO;
import com.reddot.app.entity.*;
import com.reddot.app.entity.enumeration.ROLENAME;
import com.reddot.app.exception.BadRequestException;
import com.reddot.app.exception.EmailNotFoundException;
import com.reddot.app.exception.ResourceNotFoundException;
import com.reddot.app.exception.UserNotFoundException;
import com.reddot.app.repository.*;
import com.reddot.app.service.email.MailSenderManager;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class UserServiceManagerImp implements UserServiceManager {
    private final PasswordEncoder encoder;
    private final MailSenderManager mailSenderManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PersonRepository personRepository;
    private final RecoveryTokenRepository recoveryTokenRepository;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final userDeleteRepository userDeleteRepository;
    private final String fullUrl;
    private final UserAssembler userAssembler;

    public UserServiceManagerImp(@Value("${server.address}") String appDomain,
                                 @Value("${server.port}") String appPort,
                                 @Value("${server.servlet.context-path}") String appPath,
                                 MailSenderManager mailSenderManager, UserRepository userRepository, RoleRepository roleRepository,
                                 PasswordEncoder encoder, RecoveryTokenRepository recoveryTokenRepository,
                                 ConfirmationTokenRepository confirmationTokenRepository, PersonRepository personRepository,
                                 userDeleteRepository userDeleteRepository, UserAssembler userAssembler) {
        this.mailSenderManager = mailSenderManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.recoveryTokenRepository = recoveryTokenRepository;
        this.confirmationTokenRepository = confirmationTokenRepository;
        this.fullUrl = "http://" + appDomain + ":" + appPort + appPath;
        this.personRepository = personRepository;
        this.userDeleteRepository = userDeleteRepository;
        this.userAssembler = userAssembler;
    }


    /**
     * Locates the user based on the username or email.
     *
     * @param param the username/email identifying the user whose data is required.
     * @throws UsernameNotFoundException if the user could not be found.
     */
    @Override
    public UserDetails loadUserByUsername(String param) throws UsernameNotFoundException {
        return userRepository.findByUsernameOrEmail(param, param)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + param));
    }

    @Override
    public User loadUserByEmail(String email) throws EmailNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EmailNotFoundException("User not found with mail address: " + email));
    }

    /**
     * Every user must be User role when created, EVERYONE IS EQUAL
     */
    @Override
    public void userCreate(RegisterRequest request) {
        try {
            List<String> errorMessages = validateUser(request);
            if (!errorMessages.isEmpty()) {
                log.error(String.valueOf(errorMessages));
                throw new Exception(String.valueOf(errorMessages));
            }
            User user = new User(request.getUsername(), request.getEmail(),
                    encoder.encode(request.getPassword())
            );

            ROLENAME roleUser = ROLENAME.ROLE_USER;
            Role role = findRoleByName(roleUser);
            user.addRole(role);
            userRepository.save(user);

            // Send confirmation email
            Assert.notNull(user.getId(), "User id must not be null");
            ConfirmationToken token = new ConfirmationToken(user.getId());

            // Construct the email subject and body in HTML
            String subject = "Reddot Account Confirmation";
            String body = "<html>" +
                    "<body>" +
                    "<h2 style=\"color: #4CAF50;\">Welcome to Reddot, " + user.getUsername() + "!</h2>" +
                    "<p>Thank you for signing up. To confirm your account, please click the link below:</p>" +
                    "<a href='" + fullUrl + "/auth/confirm-account?token=" + token.getToken() + "' " +
                    "style=\"padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px;\">Confirm Account</a>" +
                    "<br><br>" +
                    "<img src='https://www.reddotcorp.com/uploads/1/2/7/5/12752286/reddotlogo.png' alt='Welcome' width='300'/>" +
                    "<p>If you did not request this, please ignore this email.</p>" +
                    "<p>Best Regards,<br>The Reddot Team</p>" +
                    "</body>" +
                    "</html>";

            mailSenderManager.sendEmail(user.getEmail(), subject, body);

            // Save confirmation token
            confirmationTokenRepository.save(token);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public User userConfirm(String token) {
        try {
            ConfirmationToken confirmationToken = confirmationTokenRepository.findByToken(token).orElseThrow(() -> new ResourceNotFoundException("TOKEN_NOT_FOUND"));
            if (confirmationToken.getConfirmedAt() != null) {
                throw new Exception("EMAIL_ALREADY_CONFIRMED");
            }
            User user = getUserById(confirmationToken.getOwnerId());
            user.setEnabled(true);
            user.setEmailVerified(true);

            // Create user profile
            Person person = new Person(user.getUsername());
            user.setPerson(person);
            userRepository.save(user);

            // update confirm token
            confirmationToken.setConfirmedAt(LocalDateTime.now());
            confirmationTokenRepository.save(confirmationToken);

            return user;
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void userDeleteRequest(Integer userId) throws ResourceNotFoundException {
        try {
            if (userDeleteRepository.existsByUserId(userId)) {
                throw new Exception("USER ALREADY IN DELETE QUEUE");
            }
            User user = getUserById(userId);
            UserOnDelete userOnDelete = new UserOnDelete(userId);
            userDeleteRepository.save(userOnDelete);

            // send warning mail in HTML format
            String subject = "Reddot Account Deletion Request";
            String body = "<html>" +
                    "<body>" +
                    "<h2>Your account has been marked for deletion</h2>" +
                    "<p>If you did not request this, please contact us immediately.</p>" +
                    "<br><br>" +
                    "<img src='https://www.reddotcorp.com/uploads/1/2/7/5/12752286/reddotlogo.png' alt='Welcome' width='300'/>" +
                    "<p>Best regards,<br>The Reddot Team</p>" +
                    "</body>" +
                    "</html>";

            mailSenderManager.sendEmail(user.getEmail(), subject, body);
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    @Override
    public void userOnLoginUpdate(@NonNull String email) {
        try {
            User user = getUserByEmail(email);
            user.setLastAccess(LocalDateTime.now());
            userRepository.save(user);

            // remove delete request queue if exists
            if (userDeleteRepository.existsByUserId(user.getId())) {
                userDeleteRepository.removeByUserId(user.getId());
            }
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void emailUpdate(Integer userId, String newEmail) throws ResourceNotFoundException {
        try {
            User user = getUserById(userId);
            if (userExistsByEmail(newEmail)) {
                if (user.getEmail().equals(newEmail) && !user.isEmailVerified()) {
                    throw new Exception("Please check your mail box or spam folder to confirm your email." +
                            " Or you may need to resend the confirmation email.");
                }
                throw new Exception("EMAIL_ALREADY_EXISTS");
            }
            user.setEmail(newEmail);
            user.setEmailVerified(false);
            userRepository.save(user);

            // Send mail confirmation in HTML format
            ConfirmationToken confirmationToken = new ConfirmationToken(user.getId());
            String subject = "Reddot Email Confirmation";
            String body = "<html>" +
                    "<body>" +
                    "<h2>Confirm your new email address</h2>" +
                    "<p>To confirm your new email, click the link below:</p>" +
                    "<a href='" + fullUrl + "/settings/email/confirm?token=" + confirmationToken.getToken() + "' " +
                    "style=\"padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px;\">Confirm Email</a>" +
                    "<br><br>" +
                    "<img src='https://www.reddotcorp.com/uploads/1/2/7/5/12752286/reddotlogo.png' alt='Welcome' width='300'/>" +
                    "<p>Best regards,<br>The Reddot Team</p>" +
                    "</body>" +
                    "</html>";

            mailSenderManager.sendEmail(newEmail, subject, body);

            // Save confirmation token
            confirmationTokenRepository.save(confirmationToken);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void emailConfirm(@NonNull String token) throws ResourceNotFoundException {
        try {
            ConfirmationToken confirmationToken = confirmationTokenRepository.findByToken(token).orElseThrow(() -> new ResourceNotFoundException("TOKEN_NOT_FOUND"));
            if (confirmationToken.getConfirmedAt() != null) {
                throw new Exception("TOKEN_ALREADY_USED");
            }
            if (confirmationToken.getValidBefore().isBefore(LocalDateTime.now())) {
                throw new Exception("TOKEN_EXPIRED");
            }
            User user = getUserById(confirmationToken.getOwnerId());
            user.setEmailVerified(true);
            userRepository.save(user);

            // update confirm token
            confirmationToken.setConfirmedAt(LocalDateTime.now());
            confirmationTokenRepository.save(confirmationToken);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void emailConfirmResend(Integer userId) throws UserNotFoundException, BadRequestException {
        try {
            User user = getUserById(userId);
            if (user.isEmailVerified()) {
                throw new BadRequestException("EMAIL_ALREADY_CONFIRMED");
            }

            // Send HTML email confirmation
            ConfirmationToken confirmationToken = new ConfirmationToken(user.getId());
            String subject = "Reddot email confirmation";

            // HTML body content
            String body = "<html>" +
                    "<body>" +
                    "<h2>Confirm your new email address</h2>" +
                    "<p>To confirm your new email, click the link below:</p>" +
                    "<a href='" + fullUrl + "/settings/email/confirm?token=" + confirmationToken.getToken() + "' " +
                    "style=\"padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px;\">Confirm Email</a>" +
                    "<br><br>" +
                    "<img src='https://www.reddotcorp.com/uploads/1/2/7/5/12752286/reddotlogo.png' alt='Welcome' width='300'/>" +
                    "<p>Best regards,<br>The Reddot Team</p>" +
                    "</body>" +
                    "</html>";

            mailSenderManager.sendEmail(user.getEmail(), subject, body);

            // Save confirmation token
            confirmationTokenRepository.save(confirmationToken);

        } catch (UserNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void pwForgot(String email) throws ResourceNotFoundException {
        try {
            if (!userExistsByEmail(email)) {
                throw new ResourceNotFoundException("EMAIL_NOT_FOUND");
            }
            User user = getUserByEmail(email);

            // send password reset email in HTML format
            RecoveryToken recoveryToken = new RecoveryToken(user.getId());
            String subject = "Reddot Password Reset";
            String body = "<html>" +
                    "<body>" +
                    "<h2>Password Reset Request</h2>" +
                    "<p>To reset your password, click the link below:</p>" +
                    "<a href='" + fullUrl + "/settings/reset-password?token=" + recoveryToken.getToken() + "' " +
                    "style=\"padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px;\">Reset Password</a>" +
                    "<br><br>" +
                    "<img src='https://www.reddotcorp.com/uploads/1/2/7/5/12752286/reddotlogo.png' alt='Welcome' width='300'/>" +
                    "<p>If you did not request this, please ignore this email.</p>" +
                    "<p>Best regards,<br>The Reddot Team</p>" +
                    "</body>" +
                    "</html>";

            mailSenderManager.sendEmail(email, subject, body);

            // save recovery token
            recoveryTokenRepository.save(recoveryToken);
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void pwReset(UpdatePasswordRequest request) throws ResourceNotFoundException, BadRequestException {
        try {
            RecoveryToken recoveryToken = recoveryTokenRepository.findByToken(request.getToken()).orElseThrow(() -> new ResourceNotFoundException("TOKEN_NOT_FOUND"));
            if (recoveryToken.isUsed()) {
                throw new Exception("TOKEN_ALREADY_USED");
            }
            if (recoveryToken.getValidBefore().isBefore(LocalDateTime.now())) {
                throw new Exception("TOKEN_EXPIRED");
            }
            User user = getUserById(recoveryToken.getOwnerId());

            // prevent user from using the old password
            if (encoder.matches(request.getPassword(), user.getPassword())) {
                throw new BadRequestException("You cannot use the old password");
            }
            user.setPassword(encoder.encode(request.getPassword()));
            userRepository.save(user);
            recoveryToken.setUsed(true);
            recoveryTokenRepository.save(recoveryToken);

            // send success email in HTML format
            String subject = "Reddot Password Reset Successful";
            String body = "<html>" +
                    "<body>" +
                    "<h2>Your password has been reset successfully.</h2>" +
                    "<p>If you did not initiate this, please contact support immediately.</p>" +
                    "<br><br>" +
                    "<img src='https://www.reddotcorp.com/uploads/1/2/7/5/12752286/reddotlogo.png' alt='Welcome' width='300'/>" +
                    "<p>Best regards,<br>The Reddot Team</p>" +
                    "</body>" +
                    "</html>";

            mailSenderManager.sendEmail(user.getEmail(), subject, body);
        } catch (ResourceNotFoundException | BadRequestException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UserProfileDTO profileGetById(Integer userId) {
        try {
            User user = getUserById(userId);
            return getUserProfileDTO(user);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UserProfileDTO profileGetByUsername(String username) {
        try {
            User user = getUserByUsername(username);
            return getUserProfileDTO(user);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UserProfileDTO profileUpdate(Integer userId, @Valid ProfileUpdateRequest updateRequest) {
        try {
            User user = getUserById(userId);
            Person person = personRepository.findByUserId(user.getId()).orElse(new Person(user.getUsername()));
            user.setAvatarUrl(updateRequest.getAvatar());

            // Update person from DTO
            updateRequest.updateProfile(user, person);
            userRepository.save(user);
            log.info("User profile updated successfully");

            return userAssembler.toUserProfileDTO(user, person);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private UserProfileDTO getUserProfileDTO(User user) {
        Optional<Person> p = personRepository.findByUserId(user.getId());
        Person person;

        if (p.isPresent()) {
            person = p.get();
        } else {
            person = new Person(user.getUsername());
            user.setPerson(person);
            userRepository.save(user);
        }

        return userAssembler.toUserProfileDTO(user, person);
    }

    private boolean userExistsByEmail(String email) {
        Assert.notNull(email, "Email is null");
        return userRepository.findByEmail(email).isPresent();
    }

    private User getUserById(Integer ownerId) {
        return userRepository.findById(ownerId).orElseThrow(UserNotFoundException::new);
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(UserNotFoundException::new);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);
    }

    private Role findRoleByName(ROLENAME roleName) {
        return roleRepository.findByName(roleName).orElse(null);
    }

    private List<String> validateUser(RegisterRequest user) {
        List<String> messages = new ArrayList<>();
        if (userRepository.existsByUsername(user.getUsername())) {
            messages.add("Username already exists in the system!");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            messages.add("Email already exists in the system");
        }

        return messages;
    }
}
