package it.polimi.ingsw.model.player;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.CharacterCard;

import java.util.List;

public class Player {
    String nickname;
    Totem myTotem;

    int food;
    int prestige;

    Boolean inTurn = false;

    List<CharacterCard> myCharacterCards;
    List<BuildingCard> myBuildingCards;

    int nCollectors;
    int nArtists;
    int nHunters;
    int nInventors;
    int nShamans;
    int nBuilders;

    int totalStars;
    int totalBuildDiscount;

}
