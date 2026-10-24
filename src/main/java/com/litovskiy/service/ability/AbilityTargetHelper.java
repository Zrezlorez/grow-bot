package com.litovskiy.service.ability;

import com.litovskiy.config.properties.AbilityProperties;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.repository.ConversationParticipantRepository;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.PlayerAccountService;
import com.litovskiy.service.data.PlayerService;
import com.litovskiy.service.log.GameLogService;
import com.litovskiy.service.log.PlayerBehaviorStatService;
import com.litovskiy.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import static com.litovskiy.util.StringUtil.formatDuration;

@Component
@RequiredArgsConstructor
public class AbilityTargetHelper {

    private final PlayerAccountService playerAccountService;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final Clock clock;
    private final AbilityProperties abilityProperties;

    public AbilitySelfContext prepareSelfAbility(Platform platform, long profileId) {
        LocalDateTime now = LocalDateTime.now(clock);

        Player player = playerAccountService.resolveOrCreate(platform, profileId);

        Optional<String> cooldownMessage = getCooldownString(player);
        return cooldownMessage.map(AbilitySelfContext::rejected).orElseGet(() -> AbilitySelfContext.success(now, player));

    }

    public AbilityTargetContext prepareTargetAbility(Platform platform, long profileId,
                                                        Long scopeId, long targetProfileId)
    {
        LocalDateTime now = LocalDateTime.now(clock);

        Player actor = playerAccountService.resolveOrCreate(platform, profileId);

        Optional<String> cooldownMessage = getCooldownString(actor);
        Player target = playerAccountService.resolveOrCreate(platform, targetProfileId);

        if (cooldownMessage.isPresent()) {
            return AbilityTargetContext.rejected(now, actor, target, cooldownMessage.get());
        }

        String validationMessage = validateTarget(platform, scopeId, actor, target);
        if (validationMessage != null) {
            return AbilityTargetContext.rejected(validationMessage);
        }

        return AbilityTargetContext.success(now, actor, target);
    }

    public int toPercent(double value) {
        return (int) Math.round(value * 100);
    }

    public String validateTarget(Platform platform, long scopeId, Player actor, Player target) {
        if (actor.getId().equals(target.getId())) {
            return "Нельзя использовать эту способность на себе.";
        }

        if (!conversationParticipantRepository.existsByPlayerIdAndPlatformAndScopeId(target.getId(), platform, scopeId)) {
            return "Цель не найдена в этой беседе.";
        }

        return null;
    }

    public Optional<String> getCooldownString(Player player) {
        return getAbilityCooldown(player).map(it -> "Способность еще на перезарядке. Доступно через: " + formatDuration(it));
    }

    public Optional<Duration> getAbilityCooldown(Player player) {
        return StringUtil.getCooldown(player.getLastAbilityTime(), abilityProperties.getAbilityCooldown(), clock);
    }
}
