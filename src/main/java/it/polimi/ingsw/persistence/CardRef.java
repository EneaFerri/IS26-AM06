package it.polimi.ingsw.persistence;

/** Minimal card reference: unique card ID and whether the card has already been drawn. */
public record CardRef(int cardId, boolean drawed) {}
