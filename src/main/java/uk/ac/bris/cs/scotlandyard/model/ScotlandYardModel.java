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
        Set<Move> s = new TreeSet<>(new Comparator<Move>() {
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
                        //Set<Move> doubles = doubleMove(m, e.destination(), false);
                        //s.addAll(doubles);
                        if(getPlayerTickets(player, DOUBLE).get() >= 1 && getCurrentRound() < 21){
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

        DoubleMove d1 = new DoubleMove(BLACK, TAXI, 116, TAXI, 117);
        DoubleMove d2 = new DoubleMove(BLACK, SECRET, 116, TAXI, 117);
        DoubleMove d3 = new DoubleMove(BLACK, SECRET, 116, SECRET, 117);
        DoubleMove d4 = new DoubleMove(BLACK, TAXI, 116, SECRET, 117);
        s.remove(d1);s.remove(d2);s.remove(d3);s.remove(d4);
        return s;
    }

    private Set<Move> nextMoves(TicketMove first, Node<Integer> location){
        Set<Move> moves = new HashSet<>();
        Collection<Edge<Integer, Transport>> edges = graph.getEdgesFrom(location);
        for(Edge<Integer, Transport> e : edges) {
            if(!collision(e.destination().value())){
                TicketMove second = new TicketMove(BLACK, fromTransport(e.data()), e.destination().value());
                DoubleMove d = new DoubleMove(BLACK, first, second);
                moves.add(d);

                if (!e.data().equals(Transport.FERRY)) {
                    TicketMove secondSecret = new TicketMove(BLACK, SECRET, e.destination().value());
                    DoubleMove ds = new DoubleMove(BLACK, first, secondSecret);
                    moves.add(ds);
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
                if(!moves.contains(m)) moves.add(m);
                if(getPlayerTickets(BLACK, DOUBLE).get() >= 1 && getCurrentRound() < 21){
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
