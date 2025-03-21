package com.reddot.app.service.user;

import com.reddot.app.dto.request.ProfileUpdateRequest;
import com.reddot.app.dto.request.RegisterRequest;
import com.reddot.app.dto.request.UpdatePasswordRequest;
import com.reddot.app.dto.response.UserProfileDTO;
import com.reddot.app.entity.User;
import com.reddot.app.exception.BadRequestException;
import com.reddot.app.exception.EmailNotFoundException;
import com.reddot.app.exception.ResourceNotFoundException;
import com.reddot.app.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Service for managing user operations.
 */
@Component
public interface UserServiceManager extends UserDetailsService {
    /**
     * Locates the user based on the email.
     *
     * @param email the email identifying the user whose data is required.
     * @return a fully populated user record (never <code>null</code>)
     * @throws EmailNotFoundException if the user could not be found.
     */
    User loadUserByEmail(String email) throws EmailNotFoundException;

    void userCreate(RegisterRequest request);

    User userConfirm(String token);

    /**
     * Deletes the user identified by the given id.
     *
     * @param userId the id of the user.
     * @throws ResourceNotFoundException if the user is not found.
     */
    void userDeleteRequest(Integer userId) throws ResourceNotFoundException;

    void userOnLoginUpdate(@NonNull String email);

    /**
     * Gets the user identified by the given id.
     *
     * @param userId the id of the user.
     * @return UserProfileDTO object containing the user details.
     */
    UserProfileDTO profileGetById(Integer userId);

    /**
     * Gets the user identified by the given username.
     *
     * @param username the username of the user.
     * @return UserProfileDTO object containing the user details.
     */
    UserProfileDTO profileGetByUsername(String username);

    /**
     * Updates the user profile.
     *
     * @param userId  the id of the user.
     * @param request ProfileUpdateRequest object containing the updated user details.
     * @return UserProfileDTO object containing the updated user details.
     */
    UserProfileDTO profileUpdate(Integer userId, @Valid ProfileUpdateRequest request);

    void pwForgot(String email) throws ResourceNotFoundException;

    void pwReset(UpdatePasswordRequest request) throws ResourceNotFoundException, BadRequestException;

    void emailUpdate(Integer userId, String newEmail) throws ResourceNotFoundException;

    void emailConfirm(@NonNull String token) throws ResourceNotFoundException;

    void emailConfirmResend(Integer userId) throws UserNotFoundException, BadRequestException;

    void membershipDowngrade(User user);
}
