package org.overload.algorithms.pathfinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.overload.algorithms.pathfinding.Pathfinder.Flags;
import org.overload.algorithms.pathfinding.Pathfinder.Heuristic;
import org.overload.impl.DoubleComparator;
import org.overload.loc.Locatable;
import org.overload.loc.Node;

/**
 * Jump point search algorithm first described by </br>
 * Harabor and Grastien in 2011.<br>
 * Built upon the A* algorithm, @see AStarImpl, with jump point method.<br>
 * This algorithm only works eight way.
 * @author Odell
 */
class JPSImpl implements AlgorithmDefinition {
	
	private final PriorityQueue<JPNode> open;
	private final HashSet<JPNode> closed;
	private final HashMap<JPNode, JPNode> parentMap;
	private JPNode curr = null;
	private Node dest = null;
	
	private Heuristic heur;
	private Flags flags;
	private double diagonal;
	
	JPSImpl() {
		open = new PriorityQueue<JPNode>(10, new DoubleComparator<JPNode>() {
			public double compareD(JPNode n1, JPNode n2) {
				return n1.getF() - n2.getF();
			}
		});
		closed = new HashSet<JPNode>();
		parentMap = new HashMap<JPNode, JPNode>();
	}
	
	@Override
	public void setEight(boolean eight) {}

	@Override
	public void setFlags(Flags flags) {
		this.flags = flags;
	}

	@Override
	public void setHeuristic(Heuristic heur) {
		this.heur = heur;
	}

	@Override
	public void setDiagonal(double diagonal) {
		this.diagonal = diagonal;
	}

	@Override
	public List<Locatable> findPath(final Locatable start, final Locatable end) {
		if (heur == null)
			this.heur = Heuristic.MANHATTAN; // recommended
		try {
			dest = new Node(end);
			if (walkable(dest, null)) { // ensure the destination isn't blocked already
				curr = new JPNode(start);
				do {
					if (curr.equals(dest))
						return resolve(curr);
					closed.add(curr);
					//System.out.println("Open: " + curr);
					for (final JPNode neighbor : curr.getNeighbors(true)) {
						//System.out.println("Neighbor: " + neighbor);
						JPNode jumpPoint = jump(neighbor, curr);
						if (jumpPoint != null) {
							//System.out.println("Jump point: " + jumpPoint);
							if (closed.contains(jumpPoint))
								continue;
							//System.out.println("Added jump point.");
							if (!open.contains(jumpPoint)) {
								jumpPoint.setParent(curr);
								open.offer(jumpPoint);
							} else if ((curr.getG() + curr.getMoveCost(jumpPoint)) < jumpPoint.getG()) { // G score of node with current node as it's parent
								final JPNode instanceNode = retrieveInstance(open, jumpPoint);
								if (instanceNode != null)
									instanceNode.setParent(curr);
							}
						}
					}
					//System.out.println("Close: " + curr);
				} while ((curr = open.poll()) != null);
			}
			return null;
		} finally {
			open.clear();
			closed.clear();
			parentMap.clear();
			curr = null;
			dest = null;
			
			flags = null;
			heur = null;
		}
	}
	
	/**
	 * Finds the next jump node.
	 * @param x current node X
	 * @param y current node Y
	 * @param px parent node X
	 * @param py parent node Y
	 * @return next jump node
	 */
	private JPNode jump(Node node, Node parent) {
		int x = node.getX(), y = node.getY(), px = parent.getX(), py = parent.getY();
		int dx = x - px, dy = y - py;
		
		if (!walkable(node, parent)) // check blocked
			return null;
		if (node.equals(dest)) // reached goal
			return new JPNode(node);
		
		// resolve forced neighbors
		Node temp = new Node(node);
		if ((dx & dy) != 0) { // diagonal
			if ((walkable(temp.set(x - dx, y + dy), parent) && !walkable(temp.set(x - dx, y), parent)) ||
				(walkable(temp.set(x + dx, y - dy), parent) && !walkable(temp.set(x, y - dy), parent))) {
				return new JPNode(node);
			}
			// recurse
			JPNode h = jump(node.derive(dx, 0), node);
			if (h != null)
				return new JPNode(node);
			JPNode v = jump(node.derive(0, dy), node);
			if (v != null)
				return new JPNode(node);
		} else if (dx == 0) { // vertical, dx = 0, dy = 1 or -1
			if ((walkable(temp.set(x + 1, y + dy), parent) && !walkable(temp.set(x + 1, y), parent)) ||
				(walkable(temp.set(x - 1, y + dy), parent) && !walkable(temp.set(x - 1, y), parent))) {
				return new JPNode(node);
			}
		} else { // horizontal, dx = 1 or -1, dy = 0
			if ((walkable(temp.set(x + dx, y + 1), parent) && !walkable(temp.set(x, y + 1), parent)) ||
				(walkable(temp.set(x + dx, y - 1), parent) && !walkable(temp.set(x, y - 1), parent))) {
				return new JPNode(node);
			}
		}
		
		// recurse
		return jump(node.derive(dx, dy), node);
	}
	
	private List<Locatable> resolve(JPNode target) {
		if (target == null)
			return null;
		final LinkedList<Locatable> path = new LinkedList<Locatable>();
		path.addFirst(new Node(target));
		JPNode parent;
		while ((parent = target.getParent()) != null) {
			// FROM TARGET TO PARENT, NOT INCLUDING TARGET
			int x = parent.getX(), y = parent.getY(), px = target.getX(), py = target.getY();
			int steps = getSteps(x, y, px, py);
			Node norm = normalizeDirection(x, y, px, py);
			int dx = norm.getX(), dy = norm.getY();
			for (int i = 0; i < steps; i++) {
				target.shift(dx, dy);
				path.addFirst(new Node(target));
			}
			target = parent;
		}
		return new ArrayList<Locatable>(path);
	}
	
