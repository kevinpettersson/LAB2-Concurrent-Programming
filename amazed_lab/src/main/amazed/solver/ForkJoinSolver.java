
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

    //Synchronized shared(static) variables Set and Boolean for our parallel solution.
    private static ConcurrentSkipListSet<Integer> visited = new ConcurrentSkipListSet<>();
    private static AtomicBoolean foundGoal= new AtomicBoolean(false);
    private int currentStart;

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */
    public ForkJoinSolver(Maze maze)
    {
        super(maze);
        //set currentStart = Sequential.start so first thread know where to start.
        currentStart = start;
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



    // Constructor for the sub-threads of root, forkAfter is not used in this solution.
    public ForkJoinSolver(Maze maze, int start, int forkAfter){
        this(maze);
        //set current start to new start node.
        this.currentStart = start;
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
        int player = maze.newPlayer(currentStart);
        frontier.push(currentStart);

        //while stack is not empty and goal hasn't been found yet
        while (!frontier.isEmpty() && !foundGoal.get()) {
            //pop the first node in frontier and call it current
            int current = frontier.pop();

            //if current node is the goal
            if (maze.hasGoal(current)) {
                //set boolean to true, indicating all other threads to stop searching.
                foundGoal.set(true);
                //move player to that node
                maze.move(player, current);
                //the thread that finds the goal return the path from where it started to the goal node.
                return pathFromTo(this.currentStart, current);
            }

            //if not visited => add to visited and move player
            if(visited.add(current)){
                maze.move(player, current);
            }

            //if more than 2 neighbors
            if(maze.neighbors(current).size() > 2){
                //create list to keep track of newly created threads
                List<ForkJoinSolver> solvers = new ArrayList<>();
                //for each neighbor
                for (Integer nb : maze.neighbors(current)) {
                    //if not visited => add to visited
                    if (visited.add(nb)) {
                        //put current as predecessor to neighbor
                        predecessor.put(nb, current);
                        //create new thread, fork it and add to list of solvers
                        ForkJoinSolver solver = new ForkJoinSolver(maze, nb, 1);
                        solvers.add(solver);
                        solver.fork();
                    }
                }
                //for each solver
                for (ForkJoinSolver solver : solvers) {
                    //wait for each solver to return their search result and store it into a list(if any).
                    List<Integer> solverPath = solver.join();
                    //check if the solver found a path to the goal(null mean no path found).
                    if (solverPath != null) {
                        //create the path from the current start node to the solvers start node.
                        path = pathFromTo(currentStart, predecessor.get(solver.currentStart));
                        //append the path found by the solver to the newly created path.
                        path.addAll(solverPath);
                    }
                }
                //else if there only was one way to go
            }else{
                //for each neighbor
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
        //return path, its either null or contains the path.
        return path;
    }
}