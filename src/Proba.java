import mpi.*;

public class Proba {
    public static void main(String[] args) throws Exception {
        // Inicializacija MPI
        MPI.Init(args);

        // Rank = ID procesa, Size = število procesov
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        // Izpis za vsak proces
        System.out.println("Pozdrav od procesa " + rank + " od " + size);

        // Zaključek MPI
        MPI.Finalize();
    }
}
