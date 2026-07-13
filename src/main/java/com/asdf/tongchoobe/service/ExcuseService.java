package com.asdf.tongchoobe.service;

import com.asdf.tongchoobe.domain.Excuse;
import com.asdf.tongchoobe.domain.ExcuseAftermath;
import com.asdf.tongchoobe.domain.ExcuseRememberItem;
import com.asdf.tongchoobe.domain.ExcuseRiskFactor;
import com.asdf.tongchoobe.domain.EvolveDirection;
import com.asdf.tongchoobe.domain.Tone;
import com.asdf.tongchoobe.domain.User;
import com.asdf.tongchoobe.dto.request.ExcuseCreateRequest;
import com.asdf.tongchoobe.dto.request.ExcuseEvolveRequest;
import com.asdf.tongchoobe.dto.request.ExcuseReplyRequest;
import com.asdf.tongchoobe.dto.response.ExcuseResponse;
import com.asdf.tongchoobe.dto.response.ExcuseSummaryResponse;
import com.asdf.tongchoobe.dto.response.PageResponse;
import com.asdf.tongchoobe.exception.BusinessException;
import com.asdf.tongchoobe.exception.ErrorCode;
import com.asdf.tongchoobe.llm.FastApiClient;
import com.asdf.tongchoobe.repository.ExcuseAftermathRepository;
import com.asdf.tongchoobe.repository.ExcuseRememberItemRepository;
import com.asdf.tongchoobe.repository.ExcuseRepository;
import com.asdf.tongchoobe.repository.ExcuseRiskFactorRepository;
import com.asdf.tongchoobe.repository.UserRepository;
import com.asdf.tongchoobe.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExcuseService {
    private final UserRepository userRepository;
    private final ExcuseRepository excuseRepository;
    private final ExcuseRiskFactorRepository riskFactorRepository;
    private final ExcuseRememberItemRepository rememberItemRepository;
    private final ExcuseAftermathRepository aftermathRepository;
    private final FastApiClient fastApiClient;

    @Transactional
    public ExcuseResponse createExcuse(ExcuseCreateRequest request, CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        FastApiClient.GeneratedExcuse generated = fastApiClient.create(
                new FastApiClient.CreateRequest(request.getSituation(), request.getTarget(), request.getTone()));
        int earnedXp = calculateEarnedXp(generated.successRate(), generated.realism(), generated.persuasion(), request.getTone());

        Excuse excuse = excuseRepository.save(Excuse.builder()
                .user(user)
                .situation(request.getSituation())
                .target(request.getTarget())
                .tone(request.getTone())
                .excuseText(generated.excuseText())
                .roundNumber(1)
                .successRate(generated.successRate())
                .realism(generated.realism())
                .persuasion(generated.persuasion())
                .suspicionLevel(generated.suspicionLevel())
                .earnedXp(earnedXp)
                .build());

        List<ExcuseRiskFactor> riskFactors = riskFactorRepository.saveAll(toRiskFactors(excuse, generated.riskFactors()));
        List<ExcuseRememberItem> rememberItems = rememberItemRepository.saveAll(toRememberItems(excuse, generated.rememberItems()));
        List<ExcuseAftermath> aftermaths = aftermathRepository.saveAll(toAftermaths(excuse, generated.aftermaths()));

        user.gainXp(earnedXp);
        user.recordExcuseCreation();

        return ExcuseResponse.from(excuse, riskFactors, rememberItems, aftermaths, null, generated.replyOptions());
    }

    public PageResponse<ExcuseSummaryResponse> getMyExcuses(CustomUserDetails userDetails, int page, int size) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(Math.max(page, 0), clamp(size, 1, 50));
        Map<Long, Excuse> latestByRootId = new LinkedHashMap<>();
        excuseRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .forEach(excuse -> latestByRootId.putIfAbsent(rootOf(excuse).getId(), excuse));

        List<Excuse> latestExcuses = new ArrayList<>(latestByRootId.values());
        int start = Math.min((int) pageable.getOffset(), latestExcuses.size());
        int end = Math.min(start + pageable.getPageSize(), latestExcuses.size());
        Page<Excuse> latestPage = new PageImpl<>(latestExcuses.subList(start, end), pageable, latestExcuses.size());

        Page<ExcuseSummaryResponse> summaries = latestPage
                .map(excuse -> ExcuseSummaryResponse.from(
                        excuse,
                        aftermathRepository.findByExcuseIdOrderBySortOrderAsc(excuse.getId())
                ));

        return PageResponse.of(summaries);
    }

    public ExcuseResponse getExcuse(Long excuseId, CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Excuse excuse = excuseRepository.findById(excuseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXCUSE_NOT_FOUND));

        validateOwner(excuse, user);

        return ExcuseResponse.from(
                excuse,
                riskFactorRepository.findByExcuseIdOrderBySortOrderAsc(excuse.getId()),
                rememberItemRepository.findByExcuseIdOrderBySortOrderAsc(excuse.getId()),
                aftermathRepository.findByExcuseIdOrderBySortOrderAsc(excuse.getId()),
                null
        );
    }

    @Transactional
    public ExcuseResponse evolveExcuse(Long excuseId, ExcuseEvolveRequest request, CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Excuse parent = excuseRepository.findById(excuseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXCUSE_NOT_FOUND));

        validateOwner(parent, user);

        FastApiClient.GeneratedExcuse evolved = fastApiClient.evolve(new FastApiClient.EvolveRequest(
                parent.getSituation(), parent.getTarget(), parent.getTone(), rootExcuse(parent),
                parent.getExcuseText(), conversation(parent), parent.getRoundNumber(), evolveDirectionLabel(request.getDirection())));
        int earnedXp = calculateEarnedXp(evolved.successRate(), evolved.realism(), evolved.persuasion(), parent.getTone());

        Excuse excuse = excuseRepository.save(Excuse.builder()
                .user(user)
                .parent(parent)
                .situation(parent.getSituation())
                .target(parent.getTarget())
                .tone(parent.getTone())
                .excuseText(evolved.excuseText())
                .roundNumber(parent.getRoundNumber())
                .successRate(evolved.successRate())
                .realism(evolved.realism())
                .persuasion(evolved.persuasion())
                .suspicionLevel(evolved.suspicionLevel())
                .earnedXp(earnedXp)
                .build());

        List<ExcuseRiskFactor> riskFactors = riskFactorRepository.saveAll(toRiskFactors(excuse, evolved.riskFactors()));
        List<ExcuseRememberItem> rememberItems = rememberItemRepository.saveAll(toRememberItems(excuse, evolved.rememberItems()));
        List<ExcuseAftermath> aftermaths = aftermathRepository.saveAll(toAftermaths(excuse, evolved.aftermaths()));

        user.gainXp(earnedXp);

        return ExcuseResponse.from(
                excuse,
                riskFactors,
                rememberItems,
                aftermaths,
                buildComplexityWarning(parent),
                evolved.replyOptions()
        );
    }

    @Transactional
    public ExcuseResponse replyToExcuse(Long excuseId, ExcuseReplyRequest request, CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Excuse previous = excuseRepository.findById(excuseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXCUSE_NOT_FOUND));

        validateOwner(previous, user);

        // FastAPI와 프론트가 모두 5라운드를 상한으로 사용한다. Spring만 10라운드를
        // 허용하면 6번째 호출이 FastAPI 422로 끝나므로, 호출 전에 같은 정책으로 막는다.
        if (previous.getRoundNumber() >= 5) {
            throw new BusinessException(ErrorCode.MAX_REPLY_ROUND_REACHED);
        }

        String selectedExcuse = request.getCurrentExcuse();
        if (selectedExcuse != null && !selectedExcuse.isBlank()) {
            previous.setExcuseText(selectedExcuse.trim());
        }

        String incomingMessage = request.getIncomingMessage().trim();
        List<FastApiClient.ConversationTurn> conversation = conversation(previous);
        // 현재 질문은 아직 DB에 저장된 계보에는 없으므로, AI가 반드시 최신 질문으로
        // 인식할 수 있도록 이전 대화의 마지막 user turn으로 추가한다.
        conversation.add(new FastApiClient.ConversationTurn("user", incomingMessage));

        FastApiClient.GeneratedExcuse reply = fastApiClient.reply(new FastApiClient.ReplyRequest(
                previous.getSituation(), previous.getTarget(), previous.getTone(), rootExcuse(previous),
                previous.getExcuseText(), conversation, previous.getRoundNumber() + 1,
                incomingMessage));
        int earnedXp = calculateEarnedXp(reply.successRate(), reply.realism(), reply.persuasion(), previous.getTone());

        Excuse excuse = excuseRepository.save(Excuse.builder()
                .user(user)
                .replyToExcuse(previous)
                .situation(previous.getSituation())
                .target(previous.getTarget())
                .tone(previous.getTone())
                .excuseText(reply.excuseText())
                .incomingMessage(incomingMessage)
                .roundNumber(previous.getRoundNumber() + 1)
                .successRate(reply.successRate())
                .realism(reply.realism())
                .persuasion(reply.persuasion())
                .suspicionLevel(reply.suspicionLevel())
                .earnedXp(earnedXp)
                .build());

        List<ExcuseRiskFactor> riskFactors = riskFactorRepository.saveAll(toRiskFactors(excuse, reply.riskFactors()));
        List<ExcuseRememberItem> rememberItems = rememberItemRepository.saveAll(toRememberItems(excuse, reply.rememberItems()));
        List<ExcuseAftermath> aftermaths = aftermathRepository.saveAll(toAftermaths(excuse, reply.aftermaths()));

        user.gainXp(earnedXp);

        return ExcuseResponse.from(
                excuse,
                riskFactors,
                rememberItems,
                aftermaths,
                buildReplyComplexityWarning(previous),
                reply.replyOptions()
        );
    }

    private List<ExcuseRiskFactor> toRiskFactors(Excuse excuse, List<FastApiClient.Item> items) {
        return items.stream()
                .map(item -> ExcuseRiskFactor.builder()
                        .excuse(excuse)
                        .content(item.content())
                        .sortOrder(item.sortOrder())
                        .build())
                .toList();
    }

    private List<ExcuseRememberItem> toRememberItems(Excuse excuse, List<FastApiClient.Item> items) {
        return items.stream()
                .map(item -> ExcuseRememberItem.builder()
                        .excuse(excuse)
                        .content(item.content())
                        .sortOrder(item.sortOrder())
                        .build())
                .toList();
    }

    private List<ExcuseAftermath> toAftermaths(Excuse excuse, List<FastApiClient.Aftermath> items) {
        return items.stream()
                .map(item -> ExcuseAftermath.builder()
                        .excuse(excuse)
                        .whenLabel(item.whenLabel())
                        .dayOffset(item.dayOffset())
                        .question(item.question())
                        .collapseRate(item.collapseRate())
                        .sortOrder(item.sortOrder())
                        .build())
                .toList();
    }

    private String rootExcuse(Excuse excuse) {
        Excuse root = rootOf(excuse);
        return root.getExcuseText();
    }

    private Excuse rootOf(Excuse excuse) {
        Excuse cursor = excuse;
        while (parentOf(cursor) != null) {
            cursor = parentOf(cursor);
        }
        return cursor;
    }

    private List<FastApiClient.ConversationTurn> conversation(Excuse current) {
        List<Excuse> lineage = new ArrayList<>();
        Excuse cursor = current;
        while (cursor != null) {
            lineage.add(cursor);
            cursor = parentOf(cursor);
        }
        Collections.reverse(lineage);

        List<FastApiClient.ConversationTurn> conversation = new ArrayList<>();
        for (Excuse excuse : lineage) {
            if (excuse.getIncomingMessage() != null && !excuse.getIncomingMessage().isBlank()) {
                conversation.add(new FastApiClient.ConversationTurn("user", excuse.getIncomingMessage()));
            }
            conversation.add(new FastApiClient.ConversationTurn("assistant", excuse.getExcuseText()));
        }
        return conversation;
    }

    private Excuse parentOf(Excuse excuse) {
        return excuse.getParent() != null ? excuse.getParent() : excuse.getReplyToExcuse();
    }

    private void validateOwner(Excuse excuse, User user) {
        if (!excuse.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.EXCUSE_ACCESS_DENIED);
        }
    }

    private int calculateEarnedXp(int successRate, int realism, int persuasion, Tone tone) {
        return (int) Math.round(successRate * 0.6 + realism * 8 + persuasion * 8) + tone.getXpBonus();
    }

    private String evolveDirectionLabel(EvolveDirection direction) {
        return switch (direction) {
            case MORE_PLAUSIBLE -> "더 그럴듯하게";
            case MORE_EMOTIONAL -> "더 감성적으로";
            case SHORTER -> "더 짧게";
            case DODGE_BLAME -> "책임 회피";
            case MORE_SHAMELESS -> "더 뻔뻔하게";
        };
    }

    private ExcuseResponse.ComplexityWarningResponse buildComplexityWarning(Excuse parent) {
        int depth = calculateParentDepth(parent);
        if (depth <= 1) {
            return ExcuseResponse.ComplexityWarningResponse.builder()
                    .enabled(false)
                    .message("현재 변명의 설정은 단순한 상태입니다.")
                    .build();
        }
        if (depth <= 3) {
            return ExcuseResponse.ComplexityWarningResponse.builder()
                    .enabled(true)
                    .message("변명이 여러 번 수정되어 원본과 내용이 충돌할 가능성이 있습니다.")
                    .build();
        }
        return ExcuseResponse.ComplexityWarningResponse.builder()
                .enabled(true)
                .message("변명 수정 횟수가 많아 설정 간 충돌 가능성이 높습니다. 추가 수정은 중단하고 사실을 인정하거나 구체적인 수습 방법을 전달하는 것을 권장합니다.")
                .build();
    }

    private ExcuseResponse.ComplexityWarningResponse buildReplyComplexityWarning(Excuse previous) {
        int roundNumber = previous.getRoundNumber() + 1;
        if (roundNumber <= 2) {
            return ExcuseResponse.ComplexityWarningResponse.builder()
                    .enabled(false)
                    .message("현재 답장 단계에서는 이전 대화 내용을 안정적으로 유지할 수 있습니다.")
                    .build();
        }
        if (roundNumber <= 4) {
            return ExcuseResponse.ComplexityWarningResponse.builder()
                    .enabled(true)
                    .message("답장 대화가 길어져 이전 내용과 충돌할 가능성이 있습니다. 새로운 내용을 추가하기 전에 기존 답변의 이유와 약속을 확인해 주세요.")
                    .build();
        }
        return ExcuseResponse.ComplexityWarningResponse.builder()
                .enabled(true)
                .message("답장 가능 횟수의 마지막 단계입니다. 추가 변명보다는 사실을 인정하고 구체적인 해결 방법을 전달하는 것을 권장합니다.")
                .build();
    }

    private int calculateParentDepth(Excuse excuse) {
        int depth = 1;
        Excuse cursor = excuse;
        while (cursor.getParent() != null) {
            depth++;
            cursor = cursor.getParent();
        }
        return depth;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
