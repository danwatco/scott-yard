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

import uk.ac.bris.cs.gamekit.graph.Graph;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.DOUBLE;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.SECRET;
import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {

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

		if(rounds.isEmpty()){
			throw new IllegalArgumentException("Empty rounds");
		}
		if(graph.isEmpty()){
			throw new IllegalArgumentException("Empty map");
		}
		if(mrX.colour != BLACK){
			throw new IllegalArgumentException("MrX should be black");
		}

		ArrayList<PlayerConfiguration> configurations = new ArrayList<>();
		for (PlayerConfiguration c : restOfTheDetectives){
			configurations.add(requireNonNull(c));
		}
		configurations.add(0, firstDetective);
		configurations.add(0, mrX);

		Set<Integer> set = new HashSet<>();
		for (PlayerConfiguration c : configurations){
			if(set.contains(c.location)){
				throw new IllegalArgumentException("Duplicate location");
			}
			set.add(c.location);
		}

		Set<Colour> colourSet = new HashSet<>();
		for (PlayerConfiguration c : configurations){
			if(colourSet.contains(c.colour)){
				throw new IllegalArgumentException("Duplicate colour");
			}
			colourSet.add(c.colour);
		}

		for (PlayerConfiguration c : configurations){
			if(c.colour != BLACK){
				if(c.tickets.get(DOUBLE) != 0 || c.tickets.get(SECRET) != 0){
					throw new IllegalArgumentException("Detective has wrong tickets");
				}
			}
			if(c.tickets.size() != Ticket.values().length){
				throw new IllegalArgumentException("Missing tickets");
			}
		}

		for(PlayerConfiguration c : configurations){
			ScotlandYardPlayer p = new ScotlandYardPlayer(c.player, c.colour, c.location, c.tickets);
			players.add(p);
		}

		this.currentPlayer = BLACK;
		this.round = 1;

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
		throw new RuntimeException("Implement me");
	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Colour> getPlayers() {
		// TODO
		List<Colour> coloursList = new ArrayList<>();
		for(ScotlandYardPlayer p : players){
			coloursList.add(p.colour());
		}
		return Collections.unmodifiableList(coloursList);

	}

	@Override
	public Set<Colour> getWinningPlayers() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public boolean isGameOver() {
		// TODO
		return false;
	}

	@Override
	public Colour getCurrentPlayer() {
		// TODO
		return currentPlayer;
	}

	@Override
	public int getCurrentRound() {
		// TODO
		return round;
	}

	@Override
	public List<Boolean> getRounds() {
		// TODO
		return Collections.unmodifiableList(this.rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		// TODO
		return new ImmutableGraph<Integer, Transport>(this.graph);
	}

}
