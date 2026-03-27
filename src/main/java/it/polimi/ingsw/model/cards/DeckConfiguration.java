package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.cards.Characters.*;
import it.polimi.ingsw.model.cards.Events.*;
import it.polimi.ingsw.model.cards.Buildings.*;

import java.util.Collection;
import java.util.List;

public class DeckConfiguration {
    // Personaggi
    public List<Artist> artists;
    public List<Builder> builders;
    public List<Collector> collectors;
    public List<Hunter> hunters;
    public List<Inventor> inventors;
    public List<Shaman> shamans;

    // Eventi
    public List<Hunt> hunts;
    public List<Pictures> pictures;
    public List<Ritual> rituals;
    public List<Sustenance> sustenances;

    // Edifici
    public List<BuildingEachTurn> buildingEachTurns;
    public List<BuildingEnd> buildingEnds;
    public List<BuildingEvent> buildingEvents;


}