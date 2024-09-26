package amazed.solver;

import amazed.maze.Maze;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;

/**
 * <code>ForkJoinSolver</code> implements a solver for
 * <code>Maze</code> objects using a fork/join multi-thread
 * depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */


public class ForkJoinSolver
    extends SequentialSolver
{

    //Synchronized HashMap and Set for our parallel solution.
    protected ConcurrentHashMap<Integer, Integer> predessor = new ConcurrentHashMap<>();
    protected ConcurrentSkipListSet<Integer> visited = new ConcurrentSkipListSet<>();

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */
    public ForkJoinSolver(Maze maze)
    {
        super(maze);
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
    public ForkJoinSolver(Maze maze, ForkJoinSolver root, int start){

        this(maze);
        this.start = start;
        this.forkAfter = root.forkAfter;
        this.visited = root.visited;
        this.predessor = root.predessor;
        //this.frontier = new Stack<>();
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


    private List<Integer> parallelSearch()
    {
        // make sure new starting node wasn't already visited.
        if(!visited.contains(start)){

            int player = maze.newPlayer(start);
            frontier.push(start);

            while(!frontier.isEmpty()){

                int current = frontier.pop();

                // checks if current node is a goal.
                if(maze.hasGoal(current)){

                    maze.move(player, current);
                    return pathFromTo(start, current);
                }

                // if we haven't visited current node we move the player there and add it to visited set.
                if(!visited.contains(current)){
                    maze.move(player, current);
                    visited.add(current);

                    for (int nb : maze.neighbors(current)){

                        if(!visited.contains(nb)){
                            predessor.put(nb, current);
                            frontier.push(nb);
                        }
                    }

                    //fork new threads and wait for each task to finish.
                    ArrayList<ForkJoinSolver> tasks = spawn();
                    for(ForkJoinSolver task : tasks){
                        task.join();
                    }

                }
            }
        }
        // all nodes explored, no goal found
        return null;
    }

    public ArrayList<ForkJoinSolver> spawn() {
        ArrayList<ForkJoinSolver> tasks = new ArrayList<>();

        Stack<Integer> frontierCopy = new Stack<>();
        frontierCopy.addAll(frontier);  // Make a copy of the frontier

        // For each neighbor in the frontier, fork a new thread
        while (!frontierCopy.isEmpty()) {
            int nextNode = frontierCopy.pop();
            ForkJoinSolver task = new ForkJoinSolver(maze, this, nextNode);
            task.fork();  // Fork new tasks
            tasks.add(task);
        }

        return tasks;
    }

}
