package com.asdf.tongchoobe.service;

import com.asdf.tongchoobe.domain.User;
import com.asdf.tongchoobe.dto.response.RankResponse;
import com.asdf.tongchoobe.exception.BusinessException;
import com.asdf.tongchoobe.exception.ErrorCode;
import com.asdf.tongchoobe.repository.UserRepository;
import com.asdf.tongchoobe.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankService {
    private final UserRepository userRepository;

    public RankResponse getMyRank(CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return RankResponse.from(user);
    }
}
