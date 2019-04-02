package uk.ac.bris.cs.scotlandyard.model;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
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
        // makes sure rounds and graph (the map) is set to not null
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

        // Use a set to check there are no duplicate locations for starting players
        Set<Integer> locationSet = new HashSet<>();
        for (PlayerConfiguration c : configurations){
            if(locationSet.contains(c.location)){
                throw new IllegalArgumentException("Duplicate location");
            }
            locationSet.add(c.location);
        }

        // Use a set to check there are no duplicate colours for starting players
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
//        Optional<ScotlandYardPlayer> playerO = getPlayerFromColour(getCurrentPlayer());
//        ScotlandYardPlayer player;
//        if(playerO.isPresent()){
//            player = playerO.get();
//        } else {
//            throw new RuntimeException("Current player does not exist");
//        }
//        Player current = player.player();
//        current.makeMove(this, getPlayerLocation(getCurrentPlayer()).get(), validMove(getCurrentPlayer()), this );

        Optional<ScotlandYardPlayer> player0 = getPlayerFromColour(getCurrentPlayer());
        ScotlandYardPlayer player;
        if(player0.isPresent()) player = player0.get();
        else throw new RuntimeException("current player is null");

    }

    private Optional<ScotlandYardPlayer> getPlayerFromColour(Colour colour){
//        for(ScotlandYardPlayer p : players){
//            if(p.colour() == colour){
//                return Optional.of(p);
//            }
//        }
//        return Optional.empty();

    }

    private Set<Move> validMove(Colour player){
        Set<Move> s = new HashSet<>();
        Node<Integer> location = graph.getNode(getPlayerLocation(player).get());
        Collection<Edge<Integer, Transport>> edges = graph.getEdgesFrom(location);
        for(Edge<Integer, Transport> e : edges){
            Ticket p = Ticket.fromTransport(e.data());
            int destination = e.destination().value();
            for(Transport t : Transport.values()){
                if(e.data() == t){
                    if(player != BLACK){
                        // Logic for Detectives
                        if(getPlayerTickets(player, fromTransport(t)).get() >= 1){
                            TicketMove m = new TicketMove(player, fromTransport(t), e.destination().value());
                            s.add(m);
                        } else {
                            PassMove m = new PassMove(player);
                            s.add(m);
                        }
                    } else {
                        // Logic for MrX - Doubles & Secrets need to be handled

                    }

                }
            }
        }
        return s;
    }

    @Override
    public void accept(Move m){
        if(isNull(m)) throw new NullPointerException("Move was null");
        if(!validMove(m.colour()).contains(m)) throw new IllegalArgumentException("Move not valid");
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
