import mpi.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class Main {

    public static void main(String[] args){
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        long zacetek = 0;
        long konec = 0;

        int n = 3; // nastavi dolžino n-gramov (po potrebi spremeni tukaj)

        if (rank == 0) {
            zacetek = System.currentTimeMillis();

            // MASTER: prebere datoteko in razdeli povedi
            // Premakni se eno mapo višje iz src v projekt root in nato v resources

            Path projectRoot = Paths.get(System.getProperty("user.dir")).getParent();
            String filePath = Paths.get(projectRoot.toString(), "resources", "123MB.txt").toString();
            String text = preberiIzTxt(filePath);

            // Preveri, če je datoteka prazna
            if (text.isEmpty()) {
                System.err.println("Datoteka je prazna ali ni bila uspešno prebrana!");
                MPI.Finalize();
                return;
            }

            text = odstraniZnakce(text);
            String[] povedi = text.split("[.!?]");
            System.out.println("Število povedi: " + povedi.length);

            // pošlji dolžino n-gramov workerjem
            for (int i = 1; i < size; i++) {
                MPI.COMM_WORLD.Send(new int[]{n}, 0, 1, MPI.INT, i, 99);
            }

            // razdeli povedi na workerje
            int chunkSize = (int) Math.ceil((double) povedi.length / (size - 1));
            int start = 0;
            for (int i = 1; i < size; i++) {
                int end = Math.min(start + chunkSize, povedi.length);
                String[] chunk = Arrays.copyOfRange(povedi, start, end);
                MPI.COMM_WORLD.Send(new Object[]{chunk}, 0, 1, MPI.OBJECT, i, 0);
                start = end;
            }

            // zberi rezultate
            Map<String, Integer> allNgrams = new HashMap<>();
            for (int i = 1; i < size; i++) {
                Object[] recvObj = new Object[1];
                MPI.COMM_WORLD.Recv(recvObj, 0, 1, MPI.OBJECT, i, 1);
                Map<String, Integer> localNgrams = (Map<String, Integer>) recvObj[0];
                zdruziMape(allNgrams, localNgrams);
            }

            // izračun relativnih frekvenc
            Map<String, Double> relFrekvence = izracunajRelativneFrekvence(allNgrams);
            //izpisiVse(allNgrams, relFrekvence);

            konec = System.currentTimeMillis();
            System.out.println("\u001B[32m✔ ⏱ Distribuirana izvedba je trajala: " + (konec - zacetek) + " ms\u001B[0m");

        } else {
            // WORKER: prejme dolžino n-gramov
            int[] nVal = new int[1];
            MPI.COMM_WORLD.Recv(nVal, 0, 1, MPI.INT, 0, 99);
            int nGramLen = nVal[0];

            // prejme povedi
            Object[] recvObj = new Object[1];
            MPI.COMM_WORLD.Recv(recvObj, 0, 1, MPI.OBJECT, 0, 0);
            String[] chunk = (String[]) recvObj[0];

            // združi chunk v besedilo in izračunaj n-grame
            StringBuilder sb = new StringBuilder();
            for (String s : chunk) sb.append(s).append(". ");
            Map<String, Integer> ngrams = generateNGrams(nGramLen, sb.toString());

            // pošlji nazaj masterju
            MPI.COMM_WORLD.Send(new Object[]{ngrams}, 0, 1, MPI.OBJECT, 0, 1);
        }

        MPI.Finalize();
    }

    // ------------------ Metode ------------------

    public static String preberiIzTxt(String path) {
        Path filePath = Paths.get(path);
        System.out.println("Berem datoteko iz: " + filePath.toAbsolutePath());

        // Preveri, če datoteka obstaja in je berljiva
        if (!Files.exists(filePath)) {
            System.err.println("NAPAKA: Datoteka ne obstaja na lokaciji: " + filePath.toAbsolutePath());
            return "";
        }
        if (!Files.isReadable(filePath)) {
            System.err.println("NAPAKA: Datoteka ni berljiva!");
            return "";
        }

        // Poskusi prebrati datoteko
        try {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("NAPAKA pri branju datoteke: " + e.getMessage());
        } catch (OutOfMemoryError e) {
            System.err.println("NAPAKA: Datoteka je prevelika za branje v pomnilnik! Uporabi manjšo datoteko ali povečaj heap (-Xmx).");
        }
        return "";
    }

    public static String odstraniZnakce(String text) {
        return text.replaceAll("[,;:¡¿]", "");
    }

    public static Map<String, Integer> generateNGrams(int n, String text) {
        Map<String, Integer> nGrams = new HashMap<>();
        String[] povedi = text.split("[.!?]");
        for (String enaPoved : povedi) {
            enaPoved = enaPoved.trim();
            if (enaPoved.isEmpty()) continue;
            String[] besede = enaPoved.split("\\s+");
            if (besede.length < n) continue;
            for (int j = 0; j <= besede.length - n; j++) {
                String[] ngramArray = Arrays.copyOfRange(besede, j, j + n);
                String ngram = String.join(" ", ngramArray).trim();
                nGrams.put(ngram, nGrams.getOrDefault(ngram, 0) + 1);
            }
        }
        return nGrams;
    }

    public static Map<String, Double> izracunajRelativneFrekvence(Map<String, Integer> ngrams) {
        Map<String, Integer> zacetneBesede = new HashMap<>();
        Map<String, Double> relativneFrekvence = new HashMap<>();

        for (String ngram : ngrams.keySet()) {
            String[] parts = ngram.split(" ");
            if (parts.length == 0) continue;
            String zacetek = parts[0];
            zacetneBesede.put(zacetek, zacetneBesede.getOrDefault(zacetek, 0) + ngrams.get(ngram));
        }

        for (Map.Entry<String, Integer> entry : ngrams.entrySet()) {
            String ngram = entry.getKey();
            String[] parts = ngram.split(" ");
            if (parts.length == 0) continue;
            String zacetek = parts[0];
            double verjetnost = (double) entry.getValue() / zacetneBesede.get(zacetek);
            relativneFrekvence.put(ngram, verjetnost);
        }
        return relativneFrekvence;
    }

    public static void zdruziMape(Map<String, Integer> glavna, Map<String, Integer> nova) {
        for (var entry : nova.entrySet()) {
            glavna.put(entry.getKey(), glavna.getOrDefault(entry.getKey(), 0) + entry.getValue());
        }
    }
}