package com.asdf.tongchoobe.service;

import com.asdf.tongchoobe.domain.Excuse;
import com.asdf.tongchoobe.domain.ExcuseAftermath;
import com.asdf.tongchoobe.domain.ExcuseRememberItem;
import com.asdf.tongchoobe.domain.ExcuseRiskFactor;
import com.asdf.tongchoobe.domain.EvolveDirection;
import com.asdf.tongchoobe.domain.SuspicionLevel;
import com.asdf.tongchoobe.domain.Target;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        return ExcuseResponse.from(excuse, riskFactors, rememberItems, aftermaths, null);
    }

    public PageResponse<ExcuseSummaryResponse> getMyExcuses(CustomUserDetails userDetails, int page, int size) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(Math.max(page, 0), clamp(size, 1, 50));
        Page<ExcuseSummaryResponse> summaries = excuseRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
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

        return ExcuseResponse.from(excuse, riskFactors, rememberItems, aftermaths, buildComplexityWarning(parent));
    }

    @Transactional
    public ExcuseResponse replyToExcuse(Long excuseId, ExcuseReplyRequest request, CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Excuse previous = excuseRepository.findById(excuseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXCUSE_NOT_FOUND));

        validateOwner(previous, user);

        if (previous.getRoundNumber() >= 10) {
            throw new BusinessException(ErrorCode.MAX_REPLY_ROUND_REACHED);
        }

        FastApiClient.GeneratedExcuse reply = fastApiClient.reply(new FastApiClient.ReplyRequest(
                previous.getSituation(), previous.getTarget(), previous.getTone(), rootExcuse(previous),
                previous.getExcuseText(), conversation(previous), previous.getRoundNumber() + 1,
                request.getIncomingMessage().trim()));
        int earnedXp = calculateEarnedXp(reply.successRate(), reply.realism(), reply.persuasion(), previous.getTone());

        Excuse excuse = excuseRepository.save(Excuse.builder()
                .user(user)
                .replyToExcuse(previous)
                .situation(previous.getSituation())
                .target(previous.getTarget())
                .tone(previous.getTone())
                .excuseText(reply.excuseText())
                .incomingMessage(request.getIncomingMessage().trim())
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

        return ExcuseResponse.from(excuse, riskFactors, rememberItems, aftermaths, buildReplyComplexityWarning(previous));
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

    private TemporaryExcuse createTemporaryExcuse(ExcuseCreateRequest request) {
        Tone tone = request.getTone();
        String targetLabel = targetLabel(request.getTarget());
        AnalysisScore score = calculateAnalysisScore(request);

        return switch (tone) {
            case MILD -> new TemporaryExcuse(
                    targetLabel + ", 상황을 제대로 못 맞춘 건 내 실수야. 갑자기 처리해야 할 일이 겹쳐서 판단이 늦었어. 바로 정리해서 다시 맞춰둘게.",
                    score.successRate(),
                    score.realism(),
                    score.persuasion(),
                    score.suspicionLevel()
            );
            case SLICK -> new TemporaryExcuse(
                    targetLabel + ", 이건 내가 일부러 피한 게 아니라 타이밍이 꼬인 쪽에 가까워. 지금 바로 수습 가능한 부분부터 처리해서 흐름 안 끊기게 만들게.",
                    score.successRate(),
                    score.realism(),
                    score.persuasion(),
                    score.suspicionLevel()
            );
            case DESPERATE -> new TemporaryExcuse(
                    targetLabel + ", 진짜 변명처럼 들릴 수 있는데 이번 건 내가 놓치면 안 되는 걸 놓쳤어. 바로 복구하고, 같은 상황 안 생기게 확인 루틴까지 만들게.",
                    score.successRate(),
                    score.realism(),
                    score.persuasion(),
                    score.suspicionLevel()
            );
            case BULLSHIT -> new TemporaryExcuse(
                    targetLabel + ", 이게 설명하면 말도 안 되게 들리는데 상황이 거의 이벤트처럼 겹쳤어. 그래도 결과적으로 내가 책임지고 지금부터 바로 만회할게.",
                    score.successRate(),
                    score.realism(),
                    score.persuasion(),
                    score.suspicionLevel()
            );
        };
    }

    private AnalysisScore calculateAnalysisScore(ExcuseCreateRequest request) {
        int situationLength = request.getSituation().trim().length();
        int targetPenalty = targetRiskPenalty(request.getTarget());

        int baseSuccess = switch (request.getTone()) {
            case MILD -> 78;
            case SLICK -> 74;
            case DESPERATE -> 71;
            case BULLSHIT -> 61;
        };
        int lengthAdjustment = situationLength < 20 ? -6 : situationLength <= 120 ? 4 : -5;
        int successRate = clamp(baseSuccess + lengthAdjustment - targetPenalty, 45, 92);

        int realism = switch (request.getTone()) {
            case MILD -> 4;
            case SLICK -> 3;
            case DESPERATE -> 4;
            case BULLSHIT -> 2;
        };
        if (situationLength >= 30 && situationLength <= 120) {
            realism++;
        }
        if (request.getTone() == Tone.BULLSHIT || situationLength > 160) {
            realism--;
        }
        realism = clamp(realism, 1, 5);

        int persuasion = switch (request.getTone()) {
            case MILD -> 3;
            case SLICK -> 4;
            case DESPERATE -> 4;
            case BULLSHIT -> 5;
        };
        if (request.getTarget() == Target.LOVER || request.getTarget() == Target.PARENT) {
            persuasion++;
        }
        persuasion = clamp(persuasion, 1, 5);

        SuspicionLevel suspicionLevel = calculateSuspicionLevel(successRate, request.getTone());
        return new AnalysisScore(successRate, realism, persuasion, suspicionLevel);
    }

    private int calculateEarnedXp(int successRate, int realism, int persuasion, Tone tone) {
        return (int) Math.round(successRate * 0.6 + realism * 8 + persuasion * 8) + tone.getXpBonus();
    }

    private TemporaryExcuse evolveTemporaryExcuse(Excuse parent, EvolveDirection direction) {
        int successRate = parent.getSuccessRate();
        int realism = parent.getRealism();
        int persuasion = parent.getPersuasion();
        SuspicionLevel suspicionLevel = parent.getSuspicionLevel();

        String evolvedText = switch (direction) {
            case MORE_PLAUSIBLE -> {
                successRate = clamp(successRate + 8, 45, 94);
                realism = clamp(realism + 1, 1, 5);
                suspicionLevel = calculateSuspicionLevel(successRate, parent.getTone());
                yield parent.getExcuseText() + " 확인 가능한 부분은 바로 공유하고, 내가 처리한 내용도 같이 남겨둘게.";
            }
            case MORE_EMOTIONAL -> {
                persuasion = clamp(persuasion + 1, 1, 5);
                successRate = clamp(successRate + 4, 45, 92);
                suspicionLevel = calculateSuspicionLevel(successRate, parent.getTone());
                yield parent.getExcuseText() + " 너한테 부담 준 건 진심으로 미안하고, 이번 건 말보다 행동으로 바로 만회할게.";
            }
            case SHORTER -> {
                successRate = clamp(successRate + 2, 45, 90);
                realism = clamp(realism + 1, 1, 5);
                suspicionLevel = calculateSuspicionLevel(successRate, parent.getTone());
                yield shortenExcuse(parent.getExcuseText());
            }
            case DODGE_BLAME -> {
                persuasion = clamp(persuasion + 1, 1, 5);
                successRate = clamp(successRate - 3, 45, 88);
                suspicionLevel = successRate < 72 ? SuspicionLevel.HIGH : SuspicionLevel.MEDIUM;
                yield parent.getExcuseText() + " 다만 이건 내 의지만으로 바로 통제하기 어려운 변수가 같이 있었어.";
            }
            case MORE_SHAMELESS -> {
                persuasion = 5;
                realism = clamp(realism - 1, 1, 5);
                successRate = clamp(successRate - 7, 45, 84);
                suspicionLevel = SuspicionLevel.HIGH;
                yield parent.getExcuseText() + " 솔직히 이 정도면 나도 상황한테 당한 쪽이라, 지금부터 수습하는 걸로 봐줘.";
            }
        };

        return new TemporaryExcuse(evolvedText, successRate, realism, persuasion, suspicionLevel);
    }

    private TemporaryExcuse createTemporaryReply(Excuse previous, String incomingMessage) {
        String trimmedMessage = incomingMessage.trim();
        int messagePressure = calculateIncomingPressure(trimmedMessage);

        int successRate = clamp(previous.getSuccessRate() - messagePressure + 4, 38, 90);
        int realism = clamp(previous.getRealism() + (messagePressure >= 10 ? -1 : 0), 1, 5);
        int persuasion = clamp(previous.getPersuasion() + 1, 1, 5);
        SuspicionLevel suspicionLevel = calculateSuspicionLevel(successRate, previous.getTone());

        String replyText = switch (previous.getTarget()) {
            case TEACHER -> "맞아요, 미리 말씀드렸어야 했습니다. 지금은 변명보다 처리 결과가 중요하다고 생각해서, 바로 확인 가능한 내용부터 정리해서 보내드리겠습니다.";
            case PARENT -> "미리 말 못 한 건 제가 잘못했어요. 숨기려던 건 아니고 상황을 정리하고 말하려다 늦어졌어요. 지금부터는 바로 공유할게요.";
            case FRIEND -> "그 말 맞아. 바로 말했어야 했는데 괜히 어물쩍하다가 더 이상해졌어. 지금부터는 솔직하게 말하고 바로 맞출게.";
            case LOVER -> "그렇게 느낄 수 있어. 내가 먼저 말했어야 했는데 늦게 말해서 더 불안하게 만든 것 같아. 지금은 숨기지 않고 정확히 말할게.";
            case TEAM_LEAD -> "맞습니다. 사전 공유가 늦었던 게 제일 큰 문제였습니다. 지금 기준으로 남은 작업과 복구 시간을 바로 정리해서 공유드리겠습니다.";
            case TEAM_MEMBER -> "맞아, 미리 공유했어야 했어. 지금이라도 내가 맡을 부분을 명확히 정리하고 바로 처리할게.";
        };

        if (trimmedMessage.contains("증거") || trimmedMessage.contains("확인") || trimmedMessage.contains("보여")) {
            replyText += " 확인 가능한 자료가 있는 부분은 바로 같이 보여줄게.";
        }

        return new TemporaryExcuse(replyText, successRate, realism, persuasion, suspicionLevel);
    }

    private List<ExcuseRiskFactor> buildTemporaryRiskFactors(Excuse excuse, Target target, Tone tone, int successRate) {
        return List.of(
                ExcuseRiskFactor.builder()
                        .excuse(excuse)
                        .content(temporaryRiskFactor(target, tone))
                        .sortOrder(0)
                        .build(),
                ExcuseRiskFactor.builder()
                        .excuse(excuse)
                        .content(successRate < 65
                                ? "성공률이 낮은 편이라 추가 질문이 오면 바로 사과와 수습 약속으로 전환하는 게 좋아."
                                : "상대가 구체적인 증거를 요구하면 말이 길어질 수 있어.")
                        .sortOrder(1)
                        .build(),
                ExcuseRiskFactor.builder()
                        .excuse(excuse)
                        .content("같은 변명을 반복해서 쓰면 패턴이 잡혀서 의심도가 올라가.")
                        .sortOrder(2)
                        .build()
        );
    }

    private List<ExcuseRememberItem> buildTemporaryRememberItems(Excuse excuse, ExcuseCreateRequest request) {
        return List.of(
                ExcuseRememberItem.builder()
                        .excuse(excuse)
                        .content("처음 말한 이유를 끝까지 유지해.")
                        .sortOrder(0)
                        .build(),
                ExcuseRememberItem.builder()
                        .excuse(excuse)
                        .content("상황 키워드는 '" + summarizeSituation(request.getSituation()) + "'로 기억해.")
                        .sortOrder(1)
                        .build(),
                ExcuseRememberItem.builder()
                        .excuse(excuse)
                        .content("다음 행동 약속을 하나만 짧게 붙여.")
                        .sortOrder(2)
                        .build()
        );
    }

    private List<ExcuseRememberItem> buildEvolvedRememberItems(Excuse excuse, Excuse parent, EvolveDirection direction) {
        return List.of(
                ExcuseRememberItem.builder()
                        .excuse(excuse)
                        .content("원본 변명과 핵심 이유가 충돌하지 않게 유지해.")
                        .sortOrder(0)
                        .build(),
                ExcuseRememberItem.builder()
                        .excuse(excuse)
                        .content("이번 진화 방향은 '" + evolveDirectionLabel(direction) + "'이야.")
                        .sortOrder(1)
                        .build(),
                ExcuseRememberItem.builder()
                        .excuse(excuse)
                        .content("원본 상황 키워드는 '" + summarizeSituation(parent.getSituation()) + "'로 기억해.")
                        .sortOrder(2)
                        .build()
        );
    }

    private List<ExcuseRiskFactor> buildReplyRiskFactors(Excuse excuse, Excuse previous, int successRate) {
        return List.of(
                ExcuseRiskFactor.builder()
                        .excuse(excuse)
                        .content("상대가 이미 한 번 의심했기 때문에 이전 변명과 말이 충돌하면 바로 무너질 수 있어.")
                        .sortOrder(0)
                        .build(),
                ExcuseRiskFactor.builder()
                        .excuse(excuse)
                        .content(successRate < 60
                                ? "현재 라운드는 성공률이 낮아. 추가 변명보다 사과와 수습 약속이 더 안전할 수 있어."
                                : "이번 답장에서는 짧게 인정하고 바로 해결 행동을 붙이는 게 좋아.")
                        .sortOrder(1)
                        .build(),
                ExcuseRiskFactor.builder()
                        .excuse(excuse)
                        .content("현재 대화 라운드는 " + (previous.getRoundNumber() + 1) + "라운드라 설정을 더 늘리면 기억 부담이 커져.")
                        .sortOrder(2)
                        .build()
        );
    }

    private List<ExcuseRememberItem> buildReplyRememberItems(Excuse excuse, Excuse previous, String incomingMessage) {
        return List.of(
                ExcuseRememberItem.builder()
                        .excuse(excuse)
                        .content("직전 변명에서 말한 핵심 이유를 바꾸지 마.")
                        .sortOrder(0)
                        .build(),
                ExcuseRememberItem.builder()
                        .excuse(excuse)
                        .content("상대 답장 키워드는 '" + summarizeSituation(incomingMessage) + "'로 기억해.")
                        .sortOrder(1)
                        .build(),
                ExcuseRememberItem.builder()
                        .excuse(excuse)
                        .content("현재 답장 라운드는 " + (previous.getRoundNumber() + 1) + "라운드야.")
                        .sortOrder(2)
                        .build()
        );
    }

    private String temporaryRiskFactor(Target target, Tone tone) {
        if (tone == Tone.BULLSHIT) {
            return "개소리 모드는 재미는 강하지만 의심도 같이 올라가.";
        }

        return switch (target) {
            case TEACHER -> "선생님은 출석/과제 기록처럼 확인 가능한 정보를 물어볼 수 있어.";
            case PARENT -> "부모님은 말투 변화나 반복되는 패턴에 민감할 수 있어.";
            case FRIEND -> "친구는 장난으로 넘겨도 캡처나 대화 기록을 확인할 수 있어.";
            case LOVER -> "연인은 감정의 진정성을 더 중요하게 볼 수 있어.";
            case TEAM_LEAD -> "팀장은 일정과 결과물을 기준으로 다시 확인할 수 있어.";
            case TEAM_MEMBER -> "팀원은 실제로 일이 진행됐는지 바로 체감할 수 있어.";
        };
    }

    private List<ExcuseAftermath> buildTemporaryAftermaths(Excuse excuse, Target target, Tone tone, int successRate) {
        return List.of(
                ExcuseAftermath.builder()
                        .excuse(excuse)
                        .whenLabel("오늘 밤")
                        .dayOffset(0)
                        .question(temporaryImmediateAftermathQuestion(target))
                        .collapseRate(calculateCollapseRate(successRate, tone, 0))
                        .sortOrder(0)
                        .build(),
                ExcuseAftermath.builder()
                        .excuse(excuse)
                        .whenLabel("3일 뒤")
                        .dayOffset(3)
                        .question(temporaryAftermathQuestion(target))
                        .collapseRate(calculateCollapseRate(successRate, tone, 3))
                        .sortOrder(1)
                        .build(),
                ExcuseAftermath.builder()
                        .excuse(excuse)
                        .whenLabel("7일 뒤")
                        .dayOffset(7)
                        .question(temporaryLongTermAftermathQuestion(target))
                        .collapseRate(calculateCollapseRate(successRate, tone, 7))
                        .sortOrder(2)
                        .build()
        );
    }

    private String temporaryImmediateAftermathQuestion(Target target) {
        return switch (target) {
            case TEACHER -> "그럼 오늘 안에 어디까지 제출할 수 있어?";
            case PARENT -> "지금 바로 확인할 수 있는 건 없어?";
            case FRIEND -> "그래서 오늘은 어떻게 할 건데?";
            case LOVER -> "지금 솔직하게 말한 거 맞지?";
            case TEAM_LEAD -> "그럼 오늘 안에 복구 가능한 범위는 어디까지야?";
            case TEAM_MEMBER -> "그럼 지금 우리가 뭘 대신하면 돼?";
        };
    }

    private String temporaryAftermathQuestion(Target target) {
        return switch (target) {
            case TEACHER -> "그때 말한 자료나 기록 보여줄 수 있어?";
            case PARENT -> "그날 정확히 어디에 있었어?";
            case FRIEND -> "근데 왜 그때 바로 답장 안 했어?";
            case LOVER -> "솔직히 말하면 다른 이유 있었던 거 아니야?";
            case TEAM_LEAD -> "그럼 지금 완료된 산출물은 어디까지야?";
            case TEAM_MEMBER -> "그럼 다음엔 네가 어느 부분 맡을 거야?";
        };
    }

    private String temporaryLongTermAftermathQuestion(Target target) {
        return switch (target) {
            case TEACHER -> "다음에도 같은 이유가 나오면 어떻게 할 거야?";
            case PARENT -> "요즘 이런 일이 자주 생기는 이유가 뭐야?";
            case FRIEND -> "너 그때 말한 거랑 지금 말이 좀 다른데?";
            case LOVER -> "그때 일 아직 마음에 걸리는데 다시 설명해줄래?";
            case TEAM_LEAD -> "재발 방지 방법은 실제로 적용하고 있어?";
            case TEAM_MEMBER -> "다음 일정에서는 진짜 믿어도 되는 거지?";
        };
    }

    private int calculateCollapseRate(int successRate, Tone tone, int dayOffset) {
        int toneRisk = switch (tone) {
            case MILD -> 4;
            case SLICK -> 10;
            case DESPERATE -> 8;
            case BULLSHIT -> 18;
        };
        int timeRisk = switch (dayOffset) {
            case 0 -> 3;
            case 3 -> 10;
            default -> 16;
        };

        return clamp(100 - successRate + toneRisk + timeRisk, 8, 88);
    }

    private int calculateIncomingPressure(String incomingMessage) {
        int pressure = 0;
        if (incomingMessage.contains("?") || incomingMessage.contains("왜") || incomingMessage.contains("근데")) {
            pressure += 5;
        }
        if (incomingMessage.contains("증거") || incomingMessage.contains("확인") || incomingMessage.contains("보여")) {
            pressure += 8;
        }
        if (incomingMessage.contains("거짓말") || incomingMessage.contains("솔직") || incomingMessage.contains("믿")) {
            pressure += 10;
        }
        return clamp(pressure, 0, 18);
    }

    private SuspicionLevel calculateSuspicionLevel(int successRate, Tone tone) {
        if (tone == Tone.BULLSHIT || successRate < 62) {
            return SuspicionLevel.HIGH;
        }
        if (successRate < 76 || tone == Tone.SLICK || tone == Tone.DESPERATE) {
            return SuspicionLevel.MEDIUM;
        }
        return SuspicionLevel.LOW;
    }

    private int targetRiskPenalty(Target target) {
        return switch (target) {
            case FRIEND -> 0;
            case TEAM_MEMBER -> 2;
            case PARENT -> 3;
            case LOVER -> 4;
            case TEACHER -> 5;
            case TEAM_LEAD -> 6;
        };
    }

    private String summarizeSituation(String situation) {
        String trimmed = situation.trim();
        if (trimmed.length() <= 24) {
            return trimmed;
        }
        return trimmed.substring(0, 24) + "...";
    }

    private String shortenExcuse(String excuseText) {
        String firstSentence = excuseText.split("[.!?。！？]")[0].trim();
        if (firstSentence.length() <= 80) {
            return firstSentence + ". 바로 수습할게.";
        }
        return firstSentence.substring(0, 80) + "... 바로 수습할게.";
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
                    .message("아직 설정이 단순해서 관리하기 쉬운 상태야.")
                    .build();
        }
        if (depth <= 3) {
            return ExcuseResponse.ComplexityWarningResponse.builder()
                    .enabled(true)
                    .message("변명이 여러 번 진화했어. 원본 설정과 말이 충돌하지 않게 조심해.")
                    .build();
        }
        return ExcuseResponse.ComplexityWarningResponse.builder()
                .enabled(true)
                .message("변명 설정이 많이 쌓였어. 이제 추가 진화보다 사과/수습 전략이 더 안전할 수 있어.")
                .build();
    }

    private ExcuseResponse.ComplexityWarningResponse buildReplyComplexityWarning(Excuse previous) {
        int roundNumber = previous.getRoundNumber() + 1;
        if (roundNumber <= 2) {
            return ExcuseResponse.ComplexityWarningResponse.builder()
                    .enabled(false)
                    .message("아직 답장 라운드가 낮아서 설정 관리가 가능한 상태야.")
                    .build();
        }
        if (roundNumber <= 4) {
            return ExcuseResponse.ComplexityWarningResponse.builder()
                    .enabled(true)
                    .message("대화가 길어지고 있어. 이전 답변과 말이 충돌하지 않게 조심해.")
                    .build();
        }
        return ExcuseResponse.ComplexityWarningResponse.builder()
                .enabled(true)
                .message("최대 라운드에 가까워졌어. 추가 변명보다 수습 전략으로 전환하는 게 안전해.")
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

    private String targetLabel(Target target) {
        return switch (target) {
            case TEACHER -> "선생님";
            case PARENT -> "부모님";
            case FRIEND -> "친구야";
            case LOVER -> "자기야";
            case TEAM_LEAD -> "팀장님";
            case TEAM_MEMBER -> "팀원들";
        };
    }

    private record TemporaryExcuse(
            String excuseText,
            int successRate,
            int realism,
            int persuasion,
            SuspicionLevel suspicionLevel
    ) {
    }

    private record AnalysisScore(
            int successRate,
            int realism,
            int persuasion,
            SuspicionLevel suspicionLevel
    ) {
    }
}
