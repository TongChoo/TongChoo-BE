package com.asdf.tongchoobe.service;

import com.asdf.tongchoobe.domain.Excuse;
import com.asdf.tongchoobe.domain.ExcuseReplyOption;
import com.asdf.tongchoobe.domain.SuspicionLevel;
import com.asdf.tongchoobe.domain.Target;
import com.asdf.tongchoobe.domain.Tone;
import com.asdf.tongchoobe.domain.User;
import com.asdf.tongchoobe.dto.request.ExcuseReplyRequest;
import com.asdf.tongchoobe.dto.request.ExcuseSelectionRequest;
import com.asdf.tongchoobe.dto.response.ExcuseSummaryResponse;
import com.asdf.tongchoobe.dto.response.PageResponse;
import com.asdf.tongchoobe.llm.FastApiClient;
import com.asdf.tongchoobe.repository.ExcuseAftermathRepository;
import com.asdf.tongchoobe.repository.ExcuseRememberItemRepository;
import com.asdf.tongchoobe.repository.ExcuseRepository;
import com.asdf.tongchoobe.repository.ExcuseRiskFactorRepository;
import com.asdf.tongchoobe.repository.ExcuseReplyOptionRepository;
import com.asdf.tongchoobe.repository.UserRepository;
import com.asdf.tongchoobe.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcuseServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ExcuseRepository excuseRepository;
    @Mock private ExcuseRiskFactorRepository riskFactorRepository;
    @Mock private ExcuseRememberItemRepository rememberItemRepository;
    @Mock private ExcuseAftermathRepository aftermathRepository;
    @Mock private ExcuseReplyOptionRepository replyOptionRepository;
    @Mock private FastApiClient fastApiClient;

    @InjectMocks private ExcuseService excuseService;

    @Test
    void historyReturnsLatestStateForEachRootConversation() {
        User user = user();
        Excuse root = excuse(1L, user, null, 1, Instant.parse("2026-07-14T00:00:00Z"));
        Excuse reply2 = excuse(2L, user, root, 2, Instant.parse("2026-07-14T00:01:00Z"));
        Excuse reply3 = excuse(3L, user, reply2, 3, Instant.parse("2026-07-14T00:02:00Z"));
        Excuse otherRoot = excuse(4L, user, null, 1, Instant.parse("2026-07-14T00:01:30Z"));

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(excuseRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(reply3, otherRoot, reply2, root));
        when(aftermathRepository.findByExcuseIdOrderBySortOrderAsc(any())).thenReturn(List.of());

        PageResponse<ExcuseSummaryResponse> result = excuseService.getMyExcuses(
                new CustomUserDetails(user), 0, 10
        );

        assertEquals(2, result.getTotalElements());
        assertEquals(3L, result.getContent().getFirst().getId());
        assertEquals(3, result.getContent().getFirst().getRoundNumber());
        assertEquals(4L, result.getContent().get(1).getId());
    }

    @Test
    void selectedReplyBecomesPersistedCurrentContextBeforeNextRound() throws Exception {
        User user = user();
        Excuse previous = excuse(1L, user, null, 1, Instant.parse("2026-07-14T00:00:00Z"));
        previous.setTarget(Target.CUSTOM);
        previous.setTargetDescription("같은 프로젝트를 진행하는 친한 선배");
        ExcuseReplyRequest request = new ObjectMapper().readValue(
                """
                {
                  "incomingMessage": "그래서 지금 어떻게 할 건가요?",
                  "currentExcuse": "제가 고른 후보 답장입니다. 지금 바로 수정해서 공유할게요."
                }
                """,
                ExcuseReplyRequest.class
        );

        FastApiClient.GeneratedExcuse generated = new FastApiClient.GeneratedExcuse(
                "지금 수정하고 있고 10분 안에 공유하겠습니다.",
                60,
                4,
                4,
                SuspicionLevel.MEDIUM,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("10분 안에 공유할게요.", "수정해서 바로 공유드리겠습니다.")
        );

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(excuseRepository.findByIdForUpdate(previous.getId())).thenReturn(Optional.of(previous));
        when(excuseRepository.existsByReplyToExcuseId(previous.getId())).thenReturn(false);
        when(replyOptionRepository.findByExcuseIdOrderBySortOrderAsc(previous.getId()))
                .thenReturn(List.of(option(previous, request.getCurrentExcuse(), true)));
        when(fastApiClient.reply(any())).thenReturn(generated);
        when(excuseRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(riskFactorRepository.saveAll(any())).thenReturn(List.of());
        when(rememberItemRepository.saveAll(any())).thenReturn(List.of());
        when(aftermathRepository.saveAll(any())).thenReturn(List.of());
        when(replyOptionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        excuseService.replyToExcuse(previous.getId(), request, new CustomUserDetails(user));

        ArgumentCaptor<FastApiClient.ReplyRequest> captor = ArgumentCaptor.forClass(FastApiClient.ReplyRequest.class);
        org.mockito.Mockito.verify(fastApiClient).reply(captor.capture());

        assertEquals(request.getCurrentExcuse(), previous.getExcuseText());
        assertEquals(request.getCurrentExcuse(), captor.getValue().currentExcuse());
        assertEquals("같은 프로젝트를 진행하는 친한 선배", captor.getValue().targetDescription());
        assertEquals(request.getCurrentExcuse(), captor.getValue().conversation().getFirst().content());
    }

    @Test
    void replyFromNonLatestRoundIsRejectedBeforeCallingAi() throws Exception {
        User user = user();
        Excuse previous = excuse(1L, user, null, 1, Instant.parse("2026-07-14T00:00:00Z"));
        ExcuseReplyRequest request = new ObjectMapper().readValue(
                """
                {
                  "incomingMessage": "그래서 어떻게 할 건가요?",
                  "currentExcuse": "기존 답장입니다."
                }
                """,
                ExcuseReplyRequest.class
        );

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(excuseRepository.findByIdForUpdate(previous.getId())).thenReturn(Optional.of(previous));
        when(excuseRepository.existsByReplyToExcuseId(previous.getId())).thenReturn(true);

        com.asdf.tongchoobe.exception.BusinessException error = assertThrows(
                com.asdf.tongchoobe.exception.BusinessException.class,
                () -> excuseService.replyToExcuse(previous.getId(), request, new CustomUserDetails(user))
        );

        assertEquals(com.asdf.tongchoobe.exception.ErrorCode.EXCUSE_NOT_LATEST, error.getErrorCode());
        verify(fastApiClient, org.mockito.Mockito.never()).reply(any());
    }

    @Test
    void selectedCandidateIsPersistedImmediately() throws Exception {
        User user = user();
        Excuse current = excuse(2L, user, null, 2, Instant.parse("2026-07-14T00:00:00Z"));
        ExcuseReplyOption first = option(current, "첫 번째 후보", true);
        ExcuseReplyOption second = ExcuseReplyOption.builder()
                .id(101L)
                .excuse(current)
                .optionText("두 번째 후보")
                .sortOrder(1)
                .selected(false)
                .build();
        ExcuseSelectionRequest request = new ObjectMapper().readValue(
                "{\"selectedExcuse\":\"두 번째 후보\"}",
                ExcuseSelectionRequest.class
        );

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(excuseRepository.findByIdForUpdate(current.getId())).thenReturn(Optional.of(current));
        when(excuseRepository.existsByReplyToExcuseId(current.getId())).thenReturn(false);
        when(replyOptionRepository.findByExcuseIdOrderBySortOrderAsc(current.getId()))
                .thenReturn(List.of(first, second));
        when(riskFactorRepository.findByExcuseIdOrderBySortOrderAsc(current.getId())).thenReturn(List.of());
        when(rememberItemRepository.findByExcuseIdOrderBySortOrderAsc(current.getId())).thenReturn(List.of());
        when(aftermathRepository.findByExcuseIdOrderBySortOrderAsc(current.getId())).thenReturn(List.of());

        com.asdf.tongchoobe.dto.response.ExcuseResponse response = excuseService.selectReplyOption(
                current.getId(), request, new CustomUserDetails(user)
        );

        assertEquals("두 번째 후보", current.getExcuseText());
        assertEquals(false, first.isSelected());
        assertEquals(true, second.isSelected());
        assertEquals(1, response.getSelectedOptionIndex());
    }

    private User user() {
        return User.builder()
                .id(10L)
                .email("test@example.com")
                .password("password")
                .nickname("tester")
                .build();
    }

    private Excuse excuse(
            Long id,
            User user,
            Excuse replyTo,
            int roundNumber,
            Instant createdAt
    ) {
        return Excuse.builder()
                .id(id)
                .user(user)
                .replyToExcuse(replyTo)
                .situation("팀 회의에 늦었다")
                .target(Target.TEAM_LEAD)
                .tone(Tone.MILD)
                .excuseText("기존 답장입니다.")
                .roundNumber(roundNumber)
                .successRate(50)
                .realism(3)
                .persuasion(3)
                .suspicionLevel(SuspicionLevel.MEDIUM)
                .createdAt(createdAt)
                .build();
    }

    private ExcuseReplyOption option(Excuse excuse, String text, boolean selected) {
        return ExcuseReplyOption.builder()
                .id(100L)
                .excuse(excuse)
                .optionText(text)
                .sortOrder(0)
                .selected(selected)
                .build();
    }
}
