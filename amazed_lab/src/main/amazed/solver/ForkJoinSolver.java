package amazed.solver;

import amazed.maze.Maze;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
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

    //Synchronized HashMap and Set for our parallel solution.
    protected ConcurrentHashMap<Integer, Integer> predessor = new ConcurrentHashMap<>();
    protected ConcurrentSkipListSet<Integer> visited = new ConcurrentSkipListSet<>();
    protected AtomicBoolean foundGoal = new AtomicBoolean(false);
    protected ArrayList<ForkJoinSolver> solvers;

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */
    public ForkJoinSolver(Maze maze)
    {
        super(maze);
        solvers = new ArrayList<ForkJoinSolver>();
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
    public ForkJoinSolver(Maze maze, ForkJoinSolver root, ConcurrentSkipListSet<Integer> visited, int start){

        this(maze);
        this.start = start;
        this.visited = visited;
        this.predessor = root.predessor;
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

        List<Integer> result = new ArrayList<>();

        // make sure new starting node wasn't already visited.
        if(!visited.contains(start)){

            int player = maze.newPlayer(start);
            frontier.push(start);


            while(!frontier.isEmpty()){

                // stop searching and break the loop if goal is already found.
                if(foundGoal.get()){
                    break;
                }

                // pop the current node to be visited.
                int current = frontier.pop();

                // checks if current node is a goal.
                if(maze.hasGoal(current)){

                    foundGoal.set(true);
                    maze.move(player, current);
                    result = pathFromTo(start, current);
                    break;
                }

                if(maze.neighbors(current).size() > 2){
                    for(Integer nb : maze.neighbors(current)){
                        solvers.add(new ForkJoinSolver(maze, this, visited, nb));
                    }
                    for(ForkJoinSolver solver : solvers){
                        solver.fork();
                    }
                }
            }
        }
        return createPath(result);
    }
    public List<Integer> createPath(List<Integer> path){
        List<Integer> subpath = new ArrayList<>();
        for(ForkJoinSolver solver : solvers){

            subpath.addAll(solver.join());

        }
        return List.of();
    }

}

