import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Turing-Maschinen-Emulator  -  THIN Aufgabe 1
 *
 * Kodierungskonvention (Vorlesung Teil-6):
 *   Uebergang  d(qi, Xj) = (qk, Xl, Dm)  wird kodiert als:
 *       0^i  1  0^j  1  0^k  1  0^l  1  0^m
 *
 *   Uebergaenge werden durch "11" getrennt.
 *
 *   Zustaende  :  q1 = Startzustand, q2 = Akzeptierzustand (Halt)
 *   Symbole    :  X1 = '0',  X2 = '1',  X3 = '_' (Leerzeichen/Blank)
 *   Richtungen :  D1 = L (links),  D2 = R (rechts)
 *
 * Starten:  javac Emulator.java && java Emulator
 */
public class Emulator {

    // --- Konstanten -----------------------------------------------------------

    static final int BLANK        = 3;  // Leerzeichen-Symbol
    static final int START_STATE  = 1;  // q1 = Startzustand
    static final int ACCEPT_STATE = 2;  // q2 = Akzeptierzustand
    static final int WINDOW       = 15; // Felder links/rechts vom Kopf

    static final String SEP  = "-".repeat(60);
    static final String SEP2 = "=".repeat(60);

    static final String T1 = "010010001010011000101010010110001001001010011000100010001010";
    static final String T2 = "1010010100100110101000101001100010010100100110001010010100";

    // --- Uebergang ------------------------------------------------------------

    record Transition(int nextState, int writeSymbol, int direction) {}

    // --- Parsing --------------------------------------------------------------

    static Map<String, Transition> parseTM(String code) {
        code = code.strip().replace("{", "").replace("}", "").replace(" ", "");
        Map<String, Transition> transitions = new LinkedHashMap<>();

        String[] parts = code.split("11");
        for (String part : parts) {
            if (part.isEmpty()) continue;

            String[] components = part.split("1", -1);
            if (components.length != 5) {
                System.out.printf("  [!] Ungueltig (erwartet 5 Teile, gefunden %d): '%s'%n",
                        components.length, part);
                continue;
            }

            int state     = components[0].length();
            int symbol    = components[1].length();
            int nextState = components[2].length();
            int write     = components[3].length();
            int direction = components[4].length();

            String key = state + "," + symbol;
            if (transitions.containsKey(key))
                System.out.printf("  [!] Doppelter Uebergang fuer (q%d, X%d) - wird ueberschrieben%n",
                        state, symbol);

            transitions.put(key, new Transition(nextState, write, direction));
        }
        return transitions;
    }

    static void printTransitions(Map<String, Transition> transitions) {
        System.out.println("Uebergangsfunktion:");
        for (var entry : transitions.entrySet()) {
            String[] kp  = entry.getKey().split(",");
            int q        = Integer.parseInt(kp[0]);
            int x        = Integer.parseInt(kp[1]);
            Transition t = entry.getValue();
            String dir   = t.direction() == 1 ? "L" : (t.direction() == 2 ? "R" : "D" + t.direction());
            System.out.printf("  d(q%d, %c) = (q%d, %c, %s)%n",
                    q, symChar(x), t.nextState(), symChar(t.writeSymbol()), dir);
        }
    }

    // --- Band-Hilfsfunktionen -------------------------------------------------

    static char symChar(int sym) {
        return switch (sym) {
            case 1  -> '0';
            case 2  -> '1';
            default -> '_';
        };
    }

    static Map<Integer, Integer> parseTape(String raw) {
        Map<Integer, Integer> tape = new HashMap<>();
        for (int i = 0; i < raw.length(); i++) {
            if      (raw.charAt(i) == '0') tape.put(i, 1);
            else if (raw.charAt(i) == '1') tape.put(i, 2);
        }
        return tape;
    }

    static String getResult(Map<Integer, Integer> tape) {
        if (tape.isEmpty()) return "";
        int lo = Collections.min(tape.keySet());
        int hi = Collections.max(tape.keySet());
        StringBuilder sb = new StringBuilder();
        for (int p = lo; p <= hi; p++) {
            int sym = tape.getOrDefault(p, BLANK);
            if (sym != BLANK) sb.append(symChar(sym));
        }
        return sb.toString();
    }

    // --- Bandanzeige (Anforderungen b-e) --------------------------------------

    static void display(Map<Integer, Integer> tape, int head, int state, int steps) {
        int lo = Math.min(head - WINDOW,
                tape.isEmpty() ? head - 1 : Collections.min(tape.keySet()) - 1);
        int hi = Math.max(head + WINDOW,
                tape.isEmpty() ? head + 1 : Collections.max(tape.keySet()) + 1);

        StringBuilder band = new StringBuilder();
        for (int pos = lo; pos <= hi; pos++) {
            char ch = symChar(tape.getOrDefault(pos, BLANK));
            if (pos == head) band.append('[').append(ch).append(']');
            else             band.append(' ').append(ch).append(' ');
        }

        System.out.printf("Schritt %6d | Zustand: q%-2d | Kopf @ Position %5d%n",
                steps, state, head);
        System.out.println("Band:  " + band);
        System.out.println();
    }

