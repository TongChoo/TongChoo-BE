package com.asdf.tongchoobe.service;

import com.asdf.tongchoobe.domain.Excuse;
import com.asdf.tongchoobe.domain.ExcuseAftermath;
import com.asdf.tongchoobe.domain.ExcuseRememberItem;
import com.asdf.tongchoobe.domain.ExcuseRiskFactor;
import com.asdf.tongchoobe.domain.ExcuseReplyOption;
import com.asdf.tongchoobe.domain.Target;
import com.asdf.tongchoobe.domain.Tone;
import com.asdf.tongchoobe.domain.User;
import com.asdf.tongchoobe.dto.request.ExcuseCreateRequest;
import com.asdf.tongchoobe.dto.request.ExcuseReplyRequest;
import com.asdf.tongchoobe.dto.request.ExcuseSelectionRequest;
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
import com.asdf.tongchoobe.repository.ExcuseReplyOptionRepository;
import com.asdf.tongchoobe.repository.UserRepository;
import com.asdf.tongchoobe.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExcuseService {
    private final UserRepository userRepository;
    private final ExcuseRepository excuseRepository;
    private final ExcuseRiskFactorRepository riskFactorRepository;
    private final ExcuseRememberItemRepository rememberItemRepository;
    private final ExcuseAftermathRepository aftermathRepository;
    private final ExcuseReplyOptionRepository replyOptionRepository;
    private final FastApiClient fastApiClient;

    @Transactional
    public ExcuseResponse createExcuse(ExcuseCreateRequest request, CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        FastApiClient.GeneratedExcuse generated = fastApiClient.create(
                new FastApiClient.CreateRequest(
                        request.getSituation(),
                        request.getTarget(),
                        normalizeTargetDescription(request.getTarget(), request.getTargetDescription()),
                        request.getTone()));
        int earnedXp = calculateEarnedXp(generated.successRate(), generated.realism(), generated.persuasion(), request.getTone());

        Excuse excuse = excuseRepository.save(Excuse.builder()
                .user(user)
                .situation(request.getSituation())
                .target(request.getTarget())
                .targetDescription(normalizeTargetDescription(request.getTarget(), request.getTargetDescription()))
                .tone(request.getTone())
                .situationSeverity(generated.situationSeverity())
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
        List<ExcuseReplyOption> replyOptions = saveReplyOptions(excuse, generated.excuseText(), generated.replyOptions());

        user.gainXp(earnedXp);
        return response(excuse, riskFactors, rememberItems, aftermaths, null, replyOptions);
    }

    public PageResponse<ExcuseSummaryResponse> getMyExcuses(CustomUserDetails userDetails, int page, int size) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(Math.max(page, 0), clamp(size, 1, 50));
        Page<Excuse> latestPage = excuseRepository.findLatestConversationRounds(
                user.getId(),
                pageable
        );
        List<Long> excuseIds = latestPage.getContent().stream()
                .map(Excuse::getId)
                .toList();
        Map<Long, List<ExcuseAftermath>> aftermathsByExcuseId = excuseIds.isEmpty()
                ? Map.of()
                : aftermathRepository.findByExcuseIdInOrderByExcuseIdAscSortOrderAsc(excuseIds)
                        .stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                aftermath -> aftermath.getExcuse().getId()
                        ));

        Page<ExcuseSummaryResponse> summaries = latestPage.map(excuse ->
                ExcuseSummaryResponse.from(
                        excuse,
                        aftermathsByExcuseId.getOrDefault(excuse.getId(), List.of())
                )
        );

        return PageResponse.of(summaries);
    }

    public ExcuseResponse getExcuse(Long excuseId, CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Excuse excuse = excuseRepository.findById(excuseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXCUSE_NOT_FOUND));

        validateOwner(excuse, user);

        return response(
                excuse,
                riskFactorRepository.findByExcuseIdOrderBySortOrderAsc(excuse.getId()),
                rememberItemRepository.findByExcuseIdOrderBySortOrderAsc(excuse.getId()),
                aftermathRepository.findByExcuseIdOrderBySortOrderAsc(excuse.getId()),
                null,
                replyOptionRepository.findByExcuseIdOrderBySortOrderAsc(excuse.getId())
        );
    }

    @Transactional
    public ExcuseResponse selectReplyOption(
            Long excuseId,
            ExcuseSelectionRequest request,
            CustomUserDetails userDetails
    ) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Excuse excuse = excuseRepository.findByIdForUpdate(excuseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXCUSE_NOT_FOUND));

        validateOwner(excuse, user);
        validateLatestRound(excuse);
        List<ExcuseReplyOption> options = applySelection(excuse, request.getSelectedExcuse());

        return response(
                excuse,
                riskFactorRepository.findByExcuseIdOrderBySortOrderAsc(excuse.getId()),
                rememberItemRepository.findByExcuseIdOrderBySortOrderAsc(excuse.getId()),
                aftermathRepository.findByExcuseIdOrderBySortOrderAsc(excuse.getId()),
                null,
                options
        );
    }

    @Transactional
    public ExcuseResponse replyToExcuse(Long excuseId, ExcuseReplyRequest request, CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Excuse previous = excuseRepository.findByIdForUpdate(excuseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXCUSE_NOT_FOUND));

        validateOwner(previous, user);
        validateLatestRound(previous);

        // FastAPI와 프론트가 모두 5라운드를 상한으로 사용한다. Spring만 10라운드를
        // 허용하면 6번째 호출이 FastAPI 422로 끝나므로, 호출 전에 같은 정책으로 막는다.
        if (previous.getRoundNumber() >= 5) {
            throw new BusinessException(ErrorCode.MAX_REPLY_ROUND_REACHED);
        }

        String selectedExcuse = request.getCurrentExcuse();
        if (selectedExcuse != null
                && !selectedExcuse.isBlank()
                && !selectedExcuse.trim().equals(previous.getExcuseText())) {
            applySelection(previous, selectedExcuse);
        }

        String incomingMessage = request.getIncomingMessage().trim();
        List<FastApiClient.ConversationTurn> conversation = previousConversation(previous);

        FastApiClient.GeneratedExcuse reply = fastApiClient.reply(new FastApiClient.ReplyRequest(
                previous.getSituation(), previous.getTarget(), previous.getTargetDescription(), previous.getTone(),
                previous.getSituationSeverity(), previous.getExcuseText(),
                conversation, previous.getRoundNumber() + 1,
                incomingMessage));
        int earnedXp = calculateEarnedXp(reply.successRate(), reply.realism(), reply.persuasion(), previous.getTone());

        Excuse excuse;
        try {
            excuse = excuseRepository.saveAndFlush(Excuse.builder()
                    .user(user)
                    .replyToExcuse(previous)
                    .situation(previous.getSituation())
                    .target(previous.getTarget())
                    .targetDescription(previous.getTargetDescription())
                    .tone(previous.getTone())
                    .situationSeverity(previous.getSituationSeverity())
                    .excuseText(reply.excuseText())
                    .incomingMessage(incomingMessage)
                    .roundNumber(previous.getRoundNumber() + 1)
                    .successRate(reply.successRate())
                    .realism(reply.realism())
                    .persuasion(reply.persuasion())
                    .suspicionLevel(reply.suspicionLevel())
                    .earnedXp(earnedXp)
                    .build());
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.EXCUSE_NOT_LATEST);
        }

        List<ExcuseRiskFactor> riskFactors = riskFactorRepository.saveAll(toRiskFactors(excuse, reply.riskFactors()));
        List<ExcuseRememberItem> rememberItems = rememberItemRepository.saveAll(toRememberItems(excuse, reply.rememberItems()));
        List<ExcuseAftermath> aftermaths = aftermathRepository.saveAll(toAftermaths(excuse, reply.aftermaths()));
        List<ExcuseReplyOption> replyOptions = saveReplyOptions(excuse, reply.excuseText(), reply.replyOptions());

        user.gainXp(earnedXp);

        return response(
                excuse,
                riskFactors,
                rememberItems,
                aftermaths,
                buildReplyComplexityWarning(previous),
                replyOptions
        );
    }

    private void validateLatestRound(Excuse excuse) {
        if (excuseRepository.existsByReplyToExcuseId(excuse.getId())) {
            throw new BusinessException(ErrorCode.EXCUSE_NOT_LATEST);
        }
    }

    private String normalizeTargetDescription(Target target, String description) {
        if (target != Target.CUSTOM || description == null) {
            return null;
        }
        return description.trim();
    }

    private List<ExcuseReplyOption> applySelection(Excuse excuse, String selectedExcuse) {
        String selectedText = selectedExcuse.trim();
        List<ExcuseReplyOption> options = replyOptionRepository
                .findByExcuseIdOrderBySortOrderAsc(excuse.getId());

        if (options.isEmpty()) {
            ExcuseReplyOption legacyOption = replyOptionRepository.save(ExcuseReplyOption.builder()
                    .excuse(excuse)
                    .optionText(selectedText)
                    .sortOrder(0)
                    .selected(true)
                    .build());
            excuse.setExcuseText(selectedText);
            return List.of(legacyOption);
        }

        ExcuseReplyOption selected = options.stream()
                .filter(option -> option.getOptionText().equals(selectedText))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.REPLY_OPTION_NOT_FOUND));

        options.forEach(ExcuseReplyOption::unselect);
        selected.select();
        excuse.setExcuseText(selected.getOptionText());
        return options;
    }

    private List<ExcuseReplyOption> saveReplyOptions(
            Excuse excuse,
            String primary,
            List<String> generatedOptions
    ) {
        Set<String> uniqueOptions = new LinkedHashSet<>();
        if (generatedOptions != null) {
            generatedOptions.stream()
                    .filter(option -> option != null && !option.isBlank())
                    .map(String::trim)
                    .limit(3)
                    .forEach(uniqueOptions::add);
        }
        if (uniqueOptions.size() < 3 && primary != null && !primary.isBlank()) {
            uniqueOptions.add(primary.trim());
        }

        if (uniqueOptions.size() != 3) {
            throw new BusinessException(ErrorCode.LLM_PARSE_ERROR);
        }

        List<ExcuseReplyOption> options = new ArrayList<>();
        int sortOrder = 0;
        for (String option : uniqueOptions) {
            options.add(ExcuseReplyOption.builder()
                    .excuse(excuse)
                    .optionText(option)
                    .sortOrder(sortOrder)
                    .selected(sortOrder == 0)
                    .build());
            sortOrder++;
        }
        return replyOptionRepository.saveAll(options);
    }

    private ExcuseResponse response(
            Excuse excuse,
            List<ExcuseRiskFactor> riskFactors,
            List<ExcuseRememberItem> rememberItems,
            List<ExcuseAftermath> aftermaths,
            ExcuseResponse.ComplexityWarningResponse complexityWarning,
            List<ExcuseReplyOption> options
    ) {
        List<String> optionTexts = options.stream()
                .map(ExcuseReplyOption::getOptionText)
                .toList();
        int selectedOptionIndex = 0;
        for (int index = 0; index < options.size(); index++) {
            if (options.get(index).isSelected()) {
                selectedOptionIndex = index;
                break;
            }
        }
        return ExcuseResponse.from(
                excuse,
                riskFactors,
                rememberItems,
                aftermaths,
                complexityWarning,
                optionTexts,
                selectedOptionIndex
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

    private List<FastApiClient.ConversationTurn> previousConversation(Excuse current) {
        List<Excuse> lineage = new ArrayList<>();
        Excuse cursor = current;
        while (cursor != null) {
            lineage.add(cursor);
            cursor = previousRoundOf(cursor);
        }
        Collections.reverse(lineage);

        List<FastApiClient.ConversationTurn> conversation = new ArrayList<>();
        for (int index = 0; index < lineage.size(); index++) {
            Excuse excuse = lineage.get(index);
            if (excuse.getIncomingMessage() != null && !excuse.getIncomingMessage().isBlank()) {
                conversation.add(new FastApiClient.ConversationTurn("user", excuse.getIncomingMessage()));
            }
            // 현재 선택 답장은 currentExcuse로 별도 전달하므로 대화 기록에 또 넣지 않는다.
            if (index < lineage.size() - 1) {
                conversation.add(new FastApiClient.ConversationTurn("assistant", excuse.getExcuseText()));
            }
        }
        return conversation;
    }

    private Excuse previousRoundOf(Excuse excuse) {
        return excuse.getReplyToExcuse();
    }

    private void validateOwner(Excuse excuse, User user) {
        if (!excuse.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.EXCUSE_ACCESS_DENIED);
        }
    }

    private int calculateEarnedXp(int successRate, int realism, int persuasion, Tone tone) {
        return (int) Math.round(successRate * 0.6 + realism * 8 + persuasion * 8) + tone.getXpBonus();
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

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
