package com.asdf.tongchoobe.service;

import com.asdf.tongchoobe.domain.User;
import com.asdf.tongchoobe.dto.request.NicknameUpdateRequest;
import com.asdf.tongchoobe.dto.request.PasswordUpdateRequest;
import com.asdf.tongchoobe.dto.response.UserProfileResponse;
import com.asdf.tongchoobe.exception.BusinessException;
import com.asdf.tongchoobe.exception.ErrorCode;
import com.asdf.tongchoobe.repository.ExcuseRepository;
import com.asdf.tongchoobe.repository.UserRepository;
import com.asdf.tongchoobe.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final ExcuseRepository excuseRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getMyProfile(CustomUserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateNickname(CustomUserDetails userDetails, NicknameUpdateRequest request) {
        User user = getCurrentUser(userDetails);
        String newNickname = request.getNickname().trim();

        if (!user.getNickname().equals(newNickname) && userRepository.existsByNickname(newNickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        user.changeNickname(newNickname);
        return toProfileResponse(user);
    }

    @Transactional
    public void updatePassword(CustomUserDetails userDetails, PasswordUpdateRequest request) {
        User user = getCurrentUser(userDetails);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    private User getCurrentUser(CustomUserDetails userDetails) {
        return userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private UserProfileResponse toProfileResponse(User user) {
        int originalExcuseCount = Math.toIntExact(
                excuseRepository.countByUserIdAndReplyToExcuseIsNull(user.getId())
        );
        return UserProfileResponse.from(user, originalExcuseCount);
    }
}
