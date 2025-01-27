package com.gamehub.backend.domain;

import jakarta.persistence.Table;


@Table(name = "genres")
public enum Genre {
    ACTION,
    ADVENTURE,
    RPG,
    SIMULATION,
    STRATEGY,
    SPORTS,
    PUZZLE,
    MMO,
    MOBA,
    FPS,
    TPS,
    SURVIVAL,
    HORROR,
    SANDBOX,
    OPEN_WORLD,
    STEALTH,
    FIGHTING,
    RACING,
    PLATFORMER,
    MUSIC,
    PARTY,
    ARCADE,
    VISUAL_NOVEL,
    CARD_GAME,
    BOARD_GAME,
    TRIVIA,
    EDUCATIONAL,
    CASUAL,
    IDLE,
    TOWER_DEFENSE,
    VR,
    AR,
    TEXT_ADVENTURE,
    POINT_AND_CLICK,
    HACK_AND_SLASH,
    BEAT_EM_UP,
    INTERACTIVE_DRAMA,
    MMORPG,
    TURN_BASED_STRATEGY,
    REAL_TIME_STRATEGY,
    AUTO_BATTLER,
    ROGUELIKE,
    CITY_BUILDER,
    MANAGEMENT,
    FLIGHT_SIMULATION,
    SPACE_SIMULATION,
    FARMING_SIMULATION,
    LIFE_SIMULATION,
    DANCING,
    FISHING,
    PUZZLE_PLATFORMER,
    SHOOT_EM_UP
}
