
package amazed.solver;

import amazed.maze.Maze;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <code>ForkJoinSolver</code> implements a solver for
 * <code>Maze</code> objects using a fork/join multi-thread
 * depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */


public class ForkJoinSolver extends SequentialSolver {

    //Synchronized  Set for our parallel solution.
    private static ConcurrentSkipListSet<Integer> visited = new ConcurrentSkipListSet<>(); // Initialize visited for each instance;
    private static AtomicBoolean foundGoal= new AtomicBoolean(false);
    List<ForkJoinSolver> solvers;
    int current;

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */
    public ForkJoinSolver(Maze maze)
    {
        super(maze);
        solvers = new ArrayList<>();
        current = start;
    }

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal, forking after a given number of visited
     * nodes.
     *
     * @param maze        the maze to be searched
     * @param forkAfter   the number of steps (visited nodes) after
     *                    which a parallel task is forked; if
     *                    <code>forkAfter &lt;= 0</code> the solver never
     *                    forks new tasks
     */

    public ForkJoinSolver(Maze maze, int forkAfter)
    {
        this(maze);
        this.forkAfter = forkAfter;
    }



    // Constructor for the sub-threads of root,
    public ForkJoinSolver(Maze maze,int start, int bajs){
        this(maze);
        this.current = start;
        bajs = bajs;
        //this.predecessor = predecessor;
    }

    /**
     * Searches for and returns the path, as a list of node
     * identifiers, that goes from the start node to a goal node in
     * the maze. If such a path cannot be found (because there are no
     * goals, or all goals are unreacheable), the method returns
     * <code>null</code>.
     *
     * @return   the list of node identifiers from the start node to a
     *           goal node in the maze; <code>null</code> if such a path cannot
     *           be found.
     */
    @Override
    public List<Integer> compute()
    {
        return parallelSearch();
    }


    private List<Integer> parallelSearch() {
        // the path to be returned.
        List<Integer> path = null;
        int player = maze.newPlayer(current);
        frontier.push(current);

        while (!frontier.isEmpty() && !foundGoal.get()) {
            int current = frontier.pop();

            if (maze.hasGoal(current)) {
                foundGoal.set(true);
                maze.move(player, current);
                return pathFromTo(this.current, current);
            }

            if(visited.add(current)){
                maze.move(player, current);
            }

            if(maze.neighbors(current).size() > 2){
                for (Integer nb : maze.neighbors(current)) {
                    if (visited.add(nb)) {
                        predecessor.put(nb, current);
                        ForkJoinSolver solver = new ForkJoinSolver(maze, nb, 1);
                        solvers.add(solver);
                        solver.fork();
                    }
                }
                // Join all solvers after collecting them
                for (ForkJoinSolver solver : solvers) {
                    List<Integer> solverPath = solver.join();
                    if (solverPath != null) {
                        path = pathFromTo(this.current, solver.current);
                        path.addAll(solverPath);
                    }
                }
            }else{
                //for each of the neighbors
                for(int nb: maze.neighbors(current)){
                    //if not visited
                    if(!visited.contains(nb)){
                        //put current as predecessor
                        predecessor.put(nb, current);
                        //push to frontier
                        frontier.push(nb);
                    }
                }
            }
        }
        return path;
    }
}