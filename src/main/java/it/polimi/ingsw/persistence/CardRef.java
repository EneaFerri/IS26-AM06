package it.polimi.ingsw.persistence;

/** Riferimento minimo a una carta: ID univoco + flag drawed. */
public record CardRef(int cardId, boolean drawed) {}
