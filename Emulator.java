import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Turing-Maschinen-Emulator — THIN Aufgabe 1 & 2
 *
 * Kodierung (Vorlesung Teil-6):  d(qi, Xj) = (qk, Xl, Dm)
 *   → 0^i 1 0^j 1 0^k 1 0^l 1 0^m   |   Uebergaenge getrennt durch "11"
 *
 * Symbole:    X1='0'  X2='1'  X3='_' (Blank)
 * Richtungen: D1=L    D2=R
 * Zustaende:  q0 = Startzustand (intern 1)  |  Halt = kein Uebergang definiert
 */
public class Emulator {

    // =========================================================================
    // Konstanten
    // =========================================================================

    static final int BLANK  = 3;   // Symbol-Index fuer Leerzeichen
    static final int START  = 1;   // Interner Startzustand (angezeigt als q0)
    static final int WINDOW = 15;  // Sichtbare Felder links/rechts vom Kopf

    static final String LINE  = "-".repeat(60);
    static final String LINE2 = "=".repeat(60);

    // Vordefinierte TM-Kodierungen
    static final String T1 = "010010001010011000101010010110001001001010011000100010001010";
    static final String T2 = "1010010100100110101000101001100010010100100110001010010100";
    static final String T3 = "0100101001001101000100100001011001010010101100100100001010011001000100000010001001100001010000101001100001001000010010011000010000100001000010011000010001000001001011000001001000001001011000001000010010000101100000010100000010100110000001001000000100100110000001000010000001000010011000000100010000000100001011000000010100000001010110000000100100000001001011000000010000100000001000010110000000100010000000010001001100000000101000000000100010011000000001000010000000000000010001001100000000010100000000010100110000000001000010000000000100001001100000000001010000000000101001100000000001001000000000001010011000000000010000100000000000001000010110000000000010010000000000010010011000000000001000010000000000010000100110000000000010001000000000000100101100000000000010010000000000001001011000000000000100001000000000000100001011000000000000101000000000010100110000000000000101000000000000010010110000000000000100001000000010000101100000000000000100100000000000000100010011000000000000001000010001000100";


    // =========================================================================
    // Datenmodell
    // =========================================================================

    record Transition(int nextState, int writeSymbol, int direction) {}


    // =========================================================================
    // Kern-Logik
    // =========================================================================

    /** Wandelt Symbol-Index in lesbares Zeichen um. */
    static char symChar(int sym) {
        return switch (sym) {
            case 1  -> '0';
            case 2  -> '1';
            default -> '_';
        };
    }

    /** Parst eine TM-Kodierung in eine Uebergangstabelle. */
    static Map<String, Transition> parseTM(String code) {
        code = code.strip().replace("{", "").replace("}", "").replace(" ", "");
        Map<String, Transition> table = new LinkedHashMap<>();

        for (String part : code.split("11")) {
            if (part.isEmpty()) continue;

            String[] c = part.split("1", -1);
            if (c.length != 5) {
                System.out.printf("  [!] Ungueltig (%d Teile statt 5): '%s'%n", c.length, part);
                continue;
            }

            int state  = c[0].length();
            int symbol = c[1].length();
            String key = state + "," + symbol;

            if (table.containsKey(key))
                System.out.printf("  [!] Doppelter Uebergang (q%d, X%d) — wird ueberschrieben%n", state - 1, symbol);

            table.put(key, new Transition(c[2].length(), c[3].length(), c[4].length()));
        }
        return table;
    }

    /** Legt einen Eingabe-String als Band-Map an (Position → Symbol-Index). */
    static Map<Integer, Integer> parseTape(String raw) {
        Map<Integer, Integer> tape = new HashMap<>();
        for (int i = 0; i < raw.length(); i++) {
            if      (raw.charAt(i) == '0') tape.put(i, 1);
            else if (raw.charAt(i) == '1') tape.put(i, 2);
        }
        return tape;
    }

    /** Liest alle Nicht-Blank-Symbole vom Band als String. */
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


    // =========================================================================
    // Simulation
    // =========================================================================

    static void simulate(Map<String, Transition> table,
                         Map<Integer, Integer> inputTape,
                         boolean stepMode,
                         Scanner sc) {
        Map<Integer, Integer> tape = new HashMap<>(inputTape);
        int state = START;
        int head  = 0;
        int steps = 0;

        printSimHeader(tape, stepMode);
        if (stepMode) promptStep(tape, head, state, steps, sc);

        while (true) {
            int symbol = tape.getOrDefault(head, BLANK);
            Transition t = table.get(state + "," + symbol);

            if (t == null) {
                System.out.printf("%n[HALT]  Kein Uebergang fuer (q%d, %c) nach %d Schritten.%n%n",
                        state - 1, symChar(symbol), steps);
                break;
            }

            tape.put(head, t.writeSymbol());
            head  += (t.direction() == 2 ? 1 : -1);
            state  = t.nextState();
            steps++;

            if (stepMode) promptStep(tape, head, state, steps, sc);
        }

        printResult(tape, state, steps);
        printBand(tape, head, state, steps);
    }


    // =========================================================================
    // Ausgabe
    // =========================================================================