    // --- Simulation -----------------------------------------------------------

    static void simulate(Map<String, Transition> transitions,
                         Map<Integer, Integer> inputTape,
                         boolean stepMode,
                         Scanner scanner) {
        Map<Integer, Integer> tape = new HashMap<>(inputTape);
        int state = START_STATE;
        int head  = 0;
        int steps = 0;

        String eingabe = getResult(tape);
        System.out.println("\n" + SEP);
        System.out.println("Modus   : " + (stepMode ? "Schritt-Modus" : "Lauf-Modus"));
        System.out.println("Eingabe : " + (eingabe.isEmpty() ? "(leer)" : eingabe));
        System.out.println(SEP + "\n");

        if (stepMode) {
            display(tape, head, state, steps);
            System.out.print("  [ENTER] fuer naechsten Schritt ...");
            scanner.nextLine();
        }

        while (true) {
            int symbol = tape.getOrDefault(head, BLANK);

            // Abbruchbedingungen
            if (state == ACCEPT_STATE) {
                System.out.printf("[OK]    AKZEPTIERT - Zustand q%d nach %d Schritten.%n%n",
                        state, steps);
                break;
            }

            String key = state + "," + symbol;
            if (!transitions.containsKey(key)) {
                System.out.printf("[STOP]  Kein Uebergang fuer (q%d, %c) nach %d Schritten.%n%n",
                        state, symChar(symbol), steps);
                break;
            }

            // Uebergang ausfuehren
            Transition t = transitions.get(key);
            tape.put(head, t.writeSymbol());
            head  += (t.direction() == 2 ? 1 : -1);
            state  = t.nextState();
            steps++;

            if (stepMode) {
                display(tape, head, state, steps);
                System.out.print("  [ENTER] fuer naechsten Schritt ...");
                scanner.nextLine();
            }
        }

        // Endergebnis
        String result = getResult(tape);
        System.out.println(SEP);
        System.out.println("a) Ergebnis (Band)  : " + (result.isEmpty() ? "(leer)" : result));
        System.out.println("b) Endzustand       : q" + state);
        System.out.println("e) Schritte gesamt  : " + steps);
        System.out.println(SEP);
        display(tape, head, state, steps);
    }

    // --- Hauptprogramm --------------------------------------------------------

    public static void main(String[] args) throws Exception {
        // UTF-8 Ausgabe erzwingen (fuer IntelliJ und Windows-Konsole)
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setIn(new java.io.BufferedInputStream(System.in));
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

        System.out.println(SEP2);
        System.out.println("   Turing-Maschinen-Emulator  -  THIN Aufgabe 1");
        System.out.println(SEP2);

        // TM-Code eingeben
        System.out.println("\nTM-Kodierung eingeben");
        System.out.println("  (oder '1' / '2' fuer Testbeispiele aus der Vorlesung):");
        System.out.print("  > ");
        String rawCode = scanner.nextLine().strip();

        String tmCode;
        if (rawCode.equalsIgnoreCase("T1")) {
            tmCode = T1;
            System.out.println("  T1 = " + T1);
        } else if (rawCode.equalsIgnoreCase("T2")) {
            tmCode = T2;
            System.out.println("  T2 = " + T2);
        } else {
            tmCode = rawCode;
        }

        System.out.println();
        Map<String, Transition> transitions = parseTM(tmCode);

        if (transitions.isEmpty()) {
            System.out.println("Fehler: Keine gueltigen Uebergaenge gefunden.");
            return;
        }
        printTransitions(transitions);

        // Eingabe
        System.out.println("\nEingabe auf dem Band:");
        System.out.println("  Binaerstring direkt eingeben (z. B. '101')");
        System.out.println("  oder Dezimalzahl eingeben (wird in Binaer umgewandelt):");
        System.out.print("  > ");
        String rawInput = scanner.nextLine().strip();

        // Dezimal -> Binaer, wenn keine reine Binaerfolge
        if (!rawInput.isEmpty() && !rawInput.matches("[01]*")) {
            try {
                long n = Long.parseLong(rawInput);
                rawInput = Long.toBinaryString(n);
                System.out.printf("  Dezimal %d -> Binaer: %s%n", n, rawInput);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Ungueltige Eingabe, wird als leer behandelt.");
                rawInput = "";
            }
        }

        Map<Integer, Integer> tape = parseTape(rawInput);

        // Modus
        System.out.println("\nModus: (s)chritt-Modus  oder  (l)auf-Modus? [s/l]");
        System.out.print("  > ");
        String modeInput = scanner.nextLine().strip().toLowerCase();
        boolean stepMode = modeInput.equals("s");

        simulate(transitions, tape, stepMode, scanner);
    }
}
