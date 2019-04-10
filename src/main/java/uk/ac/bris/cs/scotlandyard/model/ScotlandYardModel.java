package uk.ac.bris.cs.scotlandyard.model;

import java.net.DatagramPacket;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.sun.prism.shader.AlphaOne_Color_Loader;
import javafx.beans.binding.DoubleExpression;
import org.apache.commons.lang3.ObjectUtils;
import uk.ac.bris.cs.gamekit.graph.Graph;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.gamekit.graph.Node;

import javax.swing.text.html.Option;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move>, MoveVisitor {

    private List<Boolean> rounds;
    private Graph<Integer, Transport> graph;
    private List<ScotlandYardPlayer> players = new ArrayList<>();
    private List<Spectator> spectators = new ArrayList<>();
    private Set<Colour> playersWon = new HashSet<>();
    private Colour currentPlayer;
    private int currentPlayerIndex;
    private int round;
    private int mrXlocation; // Store the location of MrX that players are allowed to see

    public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
                             PlayerConfiguration mrX, PlayerConfiguration firstDetective,
                             PlayerConfiguration... restOfTheDetectives) {
        // TODO
        this.rounds = requireNonNull(rounds);
        this.graph = requireNonNull(graph);

        // Check that neither the graph or rounds are empty
        if(rounds.isEmpty()){
            throw new IllegalArgumentException("Empty rounds");
        }
        if(graph.isEmpty()){
            throw new IllegalArgumentException("Empty map");
        }
        // Check that MrX is the correct colour
        if(mrX.colour != BLACK){
            throw new IllegalArgumentException("MrX should be black");
        }

        // Create a list of configurations for ease of checking
        ArrayList<PlayerConfiguration> configurations = new ArrayList<>();
        for (PlayerConfiguration c : restOfTheDetectives){
            configurations.add(requireNonNull(c));
        }
        configurations.add(0, firstDetective);
        configurations.add(0, mrX);

        // Use a set to check there are no duplicate locations
        Set<Integer> set = new HashSet<>();
        for (PlayerConfiguration c : configurations){
            if(set.contains(c.location)){
                throw new IllegalArgumentException("Duplicate location");
            }
            set.add(c.location);
        }

        // Use a set to check there are no duplicate colours
        Set<Colour> colourSet = new HashSet<>();
        for (PlayerConfiguration c : configurations){
            if(colourSet.contains(c.colour)){
                throw new IllegalArgumentException("Duplicate colour");
            }
            colourSet.add(c.colour);
        }

        // Check that the detectives don't have any illegal tickets
        for (PlayerConfiguration c : configurations){
            // Check a config contains all tickets - not passing test???
            if(c.tickets.size() != Ticket.values().length){
                throw new IllegalArgumentException("Missing tickets");
            }

            if(c.colour != BLACK){
                if(c.tickets.get(DOUBLE) != 0 || c.tickets.get(SECRET) != 0){
                    throw new IllegalArgumentException("Detective has wrong tickets");
                }
            }

        }

        // Use configurations to add players to list.
        for(PlayerConfiguration c : configurations){
            ScotlandYardPlayer p = new ScotlandYardPlayer(c.player, c.colour, c.location, c.tickets);
            players.add(p);
        }

        this.currentPlayer = BLACK;
        this.round = 0;
        this.mrXlocation = 0;
        this.currentPlayerIndex = 0;

    }

    @Override
    public void registerSpectator(Spectator spectator) {
        // TODO
        if(isNull(spectator)) throw new NullPointerException("Spectator is null");
        if(spectators.contains(spectator)) throw new IllegalArgumentException("Spectator already exists");
        spectators.add(spectator);
    }

    @Override
    public void unregisterSpectator(Spectator spectator) {
        // TODO
        if(isNull(spectator)) throw new NullPointerException("Spectator is null");
        if(!spectators.contains(spectator)) throw new IllegalArgumentException("Spectator doesnt exist");
        spectators.remove(spectator);
    }

    @Override
    public void startRotate() {
        // TODO

        if(isGameOver()){
            throw new IllegalStateException("Game over before game begun!");
        }
        Optional<ScotlandYardPlayer> playerO = getPlayerFromColour(getCurrentPlayer());
        currentPlayerIndex = 0;
        ScotlandYardPlayer player;
        if(playerO.isPresent()){
            player = playerO.get();
        } else {
            throw new RuntimeException("Current player does not exist");
        }
        Player current = player.player();
        current.makeMove(this, player.location(), validMove(getCurrentPlayer()), this );

    }

    private Optional<ScotlandYardPlayer> getPlayerFromColour(Colour colour){
        for(ScotlandYardPlayer p : players){
            if(p.colour() == colour){
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    private Set<Move> validMove(Colour player){
        Set<Move> s = new TreeSet<>(new Comparator<>() {
            @Override
            public int compare(Move o1, Move o2) {
                if(o1.equals(o2)) return 0;
                else return o1.hashCode() - o2.hashCode();
            }
        });
        ScotlandYardPlayer p = getPlayerFromColour(player).get();
        boolean pass = false;
        Graph<Integer, Transport> g = graph;
        Node<Integer> location = g.getNode(p.location());
        Collection<Edge<Integer, Transport>> edges = g.getEdgesFrom(location);

        for(Edge<Integer, Transport> e : edges){
            if(getPlayerTickets(player, fromTransport(e.data())).get() >= 1){
                if(!collision(e.destination().value())){
                    TicketMove m = new TicketMove(player, fromTransport(e.data()), e.destination().value());
                    s.add(m);
                    if(player.isMrX()){
                        if(getPlayerTickets(player, DOUBLE).get() >= 1 && getCurrentRound() < getRounds().size() - 1){
                            Set<Move> doubles = nextMoves(m, e.destination());
                            s.addAll(doubles);
                        }
                    }
                }
            }
            if(player.isDetective()){
                if(emptyTransportTickets(player)){
                    pass = true;
                }
            } else {
                if(emptyTransportTickets(player) && getPlayerTickets(player, SECRET).get() >= 1 && !collision(e.destination().value())){
                    TicketMove m = new TicketMove(player, SECRET, e.destination().value());
                    s.add(m);
                }
                Set<Move> secrets = secretMove(location);
                s.addAll(secrets);
            }
        }

        if(pass && s.isEmpty()) s.add(new PassMove(player));

        return s;
    }

    private Set<Move> nextMoves(TicketMove first, Node<Integer> location){
        Set<Move> moves = new HashSet<>();
        Collection<Edge<Integer, Transport>> edges = graph.getEdgesFrom(location);
        for(Edge<Integer, Transport> e : edges) {
            if(getCurrentRound() < 23) {
                if(! collision(e.destination().value()) && getPlayerTickets(getCurrentPlayer(), fromTransport(e.data())).get()
                        >= ((first.ticket().equals(fromTransport(e.data()))) ? 2 : 1)) {
                    TicketMove second = new TicketMove(BLACK, fromTransport(e.data()), e.destination().value());
                    DoubleMove d = new DoubleMove(BLACK, first, second);
                    moves.add(d);

                    if(! e.data().equals(Transport.FERRY) && getPlayerTickets(getCurrentPlayer(), SECRET).get() >= 1) {
                        TicketMove secondSecret = new TicketMove(BLACK, SECRET, e.destination().value());
                        DoubleMove ds = new DoubleMove(BLACK, first, secondSecret);
                        moves.add(ds);
                    }
                }
            }

        }

        return moves;
    }

    private Set<Move> secretMove(Node<Integer> location){
        Set<Move> moves = new HashSet<>();
        Collection<Edge<Integer, Transport>> edges = graph.getEdgesFrom(location);
        for(Edge<Integer, Transport> e : edges){

            if(getPlayerTickets(BLACK, SECRET).get() >= 1 && !collision(e.destination().value())){
                TicketMove m = new TicketMove(BLACK, SECRET, e.destination().value());
                if(!moves.contains(m)) moves.add(m);
                if(getPlayerTickets(BLACK, DOUBLE).get() >= 1 && getCurrentRound() < getRounds().size() - 1){
                    Set<Move> doubles = nextMoves(m, e.destination());
                    moves.addAll(doubles);
                }
            }
        }
        return moves;
    }

    private boolean emptyTransportTickets(Colour player){
        if( getPlayerTickets(player, BUS).get() == 0 &&
            getPlayerTickets(player, TAXI).get() == 0 &&
            getPlayerTickets(player, UNDERGROUND).get() == 0)
        {
            return true;
        } else return false;

    }

    private boolean collision(int newLocation){
        for(ScotlandYardPlayer p : players){
            if(p.location() == newLocation && p.isDetective()){
                return true;
            }
        }
        return false;
    }

    public void nextPlayer(){
        if(players.indexOf(getPlayerFromColour(getCurrentPlayer()).get()) == (players.size() - 1)){
            currentPlayer = BLACK;
            currentPlayerIndex = 0;
        } else {
            ScotlandYardPlayer nextPlayer = players.get(currentPlayerIndex + 1);
            currentPlayerIndex++;
            currentPlayer = nextPlayer.colour();
        }

    }

    public void moveMade(Move m){
        for(Spectator s : spectators){
            s.onMoveMade(this, m);
        }
    }

    public void endOfRound(){

    }

    @Override
    public void accept(Move m){
        requireNonNull(m);
        if(!(validMove(currentPlayer).contains(m))) throw new IllegalArgumentException("Move not valid");
        m.visit(this);
        if(isGameOver()){
            for(Spectator s : spectators){
                s.onGameOver(this, getWinningPlayers());
            }
            return;
        }
        if(getCurrentPlayer() == BLACK){
            // Update spectators
            for(Spectator s : spectators){
                s.onRotationComplete(this);
            }
        } else {
            ScotlandYardPlayer nextPlayer = players.get(currentPlayerIndex);
            Player p = nextPlayer.player();
            p.makeMove(this, nextPlayer.location(), validMove(getCurrentPlayer()), this);
        }
    }

    @Override
    public void visit(PassMove p){
        nextPlayer();
        moveMade(p);
    }

    @Override
    public void visit(TicketMove t){
        ScotlandYardPlayer p = getPlayerFromColour(t.colour()).get();
        p.removeTicket(t.ticket());
        if(t.colour() != BLACK){
            ScotlandYardPlayer mrX = getPlayerFromColour(BLACK).get();
            mrX.addTicket(t.ticket());
        }
        // Update player location
        p.location(t.destination());
        nextPlayer();
        if(t.colour() != BLACK){
            moveMade(t);
        } else {
            TicketMove hidden = new TicketMove(t.colour(), t.ticket(), mrXlocation);
            round++;
            for(Spectator s : spectators){
                s.onRoundStarted(this, getCurrentRound());
            }
            if(getCurrentRound() == 0) moveMade(hidden);
            else if(getRounds().get(getCurrentRound() - 1)) moveMade(t);
            else moveMade(hidden);
        }

    }

    @Override
    public void visit(DoubleMove d){
        ScotlandYardPlayer p = getPlayerFromColour(d.colour()).get();
        // Remove tickets from player
        p.removeTicket(DOUBLE);
        nextPlayer();
        if(getRounds().get(getCurrentRound())){
            if(getRounds().get(getCurrentRound() + 1)){
                moveMade(d);
            } else {
                DoubleMove hiddenNext = new DoubleMove(d.colour(), d.firstMove().ticket(), d.firstMove().destination(), d.secondMove().ticket(), d.firstMove().destination());
                moveMade(hiddenNext);
            }
        } else {
            if(getRounds().get(getCurrentRound() + 1)){
                DoubleMove hiddenFirst = new DoubleMove(d.colour(), d.firstMove().ticket(), 0, d.secondMove().ticket(), d.secondMove().destination());
                moveMade(hiddenFirst);
            } else {
                DoubleMove hidden = new DoubleMove(d.colour(), d.firstMove().ticket(), 0, d.secondMove().ticket(), 0);
                moveMade(hidden);
            }
        }
        p.removeTicket(d.firstMove().ticket());
        round++;
        for(Spectator s : spectators){
            s.onRoundStarted(this, getCurrentRound());
        }
        if(getRounds().get(getCurrentRound()) && getRounds().get(getCurrentRound() - 1)){
            moveMade(d.firstMove());
        } else {
            if(getRounds().get(getCurrentRound() - 1)){
                moveMade(d.firstMove());
            } else {
                TicketMove hiddenFirst = new TicketMove(d.colour(), d.firstMove().ticket(), 0);
                moveMade(hiddenFirst);
            }

        }
        p.removeTicket(d.secondMove().ticket());
        round++;
        for(Spectator s : spectators){
            s.onRoundStarted(this, getCurrentRound());
        }
        if(getRounds().get(getCurrentRound())){
            moveMade(d.secondMove());
        } else {
            if(getRounds().get(getCurrentRound() - 2)){
                TicketMove hiddenSecond = new TicketMove(d.colour(), d.secondMove().ticket(), d.firstMove().destination());
                moveMade(hiddenSecond);
            } else {
                TicketMove hiddenSecond = new TicketMove(d.colour(), d.secondMove().ticket(), 0);
                moveMade(hiddenSecond);
            }

        }
        // Update location
        p.location(d.finalDestination());
    }

    @Override
    public Collection<Spectator> getSpectators() {
        // TODO
        return Collections.unmodifiableList(spectators);
    }

    @Override
    public List<Colour> getPlayers() {
        // TODO DONE
        List<Colour> coloursList = new ArrayList<>();
        for(ScotlandYardPlayer p : players){
            coloursList.add(p.colour());
        }
        return Collections.unmodifiableList(coloursList);

    }

    @Override
    public Set<Colour> getWinningPlayers() {
        if(isGameOver()){
            return Collections.unmodifiableSet(playersWon);
        } else {
            return Collections.unmodifiableSet(emptySet());
        }

    }

    @Override
    public Optional<Integer> getPlayerLocation(Colour colour) {
        // TODO
        for(ScotlandYardPlayer p : players){
            if(colour == BLACK){
                if(getCurrentRound() < 1) {
                    return Optional.of(0);
                }
                else if(getRounds().get(getCurrentRound() - 1)){
                        mrXlocation = p.location(); // updates location with current location
                        return Optional.of(mrXlocation); // returns the updated location
                }
                else return Optional.of(mrXlocation); // only returns the location, doesnt update it
            }
            else if(p.colour() == colour){
                return Optional.of(p.location());
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
        // TODO DONE
        for(ScotlandYardPlayer p : players){
            if(p.colour() == colour){
                int t = p.tickets().get(ticket);
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isGameOver() {
        int tickets = 0;
        for(ScotlandYardPlayer p : players){
            if(p.isDetective() && p.location() == getPlayerFromColour(BLACK).get().location()){
                // Detectives captured MrX so have won.
                for(int i = 1; i < players.size(); i++){
                    playersWon.add(players.get(i).colour());
                }
                return true;
            }
            if(p.isDetective()){
                for(Ticket t : Ticket.values()){
                    tickets += p.tickets().get(t);
                }
            }
        }
        if(tickets == 0){
            // MrX Wins
            playersWon.add(BLACK);
            return true;
        }

        if(validMove(BLACK).size() == 0 && currentPlayerIndex == 0){
            // Detectives win as MrX ran out of tickets?
            for(int i = 1; i < players.size(); i++){
                playersWon.add(players.get(i).colour());
            }
            return true;
        }

        if(getCurrentRound() == getRounds().size() && currentPlayerIndex == 0){
            // End of the game and detectives haven't captured MrX yet.
            playersWon.add(BLACK);
            return true;
        }

        return false;
    }

    @Override
    public Colour getCurrentPlayer() {
        // TODO DONE
        return currentPlayer;
    }

    @Override
    public int getCurrentRound() {
        // TODO DONE
        return round;
    }

    @Override
    public List<Boolean> getRounds() {
        // TODO DONE
        return Collections.unmodifiableList(this.rounds);
    }

    @Override
    public Graph<Integer, Transport> getGraph() {
        // TODO DONE
        return new ImmutableGraph<Integer, Transport>(this.graph);
    }

}