    /** Gibt alle Uebergaenge der Tabelle lesbar aus. */
    static void printTransitions(Map<String, Transition> table) {
        System.out.println("Uebergangsfunktion:");
        for (var e : table.entrySet()) {
            String[] k = e.getKey().split(",");
            int q = Integer.parseInt(k[0]);
            int x = Integer.parseInt(k[1]);
            Transition t = e.getValue();
            String dir = t.direction() == 1 ? "L" : (t.direction() == 2 ? "R" : "D" + t.direction());
            System.out.printf("  d(q%d, %c) = (q%d, %c, %s)%n",
                    q - 1, symChar(x), t.nextState() - 1, symChar(t.writeSymbol()), dir);
        }
    }

    /** Zeigt Bandinhalt mit Kopfmarkierung — Anforderungen b, c, d, e. */
    static void printBand(Map<Integer, Integer> tape, int head, int state, int steps) {
        int lo = Math.min(head - WINDOW, tape.isEmpty() ? head - 1 : Collections.min(tape.keySet()) - 1);
        int hi = Math.max(head + WINDOW, tape.isEmpty() ? head + 1 : Collections.max(tape.keySet()) + 1);

        StringBuilder band = new StringBuilder();
        for (int pos = lo; pos <= hi; pos++) {
            char ch = symChar(tape.getOrDefault(pos, BLANK));
            band.append(pos == head ? "[" + ch + "]" : " " + ch + " ");
        }

        System.out.printf("Schritt %6d | Zustand: q%-2d | Kopf @ Position %5d%n", steps, state - 1, head);
        System.out.println("Band:  " + band);
        System.out.println();
    }

    /** Zeigt Modus und Eingabe vor dem Start. */
    static void printSimHeader(Map<Integer, Integer> tape, boolean stepMode) {
        String eingabe = getResult(tape);
        System.out.println("\n" + LINE);
        System.out.println("Modus   : " + (stepMode ? "Schritt-Modus" : "Lauf-Modus"));
        System.out.println("Eingabe : " + (eingabe.isEmpty() ? "(leer)" : eingabe));
        System.out.println(LINE + "\n");
    }

    /** Zeigt das Endergebnis — Anforderung a, b, e. */
    static void printResult(Map<Integer, Integer> tape, int state, int steps) {
        String result = getResult(tape);
        System.out.println(LINE);
        System.out.println("a) Ergebnis (Band)  : " + (result.isEmpty() ? "(leer)" : result));
        System.out.println("a) Ergebnis (Laenge): " + result.length());
        System.out.println("b) Endzustand       : q" + (state - 1));
        System.out.println("e) Schritte gesamt  : " + steps);
        System.out.println(LINE);
    }

    /** Zeigt Band und wartet auf Enter (nur im Step-Modus). */
    static void promptStep(Map<Integer, Integer> tape, int head, int state, int steps, Scanner sc) {
        printBand(tape, head, state, steps);
        System.out.print("  [ENTER] fuer naechsten Schritt ...");
        sc.nextLine();
    }


    // =========================================================================
    // Dialog / Hauptprogramm
    // =========================================================================

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setIn(new java.io.BufferedInputStream(System.in));
        Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8);

        while (true) {
            System.out.println(LINE2);
            System.out.println("   Turing-Maschinen-Emulator  —  THIN Aufgabe 1 & 2");
            System.out.println(LINE2);

            // --- TM-Kodierung einlesen ---
            System.out.println("\nTM-Kodierung eingeben");
            System.out.println("  (oder 'T1'/'T2' fuer Vorlesungsbeispiele, 'T3' fuer Aufgabe 2):");
            System.out.print("  > ");
            String rawCode = sc.nextLine().strip();

            String tmCode = switch (rawCode.toUpperCase()) {
                case "T1" -> { System.out.println("  T1 = " + T1); yield T1; }
                case "T2" -> { System.out.println("  T2 = " + T2); yield T2; }
                case "T3" -> { System.out.println("  T3 = " + T3); yield T3; }
                default   -> rawCode;
            };

            System.out.println();
            Map<String, Transition> table = parseTM(tmCode);
            if (table.isEmpty()) {
                System.out.println("Fehler: Keine gueltigen Uebergaenge gefunden.");
                continue;
            }
            printTransitions(table);

            // --- Bandeingabe ---
            System.out.println("\nEingabe auf dem Band (Dezimalzahl):");
            System.out.print("  > ");
            String rawInput = sc.nextLine().strip();

            System.out.println("Kodierung: (b)inaer oder (u)naer? [b/u]");
            System.out.print("  > ");
            boolean unary = sc.nextLine().strip().equalsIgnoreCase("u");

            if (!rawInput.isEmpty()) {
                try {
                    long n = Long.parseLong(rawInput);
                    if (unary) {
                        rawInput = "1".repeat((int) n);
                        System.out.printf("  Dezimal -> Unaer:  %s%n", rawInput);
                    } else {
                        rawInput = Long.toBinaryString(n);
                        System.out.printf("  Dezimal -> Binaer: %s%n", rawInput);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("  [!] Ungueltige Eingabe — wird als leer behandelt.");
                    rawInput = "";
                }
            }

            // --- Modus waehlen ---
            System.out.println("\nModus: (s)chritt-Modus  oder  (l)auf-Modus? [s/l]");
            System.out.print("  > ");
            boolean stepMode = sc.nextLine().strip().equalsIgnoreCase("s");

            // --- Simulation starten ---
            simulate(table, parseTape(rawInput), stepMode, sc);

            // --- Neustart oder Beenden ---
            System.out.println("\n" + LINE2);
            System.out.println("  [R] Neustart   |   beliebige Taste + ENTER zum Beenden");
            System.out.print("  > ");
            if (!sc.nextLine().strip().equalsIgnoreCase("r")) break;
            System.out.println();
        }
    }
}
