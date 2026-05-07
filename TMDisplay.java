import java.util.*;

public class TMDisplay {

    static final int    WINDOW = 15;
    static final String LINE   = "-".repeat(60);
    static final String LINE2  = "=".repeat(60);

    static void printTransitions(Map<String, Transition> table) {
        System.out.println("Uebergangsfunktion:");
        for (var e : table.entrySet()) {
            String[] k = e.getKey().split(",");
            int q = Integer.parseInt(k[0]);
            int x = Integer.parseInt(k[1]);
            Transition t = e.getValue();
            String dir = t.direction() == 1 ? "L" : (t.direction() == 2 ? "R" : "D" + t.direction());
            System.out.printf("  d(q%d, %c) = (q%d, %c, %s)%n",
                    q - 1, TMCore.symChar(x), t.nextState() - 1, TMCore.symChar(t.writeSymbol()), dir);
        }
    }

    // Anforderungen b, c, d, e
    static void printBand(Map<Integer, Integer> tape, int head, int state, int steps) {
        int lo = Math.min(head - WINDOW, tape.isEmpty() ? head - 1 : Collections.min(tape.keySet()) - 1);
        int hi = Math.max(head + WINDOW, tape.isEmpty() ? head + 1 : Collections.max(tape.keySet()) + 1);

        StringBuilder band = new StringBuilder();
        for (int pos = lo; pos <= hi; pos++) {
            char ch = TMCore.symChar(tape.getOrDefault(pos, TMCore.BLANK));
            band.append(pos == head ? "[" + ch + "]" : " " + ch + " ");
        }

        System.out.printf("Schritt %6d | Zustand: q%-2d | Kopf @ Position %5d%n", steps, state - 1, head);
        System.out.println("Band:  " + band);
        System.out.println();
    }

    static void printSimHeader(Map<Integer, Integer> tape, boolean stepMode) {
        String eingabe = TMCore.getResult(tape);
        System.out.println("\n" + LINE);
        System.out.println("Modus   : " + (stepMode ? "Schritt-Modus" : "Lauf-Modus"));
        System.out.println("Eingabe : " + (eingabe.isEmpty() ? "(leer)" : eingabe));
        System.out.println(LINE + "\n");
    }

    // Anforderung a, b, e
    static void printResult(Map<Integer, Integer> tape, int state, int steps) {
        String result = TMCore.getResult(tape);
        System.out.println(LINE);
        System.out.println("a) Ergebnis (Band)  : " + (result.isEmpty() ? "(leer)" : result));
        System.out.println("a) Ergebnis (Laenge): " + result.length());
        System.out.println("b) Endzustand       : q" + (state - 1));
        System.out.println("e) Schritte gesamt  : " + steps);
        System.out.println(LINE);
    }

    static void promptStep(Map<Integer, Integer> tape, int head, int state, int steps, Scanner sc) {
        printBand(tape, head, state, steps);
        System.out.print("  [ENTER] fuer naechsten Schritt ...");
        sc.nextLine();
    }
}
