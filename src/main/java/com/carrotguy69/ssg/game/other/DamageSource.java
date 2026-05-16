package com.carrotguy69.ssg.game.other;

import com.carrotguy69.ssg.game.GamePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record DamageSource(GamePlayer attacker, Reason reason) {
    public enum Reason {
        MELEE,
        PROJECTILE,
        EXPLOSIVE,
        NATURAL
    }

    public DamageSource(GamePlayer attacker, @NotNull Reason reason) {
        this.attacker = attacker;
        this.reason = reason;
    }

    public boolean isAttackerSelf(GamePlayer player) {
        return Objects.equals(attacker, player);
    }
}
