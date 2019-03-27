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
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move> {

    private List<Boolean> rounds;
    private Graph<Integer, Transport> graph;
    private List<ScotlandYardPlayer> players = new ArrayList<>();
    private Colour currentPlayer;
    private int round;

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

    }

    @Override
    public void registerSpectator(Spectator spectator) {
        // TODO
        throw new RuntimeException("Implement me");
    }

    @Override
    public void unregisterSpectator(Spectator spectator) {
        // TODO
        throw new RuntimeException("Implement me");
    }

    @Override
    public void startRotate() {
        // TODO
        Optional<ScotlandYardPlayer> playerO = getPlayerFromColour(getCurrentPlayer());
        ScotlandYardPlayer player;
        if(playerO.isPresent()){
            player = playerO.get();
        } else {
            throw new RuntimeException("Current player does not exist");
        }
        Player current = player.player();
        if(player.isMrX()) System.out.println("Size of set " + validMove(getCurrentPlayer()).size());
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
        Set<Move> s = new HashSet<>();
        ScotlandYardPlayer p = getPlayerFromColour(player).get();
        Boolean pass = false;
        Graph<Integer, Transport> g = graph;
        Node<Integer> location = g.getNode(p.location());
        Collection<Edge<Integer, Transport>> edges = g.getEdgesFrom(location);
        // Loop through each possible edge from the current player location on the map
        for(Edge<Integer, Transport> e : edges){
            // Loop through each transport and compare to the edge
            for(Transport t : Transport.values()){
                if(e.data() == t){
                    // Logic for All players
                    // Check if the player has enough tickets to use the current transport
                    if(getPlayerTickets(player, fromTransport(t)).get() >= 1){
                        TicketMove m = new TicketMove(player, fromTransport(t), e.destination().value());
                        s.add(m);
                        if(player.isMrX()){
                            // Add secret option
                            Set<Move> secrets = secretMove(location);
                            s.addAll(secrets);
                            // Handle double tickets if MrX is playing
                            Set<Move> doubles = doubleMove(m, e.destination(), false);
                            s.addAll(doubles);
                        }
                    } else {
                        if(t == Transport.BUS || t == Transport.TAXI || t == Transport.UNDERGROUND){
                            // If no tickets left add a pass move to the set.
                            pass = true;
                        }
                        if(player.isMrX()){
                            if(getPlayerTickets(player, SECRET).get() >= 1){
                                TicketMove m = new TicketMove(player, SECRET, e.destination().value());
                                s.add(m);
                            }
                        }
                    }

                }
            }
        }
        if(pass && s.isEmpty()) s.add(new PassMove(player));
        return s;
    }

    private Set<Move> doubleMove(TicketMove first, Node<Integer> location, boolean secret){
        Set<Move> moves = new HashSet<>();
        Collection<Edge<Integer, Transport>> edges = graph.getEdgesFrom(location);
        for(Edge<Integer, Transport> e : edges){
            for(Transport t : Transport.values()){
                if(e.data() == t){
                    if(getPlayerTickets(BLACK, fromTransport(t)).get() >= 1){
                        if(secret){
                            TicketMove second = new TicketMove(BLACK, SECRET, e.destination().value());
                            DoubleMove d = new DoubleMove(BLACK, first, second);
                            moves.add(d);
                        } else {
                            TicketMove second = new TicketMove(BLACK, fromTransport(t), e.destination().value());
                            DoubleMove d = new DoubleMove(BLACK, first, second);
                            moves.add(d);
                        }

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
            if(getPlayerTickets(BLACK, SECRET).get() >= 1){
                TicketMove m = new TicketMove(BLACK, SECRET, e.destination().value());
                moves.add(m);
                Set<Move> doubles = doubleMove(m, e.destination(), true);
                moves.addAll(doubles);
            }
        }
        return moves;
    }


    @Override
    public void accept(Move m){
        if(isNull(m)) throw new NullPointerException("Move was null");
        PassMove test = new PassMove(getCurrentPlayer());
        //if(!m.equals(test)) throw new IllegalArgumentException("Move not valid");
        if(validMove(getCurrentPlayer()).contains(m)) {} else { throw new IllegalArgumentException("Move not valid"); };
    }

    @Override
    public Collection<Spectator> getSpectators() {
        // TODO
        throw new RuntimeException("Implement me");
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
        // TODO
        return Collections.unmodifiableSet(emptySet());
    }

    @Override
    public Optional<Integer> getPlayerLocation(Colour colour) {
        // TODO
		// Need to implement the hiding of MrX at certain intervals?
        for(ScotlandYardPlayer p : players){
            if(colour == BLACK){
                return Optional.of(0);
            } else if(p.colour() == colour){
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
        // TODO
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