	private int getSteps(int x, int y, int px, int py) {
		int temp;
		if ((temp = x - px) != 0) { // straight, horizontal AND will handle diagonal
		} else if ((temp = y - py) != 0) { // straight, vertical
		} else return 0;
		return Math.abs(temp);
	}
	
	private Node normalizeDirection(int x, int y, int px, int py) {
		int dx = x - px, dy = y - py;
		dx /= Math.max(Math.abs(dx), 1);
		dy /= Math.max(Math.abs(dy), 1);
		return new Node(dx, dy);
	}
	
	private boolean walkable(Locatable loc, Locatable parent) {
		return flags == null || !flags.blocked(loc, parent);
	}
	
	private JPNode retrieveInstance(final PriorityQueue<JPNode> pq, final JPNode node) {
		if (node == null)
			return null;
		final Iterator<JPNode> nI = pq.iterator();
		while (nI.hasNext()) {
			final JPNode n = nI.next();
			if (node.equals(n))
				return n;
		}
		return null;
	}
	
	/**
	 * Jump point node
	 * @author Odell
	 */
	private class JPNode extends PNode {
		
		/**
		 * the estimated (heuristic) cost to reach the destination from here.
		 */
		private java.lang.Double h;
		/**
		 * the exact cost to reach this node from the starting node.
		 */
		private java.lang.Double g;
		/**
		 * As the algorithm runs the F value of a node tells us how expensive we think it will be to reach our goal by way of that node.
		 */
		private java.lang.Double f;
		
		public JPNode(final int x, final int y) {
			super(x, y);
			h = g = f = null;
		}
		
		public JPNode(final Locatable n) {
			this(n.getX(), n.getY());
		}

		@Override
		public JPNode getParent() {
			return parentMap.get(this);
		}
		
		/**
		 * @return the previous parent node.
		 */
		protected JPNode setParent(final JPNode node) {
			g = null;
			return parentMap.put(this, node);
		}

		protected double getF() {
			if (g == null || h == null || f == null) {
				f = getG() + getH();
			}
			return f;
		}
		
		protected double getG() {
			if (g == null) {
				final JPNode parent = getParent();
				g = parent != null ? parent.getG() + parent.getMoveCost(this) : 0.0D;
			}
			return g;
		}
		
		private double getMoveCost(final JPNode node) {
			if (node == null)
				return 0;
			int x = node.getX(), y = node.getY(), px = getX(), py = getY();
			int steps = getSteps(x, y, px, py);
			return x == px || y == px ? steps : (double) steps * diagonal;
		}
		
		protected double getH() {
			if (h == null) {
				h = heur.distance(this, dest, diagonal);
			}
			return h;
		}
		
		@Override
		protected JPNode[] getNeighbors(boolean eight) {
			LinkedList<JPNode> nodes = new LinkedList<JPNode>();
			JPNode parent = getParent();
			if (parent != null) { // determines whether to prune neighbors
				// normalize
				Node norm = normalizeDirection(getX(), getY(), parent.getX(), parent.getY());
				int dx = norm.getX(), dy = norm.getY();
				
				Node temp = new Node(this);
				if ((dx & dy) != 0) { // diagonal direction
					// check straight directions in the direction of the diagonal move
					if (walkable(temp.set(x, y + dy), this))
						nodes.add(new JPNode(temp));
					if (walkable(temp.set(x + dx, y), this))
						nodes.add(new JPNode(temp));
					if (walkable(temp.set(x + dx, y + dy), this))
						nodes.add(new JPNode(temp));
					// forced neighbor checks
					if (!walkable(temp.set(x - dx, y), this))
						nodes.add(new JPNode(temp.shift(0, dy)));
					if (!walkable(temp.set(x, y - dy), this))
						nodes.add(new JPNode(temp.shift(dx, 0)));
				} else { // straight direction
					if (dx == 0) { // moving vertically
						if (walkable(temp.set(x, y + dy), this)) {
							nodes.add(new JPNode(temp));
							// forced neighbor checks
							if (!walkable(temp.set(x + 1, y), this))
								nodes.add(new JPNode(temp.shift(0, dy)));
							if (!walkable(temp.set(x - 1, y), this))
								nodes.add(new JPNode(temp.shift(0, dy)));
						}
					} else { // moving horizontally
						if (walkable(temp.set(x + dx, y), this)) {
							nodes.add(new JPNode(temp));
							// forced neighbor checks
							if (!walkable(temp.set(x, y + 1), this))
								nodes.add(new JPNode(temp.shift(dx, 0)));
							if (!walkable(temp.set(x, y - 1), this))
								nodes.add(new JPNode(temp.shift(dx, 0)));
						}
					}
				}
			} else {
				// no parent, return all that aren't blocked
				JPNode[] ns = new JPNode[] { new JPNode(x, y - 1), new JPNode(x + 1, y - 1), new JPNode(x + 1, y), new JPNode(x + 1, y + 1), 
						new JPNode(x, y + 1), new JPNode(x - 1, y + 1), new JPNode(x - 1, y), new JPNode(x - 1, y - 1) };
				for (int i = 0; i < ns.length; i++) {
					if (walkable(ns[i], this))
						nodes.add(ns[i]);
				}
			}
			return nodes.toArray(new JPNode[nodes.size()]);
		}
		
	}
	
}