package com.asdf.tongchoobe.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Tone {
    MILD(0),
    SLICK(10),
    DESPERATE(20),
    BULLSHIT(30);

    private final int xpBonus;
}
