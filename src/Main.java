import mpi.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class Main {

    public static int dolzinaNGrama;

    public static void main(String[] args) {
        long maxHeapSize = Runtime.getRuntime().maxMemory();
        System.out.println("Max Heap Size (količina rama za JVM): " + (maxHeapSize / (1024 * 1024)) + " MB");

        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();

        if (rank == 0) {
            // MASTER: prebere datoteko in pripravi podatke
            String[] povedi = beriInPripraviPodatke();
            mpiObdelava(povedi);
        } else {
            // WORKER: samo čaka na delo
            mpiObdelava(null);
        }

        MPI.Finalize();
    }

    // -------- -------- -------- -------- -------- -------- -------- -------- -------- --------
    // Funkcija za MPI obdelavo
    public static void mpiObdelava(String[] povedi) {
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        if (rank == 0) {
            double zacetek = System.currentTimeMillis();

            System.out.println("Število povedi: " + povedi.length);

            // Pošlji dolžino n-gramov workerjem
            for (int i = 1; i < size; i++) {
                MPI.COMM_WORLD.Send(new int[]{dolzinaNGrama}, 0, 1, MPI.INT, i, 99);
            }

            // Razdeli povedi med workere
            int chunkSize = (int) Math.ceil((double) povedi.length / (size - 1));
            int start = 0;
            for (int i = 1; i < size; i++) {
                int end = Math.min(start + chunkSize, povedi.length);
                String[] chunk = Arrays.copyOfRange(povedi, start, end);
                MPI.COMM_WORLD.Send(new Object[]{chunk}, 0, 1, MPI.OBJECT, i, 0);
                start = end;
            }

            // Zberi rezultate
            Map<String, Integer> allNgrams = new HashMap<>();
            for (int i = 1; i < size; i++) {
                Object[] recvObj = new Object[1];
                MPI.COMM_WORLD.Recv(recvObj, 0, 1, MPI.OBJECT, i, 1);
                Map<String, Integer> localNgrams = (Map<String, Integer>) recvObj[0];
                zdruziMape(allNgrams, localNgrams);
            }

            // Izračun relativnih frekvenc
            Map<String, Double> relFrekvence = izracunajRelativneFrekvence(allNgrams);

            // izpisiVse(allNgrams, relFrekvence);

            double konec = System.currentTimeMillis();

            double casIzvedbeSekunde = (konec - zacetek) / 1000;
            String evropskaNotacijaCasIzvedbeSec = String.format("%.2f", casIzvedbeSekunde).replace('.', ',');
            System.out.println("\u001B[32m✔ ⏱ Celoten porazdeljen proces je trajal: " + evropskaNotacijaCasIzvedbeSec + " sec\u001B[0m");
        }

        else {
            // WORKER: prejme dolžino n-gramov
            int[] nVal = new int[1];
            MPI.COMM_WORLD.Recv(nVal, 0, 1, MPI.INT, 0, 99);
            int nGramLen = nVal[0];

            // Prejme povedi
            Object[] recvObj = new Object[1];
            MPI.COMM_WORLD.Recv(recvObj, 0, 1, MPI.OBJECT, 0, 0);
            String[] chunk = (String[]) recvObj[0];

            // Združi chunk v besedilo in izračunaj n-grame
            StringBuilder sb = new StringBuilder();
            for (String s : chunk) sb.append(s).append(". ");
            Map<String, Integer> ngrams = generateNGrams(nGramLen, sb.toString());

            // Pošlji nazaj masterju
            MPI.COMM_WORLD.Send(new Object[]{ngrams}, 0, 1, MPI.OBJECT, 0, 1);

        }
    }

// -------- -------- -------- -------- METODE -------- -------- -------- -------- -------- --------

    // Funkcija za branje podatkov iz txt
    // malo drugacna funkcija kot v paralelni in sekvencni verziji
    // tam se funkciji rece; preberiIzTxt
    public static String[] beriInPripraviPodatke() {
        dolzinaNGrama = 5;

        Path projectRoot = Paths.get(System.getProperty("user.dir")).getParent();
        Path filePath = Paths.get(projectRoot.toString(), "resources", "613MB.txt");
        System.out.println("Berem datoteko iz: " + filePath.toAbsolutePath());

        String text;

        try {
            text = Files.readString(filePath, StandardCharsets.UTF_8);
            System.out.println("Datoteka uspešno prebrana!");
        } catch (IOException e) {
            System.err.println("NAPAKA: Datoteka ne obstaja ali ni dostopna! " + e.getMessage());
            MPI.Finalize();
            System.exit(1);
            return new String[0]; // za varnost
        }

        if (text.isEmpty()) {
            System.err.println("NAPAKA: Datoteka je prazna!");
            MPI.Finalize();
            System.exit(1);
        }

        text = odstraniZnakce(text);
        return text.split("[.!?]");
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

    public static void izpisiVse(Map<String, Integer> ngrams, Map<String, Double> frekvence) {
        System.out.println("\n----- Vsi n-grami in frekvence -----");
        for (Map.Entry<String, Integer> entry : ngrams.entrySet()) {
            String ngram = entry.getKey();
            int ponovitve = entry.getValue();
            double frekvenca = frekvence.getOrDefault(ngram, 0.0);
            System.out.printf("%s -> %d -> %.4f%%%n", ngram, ponovitve, frekvenca * 100);
        }
        System.out.println("-----------------------------------");
    }
    
    
}
